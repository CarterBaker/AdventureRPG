#version 330 core

in vec3 v_dir;
in vec2 v_screenPos;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"
#include "includes/NoiseUtility.glsl"
#include "weather/includes/CloudDome.glsl"

/*
* Fullscreen volumetric cloud pass. Each weather-map entry is a horizontal
 * slab — an authored altitude plus a vertical thickness, bent toward the
 * horizon per-fragment by CloudDome.glsl — that the view ray is raymarched
 * through in a handful of steps rather than sampled once. Real front-to-
 * back Beer-Lambert integration through that thickness is what gives the
 * clouds actual volume: a grazing horizon ray crosses many effective steps
 * and reads as a puffy silhouette, a ray looking straight up through the
 * base reads as a flat, dark underside, and a ray whose origin already sits
 * inside a puff saturates to full density within its first step or two and
 * reads as fog. Horizontal shape (which puff, how big, how ragged its edge)
 * is still evaluated once per entry per pixel — see resolveCloudHorizontal
 * — since it only needs to vary with screen position, not with depth along
 * the ray; only the vertical density profile — see resolveVerticalDensity —
 * is re-evaluated at each march step. Entries arrive from
 * WeatherMapBufferSystem already sorted nearest-first and placed into their
 * own sub-region of their pattern's footprint, so front-to-back compositing
 * across entries (outer loop) nested around front-to-back compositing
 * through one entry's own thickness (inner loop) gives a convincing layered
 * sky with no true depth sort or per-pixel light-ray march.
 */

uniform float u_cloudAltitudeMin;
uniform float u_cloudAltitudeMax;
uniform float u_cloudMaxDistance;
uniform vec2  u_weatherDriftDirection;
uniform float u_weatherDriftSpeed;

const float CLOUD_DENSITY_EPSILON         = 0.001;
const float CLOUD_ALPHA_SATURATION_CUTOFF = 0.985;
const float CLOUD_CULL_SAFETY_MARGIN      = 2.2;

const float CLOUD_OUTER_FADE_START = 0.85;
const float CLOUD_OUTER_FADE_END   = 1.35;

const float CLOUD_PUFF_ANGLE_WOBBLE  = 1.2;
const float CLOUD_DRIFT_SCROLL_SCALE = 0.35;
const float CLOUD_MORPH_TIME_SCALE   = 0.015;

// Stable, non-time-varying warp on a pattern's own outer silhouette so the
// whole system doesn't read as a perfect ellipse.
const float PATTERN_BOUNDARY_WARP_STRENGTH  = 0.22;
const float PATTERN_BOUNDARY_WARP_FREQUENCY = 2.5;

const float CLOUD_FAR_FADE_FRACTION = 0.2;

// Silver lining — forward-scattering rim brightening thin edges when
// looking toward the sun through them.
const float CLOUD_SILVER_LINING_POWER    = 10.0;
const float CLOUD_SILVER_LINING_STRENGTH = 0.9;
const float CLOUD_AMBIENT_SHADOW         = 0.15;
const float CLOUD_AMBIENT_LIT            = 0.6;
const float CLOUD_SKY_TINT_STRENGTH      = 0.45;
const float CLOUD_STORM_DARKEN_MIN       = 0.45;

// Per-step optical depth = local density * archetype density * this scale *
// (step length / archetype thickness) — normalizing by the archetype's own
// thickness keeps a 25-block Stratus and a 700-block Cumulonimbus both
// reading roughly opaque at their own authored density of ~1, straight
// through.
const float CLOUD_DENSITY_OPACITY_SCALE              = 2.0;
const float CLOUD_AMBIENT_THICKNESS_REFERENCE_BLOCKS = 160.0;
const float CLOUD_MIN_SLAB_THICKNESS_BLOCKS          = 4.0;

const float CLOUD_HEIGHT_UNDULATION_FREQUENCY = 0.0012;
const float CLOUD_HEIGHT_UNDULATION_STRENGTH  = 0.35;
const float CLOUD_SAMPLE_LOOKAHEAD_BLOCKS     = 96.0;

// Raymarch through one entry's own slab thickness. Step length targets
// CLOUD_VOLUME_STEP_LENGTH_BLOCKS, clamped to a small step-count range so a
// grazing horizon ray and a straight-up ray both get real integration
// without per-entry cost ever running away (up to WEATHER_MAP_MAX_ENTRIES
// entries can be evaluated per pixel).
const float CLOUD_VOLUME_STEP_LENGTH_BLOCKS   = 40.0;
const int   CLOUD_VOLUME_STEP_COUNT_MIN       = 2;
const int   CLOUD_VOLUME_STEP_COUNT_MAX       = 6;
const float CLOUD_VOLUME_DITHER_STRENGTH      = 0.65;
const float CLOUD_VOLUME_WISP_FREQUENCY       = 0.02;
const float CLOUD_VOLUME_WISP_STRENGTH        = 0.35;
const float CLOUD_VOLUME_SELF_SHADOW_STRENGTH = 0.65;

