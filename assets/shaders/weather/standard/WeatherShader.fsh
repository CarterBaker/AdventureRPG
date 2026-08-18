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
* Cheap fake-volumetric cloud pass. Each weather-map entry is a real
 * horizontal slab — an authored altitude plus a vertical thickness — that
 * a view ray genuinely enters and exits, with opacity from the ray's own
 * path length through the slab via Beer-Lambert extinction. The dome
 * bend (see CloudDome.glsl) sags a slab's altitude toward the world's
 * fixed sea level purely as a function of the ray's elevation angle, so
 * distant cloud edges always hug the same horizon and arc up to their
 * true authored altitude overhead regardless of the camera's own height,
 * with no seam at any view angle. Within a slab, coverage comes from a
 * deterministic jittered-cell puff field (see samplePuffField) keyed off
 * the entry's own carried position and seed rather than raw threshold
 * noise, so a pattern reads as genuine little clouds with real gaps
 * between them — sparse or dense depending on this weather's own
 * authored coverage — instead of a uniformly noisy sheet, and the same
 * puffs read identically for every player looking at the same pattern.
 * A cheap ray-vs-circle test against each entry's own footprint skips
 * its slab resolve and puff-field evaluation entirely when the view ray
 * could never reach it, and a whole-ray reject against this frame's
 * overall cloud altitude band skips the loop entirely when nothing is
 * in range — both meaningful savings given up to 32 entries are
 * evaluated per pixel. Entries arrive from WeatherMapBufferSystem
 * already sorted nearest-first and already placed into their own
 * sub-region of their weather pattern's footprint, so a simple
 * front-to-back "over" composite still gives a convincing, patchy,
 * layered sky with no per-pixel depth sort.
 */

uniform float u_cloudAltitudeMin;
uniform float u_cloudAltitudeMax;
uniform float u_cloudMaxDistance;
uniform vec2  u_weatherDriftDirection;
uniform float u_weatherDriftSpeed;

const float CLOUD_DENSITY_EPSILON         = 0.001;
const float CLOUD_ALPHA_SATURATION_CUTOFF = 0.985;
const float CLOUD_CULL_SAFETY_MARGIN      = 2.2;

const float CLOUD_OUTER_FADE_START  = 0.85;
const float CLOUD_OUTER_FADE_END    = 1.35;

const float CLOUD_PUFF_ANGLE_WOBBLE  = 1.2;
const float CLOUD_DRIFT_SCROLL_SCALE = 0.35;
const float CLOUD_MORPH_TIME_SCALE   = 0.015;

// Bends a weather pattern's own outer silhouette by angle so the whole
// system doesn't read as one perfect ellipse. Stable per pattern (keyed
// off patternSeed/cloudSlotIndex, no time term), so it never shifts
// frame to frame or differs between players looking at the same pattern.
const float PATTERN_BOUNDARY_WARP_STRENGTH  = 0.22;
const float PATTERN_BOUNDARY_WARP_FREQUENCY = 2.5;

const float CLOUD_FAR_FADE_FRACTION = 0.2;

const float CLOUD_RIM_POWER         = 3.0;
const float CLOUD_RIM_STRENGTH      = 0.5;
const float CLOUD_AMBIENT_SHADOW    = 0.15;
const float CLOUD_AMBIENT_LIT       = 0.6;
const float CLOUD_SKY_TINT_STRENGTH = 0.45;
const float CLOUD_STORM_DARKEN_MIN  = 0.45;

// Opacity comes from path length through the slab rather than a flat
// per-entry constant. CLOUD_DENSITY_OPACITY_SCALE is tuned so a
// straight-through pass at a cloud's own authored thickness reads
// roughly as opaque as a flat per-entry formula would at density = 1.
const float CLOUD_DENSITY_OPACITY_SCALE             = 2.0;
const float CLOUD_AMBIENT_THICKNESS_REFERENCE_BLOCKS = 160.0;
const float CLOUD_MIN_SLAB_THICKNESS_BLOCKS          = 4.0;

const float CLOUD_HEIGHT_UNDULATION_FREQUENCY = 0.0012;
const float CLOUD_HEIGHT_UNDULATION_STRENGTH  = 0.35;
const float CLOUD_SAMPLE_LOOKAHEAD_BLOCKS     = 96.0;

