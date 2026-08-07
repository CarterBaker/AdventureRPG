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

// Cheap stand-in for the old volumetric cloud raymarch. Each weather-map
// entry is resolved to a single point on its own dome-bent altitude plane
// (two ray/plane intersections, no stepping), shaded as a soft rim-lit
// puff using a 2D layered noise field and an analytically derived fake
// normal (no extra noise taps). Entries arrive from WeatherMapBufferSystem
// already sorted nearest-first, so a simple front-to-back "over" composite
// gives a convincing layered sky without any per-pixel depth sort.

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

const float CLOUD_DOME_RADIUS_SCALE       = 0.85;
const float CLOUD_DOME_CURVE_POWER        = 0.65;
const float CLOUD_DOME_HORIZON_DIP_BLOCKS = 60.0;

const float CLOUD_FAR_FADE_FRACTION = 0.2;

const float CLOUD_RIM_POWER                  = 3.0;
const float CLOUD_RIM_STRENGTH               = 0.5;
const float CLOUD_AMBIENT_SHADOW             = 0.15;
const float CLOUD_AMBIENT_LIT                = 0.6;
const float CLOUD_SKY_TINT_STRENGTH          = 0.45;
const float CLOUD_STORM_DARKEN_MIN           = 0.45;
const float CLOUD_DENSITY_OPACITY_SCALE      = 1.5;
const float CLOUD_REFERENCE_THICKNESS_BLOCKS = 140.0;

bool intersectPlaneY(vec3 origin, vec3 dir, float planeY, out float t) {
    if (abs(dir.y) < 0.0001)
    return false;
    t = (planeY - origin.y) / dir.y;
    return t > 0.0;
}

float computeDomeT(vec2 worldXZ, float domeRadiusBlocks) {
    vec2  fromCameraXZ = worldXZ - u_cameraPosition.xz;
    float domeRadiusSq  = domeRadiusBlocks * domeRadiusBlocks;
    float domeBiasedT   = pow(clamp(dot(fromCameraXZ, fromCameraXZ) / domeRadiusSq, 0.0, 1.0), CLOUD_DOME_CURVE_POWER);
    return smoothstep(0.0, 1.0, domeBiasedT);
}

// Two-pass dome-bent plane solve: first pass finds roughly where the ray
// would hit the entry's authored altitude, which gives enough horizontal
// distance to evaluate the dome bend; second pass re-intersects at the
// bent altitude. Replaces the old per-step slab march with two divisions.
bool resolveCloudPlane(float authoredAltitude, vec3 rayDir, float domeRadiusBlocks,
    out vec3 worldPos, out float travelDistance) {
    float clampedAltitude = clamp(authoredAltitude, u_cloudAltitudeMin, u_cloudAltitudeMax);

    float t0;
    if (!intersectPlaneY(u_cameraPosition, rayDir, clampedAltitude, t0))
    return false;

    vec2  approxXZ         = u_cameraPosition.xz + rayDir.xz * t0;
    float domeT             = computeDomeT(approxXZ, domeRadiusBlocks);
    float domeFloorAltitude = u_cameraPosition.y - CLOUD_DOME_HORIZON_DIP_BLOCKS;
    float domeAltitude      = mix(clampedAltitude, domeFloorAltitude, domeT);

    float t1;
    if (!intersectPlaneY(u_cameraPosition, rayDir, domeAltitude, t1))
    return false;

    if (t1 >= u_cloudMaxDistance)
    return false;

    worldPos       = u_cameraPosition + rayDir * t1;
    travelDistance = t1;
    return true;
}