// Puff-field shaping — presence is gated per-cell by a coarse shared cluster
// field so puffs clump into real patches with real gaps; the same field is
// also sampled continuously so a clump's interior fills solidly instead of
// depending on individual puff discs to overlap. Lobe/domain-warp terms
// keep puffs reading as irregular lobes rather than round bubbles.
const float PUFF_CLUSTER_FREQUENCY   = 0.12;
const float PUFF_PRESENCE_SOFTBAND   = 0.22;
const float PUFF_RADIUS_CELLS_MIN    = 0.50;
const float PUFF_RADIUS_CELLS_MAX    = 0.85;
const float PUFF_FIELD_REACH         = 1.15;
const float PUFF_MACRO_WEIGHT        = 0.9;
const float PUFF_VERTICAL_CENTER_MIN = 0.18;
const float PUFF_VERTICAL_CENTER_MAX = 0.82;
const float PUFF_VERTICAL_SPAN_MIN   = 0.30;
const float PUFF_VERTICAL_SPAN_MAX   = 0.70;
const float PUFF_DETAIL_FREQUENCY    = 0.05;
const float PUFF_DETAIL_STRENGTH     = 0.28;
const float PUFF_LOBE_FREQUENCY      = 2.5;
const float PUFF_LOBE_STRENGTH       = 0.45;

const float PUFF_DOMAIN_WARP_FREQUENCY = 0.35;
const float PUFF_DOMAIN_WARP_STRENGTH  = 0.5;
const float PUFF_UNION_THRESHOLD       = 0.55;
const float PUFF_UNION_SOFTNESS        = 0.35;

// Cheap ray-vs-footprint-circle test used to skip an entry entirely when the
// view ray could never pass near its footprint. The circle is the entry's
// own bounding box's circumscribed radius inflated by
// CLOUD_CULL_SAFETY_MARGIN — a false accept only costs one skipped entry's
// worth of wasted work, a false reject would visibly clip a cloud, so this
// stays generous.
bool footprintMayBeVisible(vec2 rayOriginXZ, vec2 rayDirXZ, vec2 circleCenter, float circleRadius, float maxDist) {
    float rayDirLenXZ = length(rayDirXZ);

    if (rayDirLenXZ < 0.0001)
    return distance(rayOriginXZ, circleCenter) <= circleRadius;

    vec2  dirNorm   = rayDirXZ / rayDirLenXZ;
    vec2  toCenter  = circleCenter - rayOriginXZ;
    float tClosest  = clamp(dot(toCenter, dirNorm), 0.0, maxDist);
    vec2  closestXZ = rayOriginXZ + dirNorm * tClosest;

    return distance(closestXZ, circleCenter) <= circleRadius;
}