// Puff-field shaping. Presence is gated per-cell by a coarse shared
// cluster field (PUFF_CLUSTER_FREQUENCY) so puffs clump into real
// patches with real gaps; PUFF_PRESENCE_SOFTBAND feathers that gate so
// patches don't pop in/out with a hard edge. PUFF_VERTICAL_* shape each
// individual puff's own vertical band within the slab, asymmetric
// (sharper below its center, softer above) for the flat-base/round-top
// read a real convective cloud has. PUFF_DETAIL_* is a purely cosmetic
// edge ripple layered on top — it never changes whether a puff exists.
const float PUFF_CLUSTER_FREQUENCY   = 0.12;
const float PUFF_PRESENCE_SOFTBAND   = 0.22;
const float PUFF_VERTICAL_CENTER_MIN = 0.18;
const float PUFF_VERTICAL_CENTER_MAX = 0.82;
const float PUFF_VERTICAL_SPAN_MIN   = 0.30;
const float PUFF_VERTICAL_SPAN_MAX   = 0.70;
const float PUFF_DETAIL_FREQUENCY    = 0.05;
const float PUFF_DETAIL_STRENGTH     = 0.28;

// Shape/blend tuning for the puff field itself. Domain-warping the grid
// before it's sampled, and summing overlapping puffs as soft radial
// fields (a cheap metaball union) instead of picking one winning disc,
// is what turns "a circle full of little circles" into a handful of
// merged, organic-looking cloud blobs.
const float PUFF_DOMAIN_WARP_FREQUENCY = 0.35;
const float PUFF_DOMAIN_WARP_STRENGTH  = 0.5;
const float PUFF_UNION_THRESHOLD       = 0.55;
const float PUFF_UNION_SOFTNESS        = 0.35;

// Cheap ray-vs-footprint-circle test used to skip an entry's slab resolve
// and puff-field evaluation entirely when the view ray could never pass
// near its footprint. The circle is the entry's own bounding box's
// circumscribed radius inflated by CLOUD_CULL_SAFETY_MARGIN, which
// comfortably covers the extra reach elongation and the outer fade band
// can add beyond the raw box — a false accept only costs one skipped
// entry's worth of wasted work, but a false reject would visibly clip a
// cloud, so this stays generous on purpose. The straight-up/straight-down
// case (no horizontal ray travel at all) falls back to "is the camera
// itself inside the footprint" instead of dividing by a near-zero length.
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

