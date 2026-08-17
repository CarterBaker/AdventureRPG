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
#include "includes/CloudDome.glsl"

/*
* Cheap fake-volumetric cloud pass. Each weather-map entry is a real
 * horizontal slab now — an authored altitude plus the vertical thickness
 * already carried in cloudShape — instead of the single infinitely-thin
 * plane the old version intersected, so a view ray genuinely enters and
 * exits cloud material rather than sampling one point. Opacity comes from
 * the ray's own path length through that slab via a one-line
 * Beer-Lambert extinction, which for free produces thin wispy layers when
 * clipped edge-on, solid overcast when the whole slab is crossed, and
 * genuine fog the instant the camera's own position falls inside a slab
 * — no special casing, the same formula covers all three. Dome-bend sag
 * (CloudDome.glsl) is a pure function of horizontal distance from the
 * camera, never camera height, so each cloud type keeps its own altitude
 * band at every range and climbing a mountain only changes which slabs
 * the camera sits inside, never how the slabs are laid out. A slow
 * per-pattern noise also nudges each slab's local altitude up and down
 * across its footprint so a layer reads as gently undulating rather than
 * a flat sheet. Entries arrive from WeatherMapBufferSystem already
 * sorted nearest-first, so a simple front-to-back "over" composite still
 * gives a convincing layered sky with no per-pixel depth sort.
 */

uniform float u_cloudAltitudeMin;
uniform float u_cloudAltitudeMax;
uniform float u_cloudMaxDistance;
uniform vec2  u_weatherDriftDirection;
uniform float u_weatherDriftSpeed;

const float CLOUD_DENSITY_EPSILON         = 0.001;
const float CLOUD_ALPHA_SATURATION_CUTOFF = 0.985;

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

// Opacity now comes from path length through the slab rather than a flat
// per-entry constant — see resolveCloudSlab/main. CLOUD_DENSITY_OPACITY_SCALE
// is tuned so a straight-through pass at a cloud's own authored thickness
// reads roughly as opaque as the old flat formula did at density = 1.
const float CLOUD_DENSITY_OPACITY_SCALE             = 2.0;
const float CLOUD_AMBIENT_THICKNESS_REFERENCE_BLOCKS = 160.0;
const float CLOUD_MIN_SLAB_THICKNESS_BLOCKS          = 4.0;

const float CLOUD_HEIGHT_UNDULATION_FREQUENCY = 0.0012;
const float CLOUD_HEIGHT_UNDULATION_STRENGTH  = 0.35;
const float CLOUD_SAMPLE_LOOKAHEAD_BLOCKS     = 96.0;

// Two-pass like the old single-plane resolver: a first pass finds roughly
// where the ray crosses the slab's own unbent center altitude, purely to
// get a horizontal position to bend and undulate against; a second pass
// re-derives the true top and bottom planes at that bent, undulated
// altitude and slabs the ray against both. Both planes share one bend/
// undulation sample — the vertical gap between them is small next to the
// horizontal distances the bend operates over, so bending them
// independently would spend two more plane intersections on a difference
// too small to ever see.
bool resolveCloudSlab(
    vec4 shape, vec3 rayDir, float domeRadiusBlocks, float patternSeed, vec2 chunkOffsetBlocks,
    out vec3 worldPosMid, out float tNear, out float pathLength,
    out float slabBottomY, out float slabTopY) {
    float authoredAltitude = shape.y;
    float thickness        = max(shape.x, CLOUD_MIN_SLAB_THICKNESS_BLOCKS);
    float halfThickness    = thickness * 0.5;

    float clampedAltitude = clamp(authoredAltitude, u_cloudAltitudeMin, u_cloudAltitudeMax);

    vec2  approxXZ = u_cameraPosition.xz;
    float t0;
    if (intersectCloudDomePlane(u_cameraPosition, rayDir, clampedAltitude, t0))
    approxXZ = u_cameraPosition.xz + rayDir.xz * t0;

    float bentAltitude = resolveCloudDomeAltitude(approxXZ, clampedAltitude, domeRadiusBlocks);

    // Undulation is sampled at a world-stable coordinate (offset back by
    // chunkOffsetBlocks) so it doesn't pop when the floating origin
    // re-centers, matching the same convention sampleCloudEntry uses for
    // its own noise field.
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

    // The representative sample is biased toward the near edge of the
    // slab rather than its true midpoint — important when the camera is
    // inside a slab and pathLength runs long, so the coverage/shading
    // read reflects what's actually nearby instead of a point deep in
    // the distance.
    float sampleAhead = min(pathLength, CLOUD_SAMPLE_LOOKAHEAD_BLOCKS);
    float tSample = tNear + sampleAhead * 0.5;
    worldPosMid = u_cameraPosition + rayDir * tSample;

    return true;
}

// Coverage (silhouette) plus a fake hemisphere normal for cheap shading.
// Horizontal shaping (radial falloff from the pattern's own footprint,
// elongation, per-pattern rotation) is unchanged from the flat-sheet
// version; what's new is verticalT, the sample's normalized height
// within the slab (0 = its floor, 1 = its ceiling), which now drives
// puffHeight and the fake normal alongside the horizontal term — a puff
// genuinely reads brighter near its own top and darker near its own base
// instead of shading purely by lateral distance from the pattern center.
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
    float shapeNoise = fbmGradient2D(noisePos + vec2(warp), 3, 2.05, 0.5);

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

    vec2 chunkOffsetBlocks = vec2(float(u_playerChunkX), float(u_playerChunkZ)) * u_chunkSize;

    vec2  driftDirNorm = u_weatherDriftDirection;
    float driftDirLen  = length(driftDirNorm);
    driftDirNorm = driftDirLen > 0.0001 ? driftDirNorm / driftDirLen : vec2(1.0, 0.0);
    float driftAngle = atan(driftDirNorm.y, driftDirNorm.x);
    float driftSpeed = u_weatherDriftSpeed;

    float domeRadiusBlocks = resolveCloudDomeRadius();

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

        vec4 variance1 = u_weatherCloudVariance1[i];

        vec3  worldPosMid;
        float tNear;
        float pathLength;
        float slabBottomY, slabTopY;

        if (!resolveCloudSlab(shape, rayDir, domeRadiusBlocks, variance1.z, chunkOffsetBlocks,
                worldPosMid, tNear, pathLength, slabBottomY, slabTopY))
        continue;

        float farFadeStart = u_cloudMaxDistance * (1.0 - CLOUD_FAR_FADE_FRACTION);
        float farFade        = 1.0 - smoothstep(farFadeStart, u_cloudMaxDistance, tNear);

        if (farFade <= CLOUD_DENSITY_EPSILON)
        continue;

        vec4 bounds        = u_weatherBounds[i];
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