// Resolves the vertical slab (tNear/tFar plus bottom/top altitude) one view
// ray crosses for one weather entry. The dome bend is resolved per fragment:
// a first ray/plane intersection at the cloud's own authored altitude gives
// a ground-position guess, that guess's true camera distance bends the
// altitude toward the fade altitude (CloudDome.glsl), and a second
// intersection at the bent altitude gives the final slab used both for the
// undulation noise and the raymarch. worldPosMid is a representative point
// near the middle of the crossing, used by resolveCloudHorizontal for its
// single per-pixel horizontal sample.
bool resolveCloudSlab(
    vec4 shape, vec3 rayDir, vec2 chunkOffsetBlocks, float patternSeed,
    out vec3 worldPosMid, out float tNear, out float pathLength,
    out float slabBottomY, out float slabTopY) {
    float clampedAltitude = clamp(shape.y, u_cloudAltitudeMin, u_cloudAltitudeMax);
    float thickness       = max(shape.x, CLOUD_MIN_SLAB_THICKNESS_BLOCKS);
    float halfThickness   = thickness * 0.5;

    float guardedDirY = rayDir.y >= 0.0 ? max(rayDir.y, 0.05) : min(rayDir.y, -0.05);

    float tGuess       = max((clampedAltitude - u_cameraPosition.y) / guardedDirY, 0.0);
    vec2  guessXZ       = u_cameraPosition.xz + rayDir.xz * tGuess;
    float guessDistance = length(guessXZ - u_cameraPosition.xz);
    float bentAltitude  = resolveCloudDomeAltitude(clampedAltitude, guessDistance);

    float tEstimate      = max((bentAltitude - u_cameraPosition.y) / guardedDirY, 0.0);
    vec2  approxXZ        = u_cameraPosition.xz + rayDir.xz * tEstimate;
    float distanceBlocks = length(approxXZ - u_cameraPosition.xz);
    bentAltitude          = resolveCloudDomeAltitude(clampedAltitude, distanceBlocks);

    float undulation = gradientNoise2D(
        (approxXZ + chunkOffsetBlocks) * CLOUD_HEIGHT_UNDULATION_FREQUENCY
        + vec2(patternSeed * 31.7, patternSeed * 57.1));
    bentAltitude += undulation * thickness * CLOUD_HEIGHT_UNDULATION_STRENGTH;

    slabTopY    = bentAltitude + halfThickness;
    slabBottomY = bentAltitude - halfThickness;

    float tFar;

    if (abs(rayDir.y) < 0.0001) {
        bool insideSlab = u_cameraPosition.y > slabBottomY && u_cameraPosition.y < slabTopY;
        if (!insideSlab)
        return false;
        tNear = 0.0;
        tFar  = u_cloudMaxDistance;
    } else {
        float tTop    = (slabTopY    - u_cameraPosition.y) / rayDir.y;
        float tBottom = (slabBottomY - u_cameraPosition.y) / rayDir.y;
        tNear = max(min(tTop, tBottom), 0.0);
        tFar  = max(tTop, tBottom);
    }

    tFar = min(tFar, u_cloudMaxDistance);

    if (tNear >= tFar)
    return false;

    pathLength = tFar - tNear;

    float sampleAhead = min(pathLength, CLOUD_SAMPLE_LOOKAHEAD_BLOCKS);
    float tSample = tNear + sampleAhead * 0.5;
    worldPosMid = u_cameraPosition + rayDir * tSample;

    return true;
}

