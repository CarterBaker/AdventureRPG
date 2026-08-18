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
 * bend (see CloudDome.glsl) sags a slab's altitude toward the camera's
 * own eye level purely as a function of the ray's elevation angle, never
 * a horizontal world-space distance, so distant cloud edges hug the
 * horizon and arc up to their true authored altitude overhead with no
 * plane-intersection precondition and therefore no seam at any view
 * angle, including straight up. A cheap ray-vs-circle test against each
 * entry's own footprint skips its slab resolve and noise sampling
 * entirely when the view ray could never reach it, and a whole-ray reject
 * against this frame's overall cloud altitude band skips the loop
 * entirely when nothing is in range — both meaningful savings given up
 * to 32 entries are evaluated per pixel. Entries arrive from
 * WeatherMapBufferSystem already sorted nearest-first and already placed
 * into their own sub-region of their weather pattern's footprint, so a
 * simple front-to-back "over" composite still gives a convincing, patchy,
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
const float CLOUD_MIN_EDGE_BAND     = 0.16;
const float CLOUD_EDGE_SOFTBAND_MIN = 0.08;
const float CLOUD_EDGE_SOFTBAND_MAX = 0.6;

const float CLOUD_PUFF_ANGLE_WOBBLE  = 1.2;
const float CLOUD_DRIFT_SCROLL_SCALE = 0.35;
const float CLOUD_MORPH_TIME_SCALE   = 0.015;

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

// Cheap ray-vs-footprint-circle test used to skip an entry's slab resolve
// and noise sampling entirely when the view ray could never pass near its
// footprint. The circle is the entry's own bounding box's circumscribed
// radius inflated by CLOUD_CULL_SAFETY_MARGIN, which comfortably covers
// the extra reach elongation and the outer fade band can add beyond the
// raw box — a false accept only costs one skipped entry's worth of
// wasted work, but a false reject would visibly clip a cloud, so this
// stays generous on purpose. The straight-up/straight-down case (no
// horizontal ray travel at all) falls back to "is the camera itself
// inside the footprint" instead of dividing by a near-zero length.
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
// dome-bent altitude is now a direct function of the ray's own elevation
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

    float bentAltitude = resolveCloudDomeAltitude(clampedAltitude, u_cameraPosition.y, rayDir.y);

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

// Coverage (silhouette) plus a fake hemisphere normal for cheap shading.
// Horizontal shaping (radial falloff from the pattern's own sub-region,
// elongation, per-pattern rotation) is unchanged; verticalT is the
// sample's normalized height within the slab (0 = its floor, 1 = its
// ceiling), driving puffHeight and the fake normal alongside the
// horizontal term.
float sampleCloudEntry(
    vec4 bounds, vec4 shape, vec4 noiseParams, vec4 colorScale, vec4 materialParams,
    vec4 variance0, vec4 variance1,
    float intensity, vec3 worldPos, float slabBottomY, float slabTopY, vec2 chunkOffsetBlocks,
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
    float instanceHash   = hash31(vec3(
            patternSeed * 12.9898,
            cloudSlotIndex * 78.233 + patternSeed,
            patternSeed - cloudSlotIndex * 0.577));

    float puffAngle = driftAngle + (instanceHash - 0.5) * CLOUD_PUFF_ANGLE_WOBBLE;
    float cosA = cos(puffAngle);
    float sinA = sin(puffAngle);
    vec2  rotated = vec2(
        fromCenter.x * cosA + fromCenter.y * sinA,
        fromCenter.y * cosA - fromCenter.x * sinA);

    float elongation  = clamp(mix(variance0.w, variance1.x, fract(instanceHash * 3.17)), 1.0, 6.0);
    float spreadRatio = clamp(variance0.x, 0.1, 2.0);
    vec2  anisotropicExtent = boxHalfExtent * spreadRatio * vec2(1.0, 1.0 / elongation);
    vec2  spreadNorm  = rotated / max(anisotropicExtent, vec2(1.0));
    float radialDist  = length(spreadNorm);

    if (radialDist > CLOUD_OUTER_FADE_END)
    return 0.0;

    float outerFade = 1.0 - smoothstep(CLOUD_OUTER_FADE_START, CLOUD_OUTER_FADE_END, radialDist);

    float sizeVariance  = clamp(mix(variance0.y, variance0.z, fract(instanceHash * 5.63)), 0.3, 3.0);
    float instanceScale = max(colorScale.w, 1.0) * sizeVariance;

    vec2 driftScroll    = driftDirNorm * driftSpeed * u_time * CLOUD_DRIFT_SCROLL_SCALE * shape.w;
    vec2 stableWorldXZ  = worldPos.xz + chunkOffsetBlocks;
    vec3 instanceOffset = vec3(patternSeed, fract(instanceHash * 7.0), fract(instanceHash * 13.0)) * 128.0;

    vec2 noisePos = (stableWorldXZ + driftScroll) / instanceScale * max(noiseParams.x, 0.01) + instanceOffset.xy;
    noisePos += vec2(u_time * CLOUD_MORPH_TIME_SCALE * 0.3, u_time * CLOUD_MORPH_TIME_SCALE);

    float warp       = gradientNoise2D(noisePos * 0.5 + instanceOffset.zy) * noiseParams.y;
    float shapeNoise = fbmGradient2D(noisePos + vec2(warp), 2, 2.05, 0.5);

    float archetypeBias  = (noiseParams.z - 0.6) * 0.5;
    float baseBias         = clamp(intensity + archetypeBias, 0.02, 0.95);
    float softness         = clamp(noiseParams.w, 0.0, 1.0);
    float insideThreshold   = 1.0 - baseBias;
    float softBand          = max(CLOUD_MIN_EDGE_BAND, mix(CLOUD_EDGE_SOFTBAND_MIN, CLOUD_EDGE_SOFTBAND_MAX, softness) * baseBias);

    float coverage = remapClamped(shapeNoise, insideThreshold, insideThreshold + softBand, 0.0, 1.0) * outerFade;

    if (coverage <= CLOUD_DENSITY_EPSILON)
    return 0.0;

    float verticalT = clamp((worldPos.y - slabBottomY) / max(slabTopY - slabBottomY, 0.0001), 0.0, 1.0);

    float fullness      = materialParams.y;
    float roundPow        = mix(2.2, 0.7, fullness);
    float horizontalPuff  = pow(clamp(1.0 - radialDist, 0.0, 1.0), roundPow);
    puffHeight = clamp(mix(horizontalPuff, verticalT, 0.55), 0.0, 1.0);

    vec2  normalXZ     = radialDist > 0.0001 ? normalize(fromCenter) * (1.0 - puffHeight) : vec2(0.0);
    float verticalLift  = mix(-0.4, 0.6, verticalT);
    fakeNormal = normalize(vec3(normalXZ.x, max(puffHeight + verticalLift, 0.05), normalXZ.y));

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
            intensity, worldPosMid, slabBottomY, slabTopY, chunkOffsetBlocks,
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