// Resolves the vertical slab a ray crosses for one weather entry. The
// dome-bent altitude is a direct function of the ray's own elevation
// angle (see CloudDome.glsl) — no plane-intersection pass is needed to
// find it. A single cheap division still estimates a horizontal sample
// position purely to drive the undulation noise below; that estimate
// only ever refines the slab's shape, never whether it bends at all, so
// it introduces no discontinuity.
bool resolveCloudSlab(
    vec4 shape, vec3 rayDir, vec2 chunkOffsetBlocks, float patternSeed,
    out vec3 worldPosMid, out float tNear, out float pathLength,
    out float slabBottomY, out float slabTopY) {
    float clampedAltitude = clamp(shape.y, u_cloudAltitudeMin, u_cloudAltitudeMax);
    float thickness       = max(shape.x, CLOUD_MIN_SLAB_THICKNESS_BLOCKS);
    float halfThickness   = thickness * 0.5;

    float bentAltitude = resolveCloudDomeAltitude(clampedAltitude, rayDir.y);

    float guardedDirY = rayDir.y >= 0.0 ? max(rayDir.y, 0.05) : min(rayDir.y, -0.05);
    float tEstimate    = max((bentAltitude - u_cameraPosition.y) / guardedDirY, 0.0);
    vec2  approxXZ      = u_cameraPosition.xz + rayDir.xz * tEstimate;

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

// One archetype's own puff field, evaluated at `localPos` — the
// fragment's sample position expressed relative to the weather pattern's
// own carried center and already rotated into its elongation frame
// (blocks). `elongation` compresses the cell grid along the cross-wind
// axis before gridding, so individual puffs read as stretched, wispy
// strands for high-elongation archetypes (Sirrus) and stay near-round
// for low-elongation ones (Cumulus), reusing the same wind-aligned frame
// the macro footprint is already shaped in. cellSizeBlocks is that
// archetype's own real puff diameter (CloudData's "scale"). Presence is
// gated per-cell by a coarse cluster field shared across the whole
// entry, so puffs clump into genuine patches with real gaps between them
// rather than tiling uniformly forever; the fragment's own cell plus its
// 3x3 neighborhood are tested (jittered-cell/Worley technique) so a
// puff straddling a cell boundary is never clipped by it. The sample
// position is domain-warped before it's ever gridded, and every
// candidate puff contributes a soft radial field summed across the
// whole neighborhood (a cheap metaball union) rather than the fragment
// simply adopting whichever single puff is closest — that union is what
// actually merges overlapping puffs into one blob instead of leaving
// each one's hard circular edge visible. Returns the winning single
// puff's own stable per-cell hash for the caller's shading math, since
// vertical banding and the fake normal only need a representative puff,
// not the full blended field.
float samplePuffField(
    vec2 localPos, float elongation, float cellSizeBlocks, float presenceThreshold, float edgeSoftness,
    float patternSeed, float archetypeSalt,
    out vec2 outOffsetNorm, out vec3 outWinningPuffHash) {
    vec2 anisoLocal = vec2(localPos.x, localPos.y * elongation);

    // Domain-warp the sample position before it is ever gridded, so the
    // cell lattice itself never lines up with a straight edge.
    vec2 warpFreq = vec2(PUFF_DOMAIN_WARP_FREQUENCY / max(cellSizeBlocks, 1.0));
    vec2 warp = vec2(
        gradientNoise2D(anisoLocal * warpFreq + vec2(patternSeed * 11.3, archetypeSalt)),
        gradientNoise2D(anisoLocal * warpFreq + vec2(archetypeSalt, patternSeed * 7.9)));
    vec2 warpedLocal = anisoLocal + warp * cellSizeBlocks * PUFF_DOMAIN_WARP_STRENGTH;

    vec2 cellSpacePos = warpedLocal / max(cellSizeBlocks, 1.0);
    vec2 cellBase     = floor(cellSpacePos);

    float fieldSum       = 0.0;
    float bestSinglePuff = 0.0;
    vec2  bestOffsetNorm = vec2(0.0);
    vec3  bestHash       = vec3(0.0);

    for (int oy = -1; oy <= 1; oy++) {
        for (int ox = -1; ox <= 1; ox++) {
            vec2 cell     = cellBase + vec2(float(ox), float(oy));
            vec2 cellSeed = cell + vec2(patternSeed * 17.3 + archetypeSalt, patternSeed * 29.9 - archetypeSalt);

            float clusterNoise = gradientNoise2D(
                cell * PUFF_CLUSTER_FREQUENCY + vec2(patternSeed * 4.7, patternSeed * 8.3));
            float presence = clusterNoise * 0.5 + 0.5;

            float presenceFade = smoothstep(
                presenceThreshold - PUFF_PRESENCE_SOFTBAND,
                presenceThreshold + PUFF_PRESENCE_SOFTBAND,
                presence);

            if (presenceFade <= CLOUD_DENSITY_EPSILON)
            continue;

            vec3 puffHash       = hash33(vec3(cellSeed, patternSeed * 3.71 + archetypeSalt));
            vec2 jitter         = puffHash.xy * 0.5 + 0.5;
            vec2 puffCenterCell = cell + jitter;

            float sizeT           = puffHash.z * 0.5 + 0.5;
            float puffRadiusCells = mix(0.42, 0.78, sizeT);

            vec2 toPuff = cellSpacePos - puffCenterCell;

            // Random per-puff squash + rotation, seeded from this puff's
            // own stable hash (no extra noise taps needed), so the field
            // is built from lopsided blobs instead of identical discs.
            float wobbleAngle  = fract(puffHash.x * 43.27 + puffHash.y * 17.61) * 6.28318;
            float wobbleAspect = mix(0.7, 1.35, fract(puffHash.z * 29.13 + puffHash.x * 11.7));
            float wca = cos(wobbleAngle);
            float wsa = sin(wobbleAngle);
            vec2  puffLocal   = vec2(toPuff.x * wca + toPuff.y * wsa, -toPuff.x * wsa + toPuff.y * wca);
            vec2  shapeOffset = vec2(puffLocal.x, puffLocal.y * wobbleAspect) / max(puffRadiusCells, 0.001);
            float distNorm    = length(shapeOffset);

            // Soft radial field (metaball), not a hard disc — squared so
            // influence concentrates near each puff's own center and
            // tapers out well before its nominal edge.
            float field = clamp(1.0 - distNorm, 0.0, 1.0);
            field = field * field * presenceFade;
            fieldSum += field;

            float singleCoverage = (1.0 - smoothstep(max(0.0001, 1.0 - edgeSoftness), 1.0, distNorm)) * presenceFade;

            if (singleCoverage > bestSinglePuff) {
                bestSinglePuff = singleCoverage;
                bestOffsetNorm = toPuff / max(puffRadiusCells, 0.001);
                bestHash       = puffHash;
            }
        }
    }

    outOffsetNorm      = bestOffsetNorm;
    outWinningPuffHash = bestHash;

    // Wispier archetypes (higher edgeSoftness, e.g. Sirrus) get a wider
    // union band so they fringe out gently; dense archetypes keep a
    // firmer, more sculpted edge.
    float band = mix(PUFF_UNION_SOFTNESS * 0.5, PUFF_UNION_SOFTNESS * 1.5, edgeSoftness);
    return smoothstep(PUFF_UNION_THRESHOLD - band, PUFF_UNION_THRESHOLD + band, fieldSum);
}

// Coverage (silhouette) plus a fake hemisphere normal for cheap shading.
// Horizontal shaping (the puff field above, elongation, per-pattern
// rotation) locates and sizes each little cloud; verticalT is the
// sample's normalized height within the slab (0 = its floor, 1 = its
// ceiling), used both to band each puff to its own stable vertical slice
// of the slab and to drive puffHeight/the fake normal alongside the
// horizontal term, so the same shape reads correctly from any angle —
// flat and dark from underneath, round and lit from the side or above.
float sampleCloudEntry(
    vec4 bounds, vec4 shape, vec4 noiseParams, vec4 colorScale, vec4 materialParams,
    vec4 variance0, vec4 variance1,
    float intensity, vec3 worldPos, float slabBottomY, float slabTopY,
    vec2 driftDirNorm, float driftAngle, float driftSpeed,
    out vec3 fakeNormal, out float puffHeight) {
    fakeNormal = vec3(0.0, 1.0, 0.0);
    puffHeight = 0.0;

    vec2 boxCenter     = (bounds.xy + bounds.zw) * 0.5;
    vec2 boxHalfExtent = max((bounds.zw - bounds.xy) * 0.5, vec2(1.0));
    vec2 fromCenter    = worldPos.xz - boxCenter;

    vec2 cullExtent = boxHalfExtent * (CLOUD_OUTER_FADE_END * 2.0);
    if (abs(fromCenter.x) > cullExtent.x || abs(fromCenter.y) > cullExtent.y)
    return 0.0;

    float patternSeed    = variance1.z;
    float cloudSlotIndex = variance1.y;
    float archetypeSalt  = cloudSlotIndex * 41.9;

    // fromCenter is already the fragment's true offset from this
    // pattern's own carried position — both share the same per-grid
    // relative origin, so the offset itself is frame-independent and
    // needs no chunk-offset correction. Rotating it into the pattern's
    // own elongation frame gives the stable local space every puff cell
    // below is placed in, so puffs translate for free as the CPU moves
    // the pattern's own bounds and read identically for every player.
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

    // Cheap reject using an inflated bound before paying for the boundary
    // warp below, which can only ever push the true edge further out.
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

    vec2 outOffsetNorm;
    vec3 winningPuffHash;
    float coverage = samplePuffField(
        rotated, elongation, cellSizeBlocks, presenceThreshold, edgeSoftness,
        patternSeed, archetypeSalt, outOffsetNorm, winningPuffHash);

    coverage *= outerFade;

    if (coverage <= CLOUD_DENSITY_EPSILON)
    return 0.0;

    // Fine detail erosion — a slow-scrolling, low-cost noise tap that
    // ripples each puff's clean edge so the silhouette reads as living
    // vapor rather than a stamped decal. Scaled by this archetype's own
    // authored densityNoiseScale/noiseWarpStrength so a turbulent cloud
    // (Cumulonimbus, Nimbus) reads visibly rougher than a smooth one
    // (Stratus). Purely cosmetic: it only ever nibbles at the outer edge
    // (1.0 - coverage), never moves a puff's center or decides whether
    // it exists, so the defined-area structure above stays fully stable.
    vec2 driftScroll = driftDirNorm * driftSpeed * u_time * CLOUD_DRIFT_SCROLL_SCALE * shape.w;
    vec2 detailPos   = (rotated + driftScroll) * (PUFF_DETAIL_FREQUENCY * max(noiseParams.x, 0.1));
    detailPos += vec2(u_time * CLOUD_MORPH_TIME_SCALE * 0.3, u_time * CLOUD_MORPH_TIME_SCALE)
    + vec2(patternSeed * 19.1, cloudSlotIndex * 53.7);
    float detail         = fbmGradient2D(detailPos, 2, 2.1, 0.5) - 0.5;
    float detailStrength = PUFF_DETAIL_STRENGTH * clamp(noiseParams.y, 0.0, 1.5);
    coverage = clamp(coverage + detail * detailStrength * (1.0 - coverage), 0.0, 1.0);

    if (coverage <= CLOUD_DENSITY_EPSILON)
    return 0.0;

    // Vertical banding — this specific puff's own stable center and
    // half-span within the slab, derived from its winning cell's hash so
    // every puff in a pattern settles at a slightly different height and
    // the whole layer reads as staggered rather than a flat sheet. The
    // band is asymmetric — narrower below its center, broader above —
    // so the puff cuts off sharply at its condensation-level base and
    // fades gradually into a rounded, thermal-plume top; fullness (this
    // archetype's own material setting) sharpens that asymmetry further
    // for the towering, stormy cloud types.
    float verticalT = clamp((worldPos.y - slabBottomY) / max(slabTopY - slabBottomY, 0.0001), 0.0, 1.0);
    float fullness   = materialParams.y;

    float puffCenterT = mix(PUFF_VERTICAL_CENTER_MIN, PUFF_VERTICAL_CENTER_MAX, winningPuffHash.x * 0.5 + 0.5);
    float puffSpanT   = mix(PUFF_VERTICAL_SPAN_MIN, PUFF_VERTICAL_SPAN_MAX, winningPuffHash.y * 0.5 + 0.5);

    float belowSpan = puffSpanT * mix(0.45, 0.85, fullness);
    float aboveSpan = puffSpanT * mix(1.35, 1.05, fullness);

    float verticalOffset = verticalT - puffCenterT;
    float verticalNorm   = verticalOffset < 0.0
    ? -verticalOffset / max(belowSpan, 0.001)
    : verticalOffset / max(aboveSpan, 0.001);

    float verticalFalloff = 1.0 - smoothstep(0.6, 1.0, verticalNorm);
    coverage *= verticalFalloff;

    if (coverage <= CLOUD_DENSITY_EPSILON)
    return 0.0;

    // Fake shading normal — horizontal roundness (distance from this
    // puff's own 2D center, unstretched back out of the elongation
    // frame) blended with the same asymmetric vertical position, so the
    // puff shades with a flat, shadowed underside and a rounded, lit
    // crown from any viewing angle, including from directly beneath it.
    float horizontalPuff = pow(clamp(1.0 - length(outOffsetNorm), 0.0, 1.0), mix(2.2, 0.8, fullness));
    float verticalShape  = 1.0 - clamp(abs(verticalNorm), 0.0, 1.0);
    puffHeight = clamp(mix(horizontalPuff, verticalShape, 0.5), 0.0, 1.0);

    vec2 approxLocalOffset = vec2(outOffsetNorm.x, outOffsetNorm.y / max(elongation, 1.0));
    vec2 worldOffsetNorm = vec2(
        approxLocalOffset.x * cosA - approxLocalOffset.y * sinA,
        approxLocalOffset.x * sinA + approxLocalOffset.y * cosA);
    float lift = verticalOffset < 0.0 ? mix(-0.6, -0.15, fullness) : mix(0.3, 0.75, fullness);
    fakeNormal = normalize(vec3(
            worldOffsetNorm.x * (1.0 - puffHeight),
            max(puffHeight + lift, 0.05),
            worldOffsetNorm.y * (1.0 - puffHeight)));

    return coverage;
}

vec3 shadeCloudEntry(vec3 worldPos, vec3 fakeNormal, float puffHeight, float thicknessNorm,
    vec4 colorScale, vec4 materialParams) {
    vec3 sunDir  = normalize(u_sunDirection);
    vec3 moonDir = normalize(u_moonDirection);

    float saturation = materialParams.x;

    float sunLight  = clamp(dot(fakeNormal, sunDir), 0.0, 1.0);
    float moonLight = clamp(dot(fakeNormal, moonDir), 0.0, 1.0) * min(u_moonIntensity, 0.18);

    vec3 moonTint    = vec3(0.58, 0.74, 1.00);
    vec3 directLight = u_sunColor * u_sunIntensity * sunLight + u_moonColor * moonTint * moonLight;

    float luminance    = dot(colorScale.rgb, vec3(0.299, 0.587, 0.114));
    vec3  tintedAlbedo = mix(vec3(luminance), colorScale.rgb, saturation);
    tintedAlbedo = mix(tintedAlbedo, tintedAlbedo * u_skyCloudColor, CLOUD_SKY_TINT_STRENGTH);

    float ambientFloor = mix(CLOUD_AMBIENT_SHADOW, CLOUD_AMBIENT_SHADOW * 0.5, thicknessNorm);
    float ambient       = mix(ambientFloor, CLOUD_AMBIENT_LIT, puffHeight);

    vec3 shaded = tintedAlbedo * (ambient + directLight);
    shaded *= mix(CLOUD_STORM_DARKEN_MIN, 1.0, saturation);

    vec3  viewDir = normalize(u_cameraPosition - worldPos);
    float rim      = pow(1.0 - clamp(dot(fakeNormal, viewDir), 0.0, 1.0), CLOUD_RIM_POWER);
    vec3  rimTint  = mix(u_sunColor, vec3(1.0), 0.5);
    shaded += rimTint * rim * CLOUD_RIM_STRENGTH * mix(0.35, 1.0, sunLight);

    return shaded;
}

void main() {
    int entryCount = min(u_weatherEntryCount, WEATHER_MAP_MAX_ENTRIES);

    if (entryCount == 0)
    discard;

    vec3 rayDir = normalize(v_dir);

    // Cheap whole-ray reject: every entry written this frame lives inside
    // [u_weatherCloudLayerMinY, u_weatherCloudLayerMaxY] (see
    // WeatherMapBufferSystem), so a ray that can never climb or descend
    // into that band has nothing to raymarch at all.
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
        float intensity     = patternState.x;
        float fadeAlpha     = patternState.y;
        float rangeFade     = patternState.w;

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

        vec3  fakeNormal;
        float puffHeight;
        float coverage = sampleCloudEntry(
            bounds, shape, noiseParams, colorScale, materialParams, variance0, variance1,
            intensity, worldPosMid, slabBottomY, slabTopY,
            driftDirNorm, driftAngle, driftSpeed,
            fakeNormal, puffHeight);

        if (coverage <= CLOUD_DENSITY_EPSILON)
        continue;

        float thicknessNorm  = clamp(shape.x / CLOUD_AMBIENT_THICKNESS_REFERENCE_BLOCKS, 0.0, 1.0);
        float travelRatio    = pathLength / max(shape.x, CLOUD_MIN_SLAB_THICKNESS_BLOCKS);
        float opticalDepth   = shape.z * CLOUD_DENSITY_OPACITY_SCALE * travelRatio;
        float densityOpacity = 1.0 - exp(-opticalDepth);
        float entryAlpha      = clamp(coverage * densityOpacity * fadeAlpha * rangeFade * farFade, 0.0, 1.0);

        if (entryAlpha <= CLOUD_DENSITY_EPSILON)
        continue;

        vec3 entryColor = shadeCloudEntry(worldPosMid, fakeNormal, puffHeight, thicknessNorm, colorScale, materialParams);

        float remaining = 1.0 - accumulatedAlpha;
        accumulatedColor += entryColor * entryAlpha * remaining;
        accumulatedAlpha += entryAlpha * remaining;
    }

    if (accumulatedAlpha <= 0.003)
    discard;

    vec3 straightColor = accumulatedColor / max(accumulatedAlpha, 0.0001);
    fragColor = vec4(straightColor, accumulatedAlpha);
}