// One archetype's own puff field, evaluated at `localPos` — the fragment's
// sample position relative to the pattern's own carried center, already
// rotated into its elongation frame. Presence is gated per-cell by a coarse
// cluster field shared across the whole entry so puffs clump into genuine
// patches with real gaps; the fragment's own cell plus its 3x3 neighborhood
// are tested (jittered-cell/Worley technique) so a puff straddling a cell
// boundary is never clipped. Each candidate puff's radius is angularly
// warped before being thresholded (see PUFF_LOBE_*) so it reads as an
// irregular lobe, then every candidate puff contributes a soft radial field
// summed across the neighborhood (a cheap metaball union), topped up by a
// continuous macro sample of the same cluster field so a clump's interior
// reads solidly filled. Returns the winning puff's own stable per-cell hash
// for the caller's vertical/shading math. Where no candidate wins the
// presence gate, a mild shading direction is derived from the macro field's
// own gradient instead of forcing every such pixel to read as dead-center of
// a puff.
float samplePuffField(
    vec2 localPos, float elongation, float cellSizeBlocks, float presenceThreshold, float edgeSoftness,
    float patternSeed, float archetypeSalt,
    out vec2 outOffsetNorm, out vec3 outWinningPuffHash) {
    vec2 anisoLocal = vec2(localPos.x, localPos.y * elongation);

    vec2 warpFreq = vec2(PUFF_DOMAIN_WARP_FREQUENCY / max(cellSizeBlocks, 1.0));
    vec2 warp = vec2(
        gradientNoise2D(anisoLocal * warpFreq + vec2(patternSeed * 11.3, archetypeSalt)),
        gradientNoise2D(anisoLocal * warpFreq + vec2(archetypeSalt, patternSeed * 7.9)));
    vec2 warpedLocal = anisoLocal + warp * cellSizeBlocks * PUFF_DOMAIN_WARP_STRENGTH;

    vec2 cellSpacePos = warpedLocal / max(cellSizeBlocks, 1.0);
    vec2 cellBase     = floor(cellSpacePos);
    vec2 clusterSeedOffset = vec2(patternSeed * 4.7, patternSeed * 8.3);

    float macroNoise = gradientNoise2D(cellSpacePos * PUFF_CLUSTER_FREQUENCY + clusterSeedOffset) * 0.5 + 0.5;
    float macroField = smoothstep(
        presenceThreshold - PUFF_PRESENCE_SOFTBAND, presenceThreshold + PUFF_PRESENCE_SOFTBAND, macroNoise);

    float fieldSum       = 0.0;
    float bestField       = 0.0;
    vec2  bestOffsetNorm = vec2(0.0);
    vec3  bestHash       = vec3(0.0);

    for (int oy = -1; oy <= 1; oy++) {
        for (int ox = -1; ox <= 1; ox++) {
            vec2 cell     = cellBase + vec2(float(ox), float(oy));
            vec2 cellSeed = cell + vec2(patternSeed * 17.3 + archetypeSalt, patternSeed * 29.9 - archetypeSalt);

            float cellNoise    = gradientNoise2D(cell * PUFF_CLUSTER_FREQUENCY + clusterSeedOffset) * 0.5 + 0.5;
            float presenceFade = smoothstep(
                presenceThreshold - PUFF_PRESENCE_SOFTBAND, presenceThreshold + PUFF_PRESENCE_SOFTBAND, cellNoise);

            if (presenceFade <= CLOUD_DENSITY_EPSILON)
            continue;

            vec3 puffHash       = hash33(vec3(cellSeed, patternSeed * 3.71 + archetypeSalt));
            vec2 jitter         = puffHash.xy * 0.5 + 0.5;
            vec2 puffCenterCell = cell + jitter;

            float sizeT           = puffHash.z * 0.5 + 0.5;
            float puffRadiusCells = mix(PUFF_RADIUS_CELLS_MIN, PUFF_RADIUS_CELLS_MAX, sizeT);

            vec2 toPuff = cellSpacePos - puffCenterCell;

            float wobbleAngle  = fract(puffHash.x * 43.27 + puffHash.y * 17.61) * 6.28318;
            float wobbleAspect = mix(0.7, 1.35, fract(puffHash.z * 29.13 + puffHash.x * 11.7));
            float wca = cos(wobbleAngle);
            float wsa = sin(wobbleAngle);
            vec2  puffLocal   = vec2(toPuff.x * wca + toPuff.y * wsa, -toPuff.x * wsa + toPuff.y * wca);
            vec2  shapeOffset = vec2(puffLocal.x, puffLocal.y * wobbleAspect) / max(puffRadiusCells, 0.001);
            float distNorm    = length(shapeOffset);

            float lobeAngle = atan(shapeOffset.y, shapeOffset.x);
            float lobeNoise = gradientNoise2D(
                vec2(cos(lobeAngle), sin(lobeAngle)) * PUFF_LOBE_FREQUENCY + cellSeed * 0.37);
            distNorm *= 1.0 + lobeNoise * PUFF_LOBE_STRENGTH;

            float field = (1.0 - smoothstep(0.0, PUFF_FIELD_REACH, distNorm)) * presenceFade;
            fieldSum += field;

            if (field > bestField) {
                bestField      = field;
                bestOffsetNorm = toPuff / max(puffRadiusCells, 0.001);
                bestHash       = puffHash;
            }
        }
    }

    if (bestField <= CLOUD_DENSITY_EPSILON) {
        float macroGradX = gradientNoise2D((cellSpacePos + vec2(0.37, 0.0)) * PUFF_CLUSTER_FREQUENCY + clusterSeedOffset)
        - gradientNoise2D((cellSpacePos - vec2(0.37, 0.0)) * PUFF_CLUSTER_FREQUENCY + clusterSeedOffset);
        float macroGradY = gradientNoise2D((cellSpacePos + vec2(0.0, 0.37)) * PUFF_CLUSTER_FREQUENCY + clusterSeedOffset)
        - gradientNoise2D((cellSpacePos - vec2(0.0, 0.37)) * PUFF_CLUSTER_FREQUENCY + clusterSeedOffset);
        bestOffsetNorm = clamp(vec2(macroGradX, macroGradY) * 2.5, -0.85, 0.85);
        bestHash = vec3(fract(macroNoise * 17.13) * 2.0 - 1.0, fract(macroNoise * 9.71) * 2.0 - 1.0, 0.0);
    }

    outOffsetNorm      = bestOffsetNorm;
    outWinningPuffHash = bestHash;

    float combinedField = max(fieldSum, macroField * PUFF_MACRO_WEIGHT);
    float band = mix(PUFF_UNION_SOFTNESS * 0.5, PUFF_UNION_SOFTNESS * 1.5, edgeSoftness);
    return smoothstep(PUFF_UNION_THRESHOLD - band, PUFF_UNION_THRESHOLD + band, combinedField);
}