// Coverage plus a fake hemisphere normal, derived analytically from the
// puff's radial falloff rather than from extra noise samples — the
// "approximated 2D simulating 3D" puff bulge used for rim/ambient shading.
float sampleCloudEntry(
    vec4 bounds, vec4 shape, vec4 noiseParams, vec4 colorScale, vec4 materialParams,
    vec4 variance0, vec4 variance1,
    float intensity, vec3 worldPos, vec2 chunkOffsetBlocks,
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

    float patternSeed   = variance1.z;
    float cloudSlotIndex = variance1.y;
    float instanceHash  = hash31(vec3(
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

    vec2 driftScroll   = driftDirNorm * driftSpeed * u_time * CLOUD_DRIFT_SCROLL_SCALE * shape.w;
    vec2 stableWorldXZ = worldPos.xz + chunkOffsetBlocks;
    vec3 instanceOffset = vec3(patternSeed, fract(instanceHash * 7.0), fract(instanceHash * 13.0)) * 128.0;

    vec2 noisePos = (stableWorldXZ + driftScroll) / instanceScale * max(noiseParams.x, 0.01) + instanceOffset.xy;
    noisePos += vec2(u_time * CLOUD_MORPH_TIME_SCALE * 0.3, u_time * CLOUD_MORPH_TIME_SCALE);

    float warp       = gradientNoise2D(noisePos * 0.5 + instanceOffset.zy) * noiseParams.y;
    float shapeNoise = fbmGradient2D(noisePos + vec2(warp), 3, 2.05, 0.5);

    float archetypeBias = (noiseParams.z - 0.6) * 0.5;
    float baseBias       = clamp(intensity + archetypeBias, 0.02, 0.95);
    float softness       = clamp(noiseParams.w, 0.0, 1.0);
    float insideThreshold = 1.0 - baseBias;
    float softBand        = max(CLOUD_MIN_EDGE_BAND, mix(CLOUD_EDGE_SOFTBAND_MIN, CLOUD_EDGE_SOFTBAND_MAX, softness) * baseBias);

    float coverage = remapClamped(shapeNoise, insideThreshold, insideThreshold + softBand, 0.0, 1.0) * outerFade;

    if (coverage <= CLOUD_DENSITY_EPSILON)
    return 0.0;

    float fullness    = materialParams.y;
    float roundPow     = mix(2.2, 0.7, fullness);
    float smoothHeight = pow(clamp(1.0 - radialDist, 0.0, 1.0), roundPow);
    puffHeight = clamp(smoothHeight * mix(0.8, 1.2, shapeNoise), 0.0, 1.0);

    float horizMag = 1.0 - puffHeight;
    vec2  normalXZ = radialDist > 0.0001 ? normalize(fromCenter) * horizMag : vec2(0.0);
    fakeNormal = normalize(vec3(normalXZ.x, puffHeight + 0.05, normalXZ.y));

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

    float domeRadiusBlocks = max(u_weatherRangeBlocks * CLOUD_DOME_RADIUS_SCALE, 1.0);

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

        vec3  worldPos;
        float travelDistance;
        if (!resolveCloudPlane(shape.y, rayDir, domeRadiusBlocks, worldPos, travelDistance))
        continue;

        float farFadeStart = u_cloudMaxDistance * (1.0 - CLOUD_FAR_FADE_FRACTION);
        float farFade       = 1.0 - smoothstep(farFadeStart, u_cloudMaxDistance, travelDistance);

        if (farFade <= CLOUD_DENSITY_EPSILON)
        continue;

        vec4 bounds        = u_weatherBounds[i];
        vec4 noiseParams    = u_weatherCloudNoise[i];
        vec4 colorScale     = u_weatherCloudColorScale[i];
        vec4 materialParams = u_weatherCloudMaterial[i];
        vec4 variance0      = u_weatherCloudVariance0[i];
        vec4 variance1      = u_weatherCloudVariance1[i];

        vec3  fakeNormal;
        float puffHeight;
        float coverage = sampleCloudEntry(
            bounds, shape, noiseParams, colorScale, materialParams, variance0, variance1,
            intensity, worldPos, chunkOffsetBlocks, driftDirNorm, driftAngle, driftSpeed,
            fakeNormal, puffHeight);

        if (coverage <= CLOUD_DENSITY_EPSILON)
        continue;

        float thicknessNorm  = clamp(shape.x / CLOUD_REFERENCE_THICKNESS_BLOCKS, 0.0, 1.0);
        float densityOpacity = clamp(shape.z * CLOUD_DENSITY_OPACITY_SCALE, 0.0, 1.0);
        float entryAlpha      = clamp(coverage * densityOpacity * fadeAlpha * rangeFade * farFade, 0.0, 1.0);

        if (entryAlpha <= CLOUD_DENSITY_EPSILON)
        continue;

        vec3 entryColor = shadeCloudEntry(worldPos, fakeNormal, puffHeight, thicknessNorm, colorScale, materialParams);

        float remaining = 1.0 - accumulatedAlpha;
        accumulatedColor += entryColor * entryAlpha * remaining;
        accumulatedAlpha += entryAlpha * remaining;
    }

    if (accumulatedAlpha <= 0.003)
    discard;

    vec3 straightColor = accumulatedColor / max(accumulatedAlpha, 0.0001);
    fragColor = vec4(straightColor, accumulatedAlpha);
}