// Horizontal cloud silhouette for one weather-map entry, evaluated once per
// pixel at the ray's representative crossing point through its slab (see
// resolveCloudSlab's worldPosMid) — screen-space parallax between pixels
// already comes from each pixel casting its own ray, so a single 2D sample
// per entry per pixel is enough; only the vertical extent (see
// resolveVerticalDensity) needs to vary along the ray for genuine volume.
// Returns 0.0 and leaves the out params at their identity defaults if the
// entry is culled or has no coverage at this pixel.
float resolveCloudHorizontal(
    vec4 bounds, vec4 shape, vec4 noiseParams, vec4 colorScale,
    vec4 variance0, vec4 variance1,
    float intensity, vec3 worldPos,
    vec2 driftDirNorm, float driftAngle, float driftSpeed,
    out vec2 outOffsetNorm, out vec3 outWinningPuffHash, out vec2 outWorldOffsetNorm) {
    outOffsetNorm      = vec2(0.0);
    outWinningPuffHash = vec3(0.0);
    outWorldOffsetNorm = vec2(0.0);

    vec2 boxCenter     = (bounds.xy + bounds.zw) * 0.5;
    vec2 boxHalfExtent = max((bounds.zw - bounds.xy) * 0.5, vec2(1.0));
    vec2 fromCenter    = worldPos.xz - boxCenter;

    vec2 cullExtent = boxHalfExtent * (CLOUD_OUTER_FADE_END * 2.0);
    if (abs(fromCenter.x) > cullExtent.x || abs(fromCenter.y) > cullExtent.y)
    return 0.0;

    float patternSeed    = variance1.z;
    float cloudSlotIndex = variance1.y;
    float archetypeSalt  = cloudSlotIndex * 41.9;

    float orientationHash = hash31(vec3(
            patternSeed * 12.9898,
            cloudSlotIndex * 78.233 + patternSeed,
            patternSeed - cloudSlotIndex * 0.577));

    float puffAngle = driftAngle + (orientationHash - 0.5) * CLOUD_PUFF_ANGLE_WOBBLE;
    float cosA = cos(puffAngle);
    float sinA = sin(puffAngle);
    vec2  rotated = vec2(
        fromCenter.x * cosA + fromCenter.y * sinA,
        fromCenter.y * cosA - fromCenter.x * sinA);

    float elongation  = clamp(mix(variance0.w, variance1.x, fract(orientationHash * 3.17)), 1.0, 6.0);
    float spreadRatio = clamp(variance0.x, 0.1, 2.0);
    vec2  anisotropicExtent = boxHalfExtent * spreadRatio * vec2(1.0, 1.0 / elongation);
    vec2  spreadNorm  = rotated / max(anisotropicExtent, vec2(1.0));
    float rawRadialDist = length(spreadNorm);

    if (rawRadialDist > CLOUD_OUTER_FADE_END * (1.0 + PATTERN_BOUNDARY_WARP_STRENGTH))
    return 0.0;

    float boundaryAngle  = atan(spreadNorm.y, spreadNorm.x);
    float boundaryWobble = 1.0 + PATTERN_BOUNDARY_WARP_STRENGTH * gradientNoise2D(
        vec2(cos(boundaryAngle), sin(boundaryAngle)) * PATTERN_BOUNDARY_WARP_FREQUENCY
        + vec2(patternSeed * 5.1, cloudSlotIndex * 13.7));
    float radialDist = rawRadialDist / max(boundaryWobble, 0.35);

    if (radialDist > CLOUD_OUTER_FADE_END)
    return 0.0;

    float outerFade = 1.0 - smoothstep(CLOUD_OUTER_FADE_START, CLOUD_OUTER_FADE_END, radialDist);

    if (outerFade <= CLOUD_DENSITY_EPSILON)
    return 0.0;

    float sizeVariance   = clamp(mix(variance0.y, variance0.z, fract(orientationHash * 5.63)), 0.3, 3.0);
    float cellSizeBlocks = max(colorScale.w, 4.0) * sizeVariance;

    float coverageBias      = noiseParams.z;
    float presenceThreshold = clamp(1.0 - (intensity * 0.65 + coverageBias * 0.35), 0.02, 0.97);
    float edgeSoftness      = clamp(noiseParams.w, 0.05, 1.0);

    vec2 offsetNorm;
    vec3 winningPuffHash;
    float coverage = samplePuffField(
        rotated, elongation, cellSizeBlocks, presenceThreshold, edgeSoftness,
        patternSeed, archetypeSalt, offsetNorm, winningPuffHash);

    coverage *= outerFade;

    if (coverage <= CLOUD_DENSITY_EPSILON)
    return 0.0;

    vec2 driftScroll = driftDirNorm * driftSpeed * u_time * CLOUD_DRIFT_SCROLL_SCALE * shape.w;
    vec2 detailPos   = (rotated + driftScroll) * (PUFF_DETAIL_FREQUENCY * max(noiseParams.x, 0.1));
    detailPos += vec2(u_time * CLOUD_MORPH_TIME_SCALE * 0.3, u_time * CLOUD_MORPH_TIME_SCALE)
    + vec2(patternSeed * 19.1, cloudSlotIndex * 53.7);
    float detail         = fbmGradient2D(detailPos, 3, 2.1, 0.5) - 0.5;
    float detailStrength = PUFF_DETAIL_STRENGTH * clamp(noiseParams.y, 0.0, 1.5);
    coverage = clamp(coverage + detail * detailStrength * (1.0 - coverage), 0.0, 1.0);

    if (coverage <= CLOUD_DENSITY_EPSILON)
    return 0.0;

    outOffsetNorm      = offsetNorm;
    outWinningPuffHash = winningPuffHash;

    vec2 approxLocalOffset = vec2(offsetNorm.x, offsetNorm.y / max(elongation, 1.0));
    outWorldOffsetNorm = vec2(
        approxLocalOffset.x * cosA - approxLocalOffset.y * sinA,
        approxLocalOffset.x * sinA + approxLocalOffset.y * cosA);

    return coverage;
}

// One puff's own vertical density profile at an arbitrary world height
// within its slab, sampled once per raymarch step so a ray crossing at any
// angle integrates real thickness instead of a single flat number. Reuses
// the winning puff's own stable hash (see resolveCloudHorizontal /
// samplePuffField) so its vertical center/span stay fixed for that puff's
// whole lifetime — asymmetric (sharper below center, softer above) for the
// flat-base/round-top read a real convective cloud has. verticalNorm is the
// 0..1 distance from the puff's own vertical center; verticalSign records
// which side of center the sample fell on, since below/above shade
// differently.
float resolveVerticalDensity(
    float worldPosY, float slabBottomY, float slabTopY, float fullness,
    vec3 winningPuffHash, out float verticalNorm, out float verticalSign) {
    float verticalT = clamp((worldPosY - slabBottomY) / max(slabTopY - slabBottomY, 0.0001), 0.0, 1.0);

    float puffCenterT = mix(PUFF_VERTICAL_CENTER_MIN, PUFF_VERTICAL_CENTER_MAX, winningPuffHash.x * 0.5 + 0.5);
    float puffSpanT   = mix(PUFF_VERTICAL_SPAN_MIN, PUFF_VERTICAL_SPAN_MAX, winningPuffHash.y * 0.5 + 0.5);

    float belowSpan = puffSpanT * mix(0.45, 0.85, fullness);
    float aboveSpan = puffSpanT * mix(1.35, 1.05, fullness);

    float verticalOffset = verticalT - puffCenterT;
    verticalSign = verticalOffset < 0.0 ? -1.0 : 1.0;
    verticalNorm = verticalOffset < 0.0
    ? -verticalOffset / max(belowSpan, 0.001)
    : verticalOffset / max(aboveSpan, 0.001);

    return 1.0 - smoothstep(0.6, 1.0, verticalNorm);
}

// Shades one raymarch step's resolved sample — a soft top-lit/bottom-dark
// gradient from the fake normal, plus a silver lining that only brightens
// the cloud's own thin edges when looking roughly toward the sun through
// them (forward scattering). selfShadow darkens both ambient and direct
// terms the deeper this step sits within its own entry's already-traversed
// volume, approximating multiple-scattering falloff without an actual
// light-ray march.
vec3 shadeCloudStep(
    vec3 rayDir, vec3 fakeNormal, float puffHeight, float selfShadow, float thicknessNorm,
    vec4 colorScale, vec4 materialParams) {
    vec3 sunDir  = normalize(u_sunDirection);
    vec3 moonDir = normalize(u_moonDirection);

    float saturation = materialParams.x;

    float sunLight  = clamp(dot(fakeNormal, sunDir), 0.0, 1.0) * selfShadow;
    float moonLight = clamp(dot(fakeNormal, moonDir), 0.0, 1.0) * min(u_moonIntensity, 0.18) * selfShadow;

    vec3  moonTint    = vec3(0.58, 0.74, 1.00);
    vec3  directLight = u_sunColor * u_sunIntensity * sunLight + u_moonColor * moonTint * moonLight;

    float luminance    = dot(colorScale.rgb, vec3(0.299, 0.587, 0.114));
    vec3  tintedAlbedo = mix(vec3(luminance), colorScale.rgb, saturation);
    tintedAlbedo = mix(tintedAlbedo, tintedAlbedo * u_skyCloudColor, CLOUD_SKY_TINT_STRENGTH);

    float ambientFloor = mix(CLOUD_AMBIENT_SHADOW, CLOUD_AMBIENT_SHADOW * 0.5, thicknessNorm);
    float ambient       = mix(ambientFloor, CLOUD_AMBIENT_LIT, puffHeight) * selfShadow;

    vec3 shaded = tintedAlbedo * (ambient + directLight);
    shaded *= mix(CLOUD_STORM_DARKEN_MIN, 1.0, saturation);

    float sunAlignment  = clamp(dot(rayDir, sunDir), 0.0, 1.0);
    float edgeThinness  = 1.0 - puffHeight;
    float silverLining  = pow(sunAlignment, CLOUD_SILVER_LINING_POWER) * edgeThinness;
    shaded += u_sunColor * u_sunIntensity * silverLining * CLOUD_SILVER_LINING_STRENGTH * selfShadow;

    return shaded;
}

void main() {
    int entryCount = min(u_weatherEntryCount, WEATHER_MAP_MAX_ENTRIES);

    if (entryCount == 0)
    discard;

    vec3 rayDir = normalize(v_dir);

    if (u_weatherCloudLayerMaxY > u_weatherCloudLayerMinY) {
        bool belowBand = u_cameraPosition.y < u_weatherCloudLayerMinY && rayDir.y <= 0.0;
        bool aboveBand = u_cameraPosition.y > u_weatherCloudLayerMaxY && rayDir.y >= 0.0;
        if (belowBand || aboveBand)
        discard;
    }

    vec2 chunkOffsetBlocks = vec2(float(u_playerChunkX), float(u_playerChunkZ)) * u_chunkSize;

    vec2  driftDirNorm = u_weatherDriftDirection;
    float driftDirLen  = length(driftDirNorm);
    driftDirNorm = driftDirLen > 0.0001 ? driftDirNorm / driftDirLen : vec2(1.0, 0.0);
    float driftAngle = atan(driftDirNorm.y, driftDirNorm.x);
    float driftSpeed = u_weatherDriftSpeed;

    vec3  accumulatedColor = vec3(0.0);
    float accumulatedAlpha = 0.0;

    for (int i = 0; i < entryCount; i++) {
        if (accumulatedAlpha > CLOUD_ALPHA_SATURATION_CUTOFF)
        break;

        vec4  patternState = u_weatherPatternState[i];
        float intensity  = patternState.x;
        float fadeAlpha  = patternState.y;
        float rangeFade  = patternState.w;

        if (intensity <= CLOUD_DENSITY_EPSILON || fadeAlpha <= CLOUD_DENSITY_EPSILON || rangeFade <= CLOUD_DENSITY_EPSILON)
        continue;

        vec4 shape = u_weatherCloudShape[i];

        if (shape.z <= CLOUD_DENSITY_EPSILON)
        continue;

        vec4  bounds         = u_weatherBounds[i];
        vec2  boxCenter       = (bounds.xy + bounds.zw) * 0.5;
        float boxHalfDiagonal = length(bounds.zw - bounds.xy) * 0.5;

        if (!footprintMayBeVisible(u_cameraPosition.xz, rayDir.xz, boxCenter, boxHalfDiagonal * CLOUD_CULL_SAFETY_MARGIN, u_cloudMaxDistance))
        continue;

        vec4 variance1 = u_weatherCloudVariance1[i];

        vec3  worldPosMid;
        float tNear;
        float pathLength;
        float slabBottomY, slabTopY;

        if (!resolveCloudSlab(shape, rayDir, chunkOffsetBlocks, variance1.z,
                worldPosMid, tNear, pathLength, slabBottomY, slabTopY))
        continue;

        float farFadeStart = u_cloudMaxDistance * (1.0 - CLOUD_FAR_FADE_FRACTION);
        float farFade        = 1.0 - smoothstep(farFadeStart, u_cloudMaxDistance, tNear);

        if (farFade <= CLOUD_DENSITY_EPSILON)
        continue;

        vec4 noiseParams    = u_weatherCloudNoise[i];
        vec4 colorScale     = u_weatherCloudColorScale[i];
        vec4 materialParams = u_weatherCloudMaterial[i];
        vec4 variance0      = u_weatherCloudVariance0[i];

        vec2 offsetNorm, worldOffsetNorm;
        vec3 winningPuffHash;
        float horizontalCoverage = resolveCloudHorizontal(
            bounds, shape, noiseParams, colorScale, variance0, variance1,
            intensity, worldPosMid, driftDirNorm, driftAngle, driftSpeed,
            offsetNorm, winningPuffHash, worldOffsetNorm);

        if (horizontalCoverage <= CLOUD_DENSITY_EPSILON)
        continue;

        float fullness           = materialParams.y;
        float horizontalPuffTerm = pow(clamp(1.0 - length(offsetNorm), 0.0, 1.0), mix(2.2, 0.8, fullness));
        float thicknessNorm      = clamp(shape.x / CLOUD_AMBIENT_THICKNESS_REFERENCE_BLOCKS, 0.0, 1.0);

        int   stepCount       = clamp(int(pathLength / CLOUD_VOLUME_STEP_LENGTH_BLOCKS) + 1,
            CLOUD_VOLUME_STEP_COUNT_MIN, CLOUD_VOLUME_STEP_COUNT_MAX);
        float stepLength      = pathLength / float(stepCount);
        float travelStepRatio = stepLength / max(shape.x, CLOUD_MIN_SLAB_THICKNESS_BLOCKS);
        float ditherSeed      = hash31(vec3(v_screenPos * 371.7, float(i) * 17.1 + 3.0));

        vec3  entryColor     = vec3(0.0);
        float entryRemaining = 1.0;

        for (int s = 0; s < CLOUD_VOLUME_STEP_COUNT_MAX; s++) {
            if (s >= stepCount)
            break;

            if (entryRemaining <= CLOUD_DENSITY_EPSILON)
            break;

            float sampleOffset = (float(s) + 0.5) + (ditherSeed - 0.5) * CLOUD_VOLUME_DITHER_STRENGTH;
            float tStep         = clamp(tNear + sampleOffset * stepLength, tNear, tNear + pathLength);
            vec3  stepWorldPos  = u_cameraPosition + rayDir * tStep;

            float verticalNorm, verticalSign;
            float verticalDensity = resolveVerticalDensity(
                stepWorldPos.y, slabBottomY, slabTopY, fullness, winningPuffHash, verticalNorm, verticalSign);

            if (verticalDensity <= CLOUD_DENSITY_EPSILON)
            continue;

            float wispNoise = gradientNoise2D(
                stepWorldPos.xz * CLOUD_VOLUME_WISP_FREQUENCY + vec2(variance1.z * 13.1, stepWorldPos.y * 0.05));
            float wisp = mix(1.0 - CLOUD_VOLUME_WISP_STRENGTH, 1.0, wispNoise * 0.5 + 0.5);

            float localDensity = horizontalCoverage * verticalDensity * wisp;
            float opticalDepth = localDensity * shape.z * CLOUD_DENSITY_OPACITY_SCALE * travelStepRatio;
            float stepTransmittance = exp(-opticalDepth);
            float stepAlpha         = 1.0 - stepTransmittance;

            if (stepAlpha <= CLOUD_DENSITY_EPSILON)
            continue;

            float verticalShape = 1.0 - clamp(verticalNorm, 0.0, 1.0);
            float puffHeight    = clamp(mix(horizontalPuffTerm, verticalShape, 0.5), 0.0, 1.0);
            float lift           = verticalSign < 0.0 ? mix(-0.6, -0.15, fullness) : mix(0.3, 0.75, fullness);

            vec3 fakeNormal = normalize(vec3(
                    worldOffsetNorm.x * (1.0 - puffHeight),
                    max(puffHeight + lift, 0.05),
                    worldOffsetNorm.y * (1.0 - puffHeight)));

            float selfShadow = mix(1.0, entryRemaining, CLOUD_VOLUME_SELF_SHADOW_STRENGTH);
            vec3  stepColor   = shadeCloudStep(rayDir, fakeNormal, puffHeight, selfShadow, thicknessNorm, colorScale, materialParams);

            entryColor     += stepColor * stepAlpha * entryRemaining;
            entryRemaining *= stepTransmittance;
        }

        float entryAlpha = clamp((1.0 - entryRemaining) * fadeAlpha * rangeFade * farFade, 0.0, 1.0);

        if (entryAlpha <= CLOUD_DENSITY_EPSILON)
        continue;

        vec3  entryStraightColor = entryColor / max(1.0 - entryRemaining, 0.0001);
        float remaining           = 1.0 - accumulatedAlpha;

        accumulatedColor += entryStraightColor * entryAlpha * remaining;
        accumulatedAlpha += entryAlpha * remaining;
    }

    if (accumulatedAlpha <= 0.003)
    discard;

    vec3 straightColor = accumulatedColor / max(accumulatedAlpha, 0.0001);
    fragColor = vec4(straightColor, accumulatedAlpha);
}