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

/*
* Fullscreen weather/cloud raymarch. Reconstructs a world-space view ray
 * per pixel, intersects it against the shared cloud altitude band, and
 * marches through every in-range weather pattern's cloud entries from
 * WeatherMapData, accumulating premultiplied color and coverage.
 */

uniform float u_cloudAltitudeMin;
uniform float u_cloudAltitudeMax;
uniform float u_cloudMaxDistance;
uniform vec2  u_weatherDriftDirection;
uniform float u_weatherDriftSpeed;

const int   CLOUD_RAYMARCH_MIN_STEPS          = 40;
const int   CLOUD_RAYMARCH_MAX_STEPS          = 80;
const float CLOUD_RAYMARCH_TARGET_STEP_LENGTH = 20.0;

const float CLOUD_FAR_FADE_FRACTION = 0.2;

const float CLOUD_DENSITY_ABSORPTION   = 1.35;
const float CLOUD_TRANSMITTANCE_CUTOFF = 0.01;
const float CLOUD_DENSITY_EPSILON      = 0.001;

const float CLOUD_DRIFT_SCROLL_SCALE = 0.35;
const float CLOUD_DRIFT_TIME_SCALE   = 0.1;
const float CLOUD_PUFF_ANGLE_WOBBLE  = 1.2;
const float CLOUD_EDGE_SOFTBAND_MIN  = 0.08;
const float CLOUD_EDGE_SOFTBAND_MAX  = 0.6;
const float CLOUD_MIN_EDGE_BAND      = 0.16;
const float CLOUD_OUTER_FADE_START   = 0.85;
const float CLOUD_OUTER_FADE_END     = 1.35;

// Saturation-driven shading — applied ahead of sky tint and lighting so
// low-saturation archetypes (storm clouds) read as denser and darker
// before either of those ever get a say.
const float CLOUD_STORM_ABSORPTION_BOOST = 1.35;
const float CLOUD_STORM_DARKEN_MIN       = 0.45;
const float CLOUD_SKY_TINT_STRENGTH      = 0.35;

/*
* Coverage comes from the noise field and this entry's own coverage/
 * intensity alone, evaluated identically everywhere inside the footprint
 * — never eroded by distance from the home point — so puffs scatter
 * across the whole area a weather's coverage describes instead of
 * clumping around a single anchor. The anisotropic envelope and
 * outerFade still bound and blend out the footprint's true edge.
 */
float sampleEntryDensity(
    vec4 bounds,
    vec4 shape,
    vec4 noiseParams,
    vec4 colorScale,
    vec4 variance0,
    vec4 variance1,
    float intensity,
    vec3 worldPos,
    vec2 chunkOffsetBlocks,
    vec2 driftDirNorm,
    float driftSpeed,
    out float heightNorm) {
    float halfThickness = max(shape.x * 0.5, 0.01);
    float rawHeightT = (worldPos.y - shape.y) / halfThickness;
    heightNorm = clamp(rawHeightT * 0.5 + 0.5, 0.0, 1.0);

    if (abs(rawHeightT) > 1.0)
    return 0.0;

    vec2 boxCenter     = (bounds.xy + bounds.zw) * 0.5;
    vec2 boxHalfExtent = max((bounds.zw - bounds.xy) * 0.5, vec2(1.0));
    vec2 fromCenter    = worldPos.xz - boxCenter;

    vec2 cullExtent = boxHalfExtent * (CLOUD_OUTER_FADE_END * 2.0);
    if (abs(fromCenter.x) > cullExtent.x || abs(fromCenter.y) > cullExtent.y)
    return 0.0;

    float patternSeed    = variance1.z;
    float cloudTypeIndex = variance1.y;
    float instanceHash   = hash31(vec3(
            patternSeed * 12.9898,
            cloudTypeIndex * 78.233 + patternSeed,
            patternSeed - cloudTypeIndex * 0.577));

    float driftAngle = atan(driftDirNorm.y, driftDirNorm.x);
    float puffAngle = driftAngle + (instanceHash - 0.5) * CLOUD_PUFF_ANGLE_WOBBLE;
    float cosA = cos(puffAngle);
    float sinA = sin(puffAngle);

    vec2 rotated = vec2(
        fromCenter.x * cosA + fromCenter.y * sinA,
        fromCenter.y * cosA - fromCenter.x * sinA);

    float elongation  = clamp(mix(variance0.w, variance1.x, fract(instanceHash * 3.17)), 1.0, 6.0);
    float spreadRatio = clamp(variance0.x, 0.1, 2.0);

    vec2 anisotropicExtent = boxHalfExtent * spreadRatio * vec2(1.0, 1.0 / elongation);
    vec2 spreadNorm  = rotated / max(anisotropicExtent, vec2(1.0));
    float radialDist = length(spreadNorm);

    if (radialDist > CLOUD_OUTER_FADE_END)
    return 0.0;

    float outerFade = 1.0 - smoothstep(CLOUD_OUTER_FADE_START, CLOUD_OUTER_FADE_END, radialDist);

    float sizeVariance  = clamp(mix(variance0.y, variance0.z, fract(instanceHash * 5.63)), 0.3, 3.0);
    float instanceScale = max(colorScale.w, 1.0) * sizeVariance;

    vec2 driftScroll = driftDirNorm * driftSpeed * u_time * CLOUD_DRIFT_SCROLL_SCALE * shape.w;
    vec3 stableWorldPos = vec3(worldPos.x + chunkOffsetBlocks.x, worldPos.y, worldPos.z + chunkOffsetBlocks.y);
    vec3 evolvePos = stableWorldPos + vec3(driftScroll.x, u_time * CLOUD_DRIFT_TIME_SCALE, driftScroll.y);

    vec3 instanceOffset = vec3(patternSeed, fract(instanceHash * 7.0), fract(instanceHash * 13.0)) * 128.0;
    vec3 noisePos = evolvePos / instanceScale * max(noiseParams.x, 0.01) + instanceOffset;

    vec3 warp = cloudWarp3D(noisePos * 0.35) * noiseParams.y;
    float shapeNoise = perlinWorley3D(noisePos + warp, 1.4);

    float archetypeBias = (noiseParams.z - 0.6) * 0.5;
    float baseBias       = clamp(intensity + archetypeBias, 0.02, 0.95);
    float softness       = clamp(noiseParams.w, 0.0, 1.0);

    float insideThreshold = 1.0 - baseBias;
    float softBand        = max(CLOUD_MIN_EDGE_BAND, mix(CLOUD_EDGE_SOFTBAND_MIN, CLOUD_EDGE_SOFTBAND_MAX, softness) * baseBias);
    float coverage         = remapClamped(shapeNoise, insideThreshold, insideThreshold + softBand, 0.0, 1.0);

    return coverage * outerFade;
}

bool intersectAltitudeSlab(vec3 origin, vec3 dir, float minY, float maxY, out float tNear, out float tFar) {
    if (abs(dir.y) < 0.0001) {
        if (origin.y < minY || origin.y > maxY)
        return false;
        tNear = 0.0;
        tFar  = 1000000.0;
        return true;
    }

    float t0 = (minY - origin.y) / dir.y;
    float t1 = (maxY - origin.y) / dir.y;

    tNear = max(min(t0, t1), 0.0);
    tFar  = max(t0, t1);

    return tFar > tNear;
}

void main() {
    int entryCount = min(u_weatherEntryCount, WEATHER_MAP_MAX_ENTRIES);

    if (entryCount == 0)
    discard;

    float layerMinY = max(u_weatherCloudLayerMinY, u_cloudAltitudeMin);
    float layerMaxY = min(u_weatherCloudLayerMaxY, u_cloudAltitudeMax);

    if (layerMaxY <= layerMinY)
    discard;

    vec3 rayDir    = normalize(v_dir);
    vec3 rayOrigin = u_cameraPosition;

    vec2 chunkOffsetBlocks = vec2(float(u_playerChunkX), float(u_playerChunkZ)) * u_chunkSize;

    float tNear, tFar;
    if (!intersectAltitudeSlab(rayOrigin, rayDir, layerMinY, layerMaxY, tNear, tFar))
    discard;

    tFar = min(tFar, u_cloudMaxDistance);

    if (tFar <= tNear)
    discard;

    vec2 driftDirNorm = u_weatherDriftDirection;
    float driftDirLen = length(driftDirNorm);
    if (driftDirLen > 0.0001)
    driftDirNorm /= driftDirLen;
    else
    driftDirNorm = vec2(1.0, 0.0);
    float driftSpeed = u_weatherDriftSpeed;

    float rayLength = tFar - tNear;
    int stepCount = clamp(
        int(rayLength / CLOUD_RAYMARCH_TARGET_STEP_LENGTH),
        CLOUD_RAYMARCH_MIN_STEPS,
        CLOUD_RAYMARCH_MAX_STEPS);

    float dither = hash31(vec3(gl_FragCoord.xy, 0.0)) - 0.5;

    vec3 entryWorld = rayOrigin + rayDir * tNear;
    vec3 exitWorld  = rayOrigin + rayDir * tFar;

    vec3  stepVec      = (exitWorld - entryWorld) / float(stepCount);
    float stepLength   = length(stepVec);
    float farFadeStart = u_cloudMaxDistance * (1.0 - CLOUD_FAR_FADE_FRACTION);

    float sunFacing  = clamp(dot(vec3(0.0, 1.0, 0.0), normalize(u_sunDirection)), 0.0, 1.0);
    float moonFacing = clamp(dot(vec3(0.0, 1.0, 0.0), normalize(u_moonDirection)), 0.0, 1.0);
    vec3  moonTint   = vec3(0.58, 0.74, 1.00);
    vec3  directLight = u_sunColor * u_sunIntensity * mix(0.6, 1.0, sunFacing)
    + u_moonColor * moonTint * min(u_moonIntensity, 0.18) * mix(0.6, 1.0, moonFacing);

    float transmittance    = 1.0;
    vec3  accumulatedColor = vec3(0.0);
    float traveled         = tNear + stepLength * (0.5 + dither);
    vec3  samplePos        = entryWorld + stepVec * (0.5 + dither);

    for (int s = 0; s < stepCount; s++) {
        if (transmittance < CLOUD_TRANSMITTANCE_CUTOFF)
        break;

        float farFade = 1.0 - smoothstep(farFadeStart, u_cloudMaxDistance, traveled);

        if (farFade > CLOUD_DENSITY_EPSILON) {
            for (int i = 0; i < entryCount; i++) {
                vec4 patternState = u_weatherPatternState[i];
                float intensity  = patternState.x;
                float fadeAlpha  = patternState.y;
                float rangeFade  = patternState.w;

                if (intensity <= CLOUD_DENSITY_EPSILON || fadeAlpha <= CLOUD_DENSITY_EPSILON
                    || rangeFade <= CLOUD_DENSITY_EPSILON)
                continue;

                vec4 shape = u_weatherCloudShape[i];

                if (shape.z <= CLOUD_DENSITY_EPSILON)
                continue;

                vec4 bounds      = u_weatherBounds[i];
                vec4 noiseParams = u_weatherCloudNoise[i];
                vec4 colorScale  = u_weatherCloudColorScale[i];
                vec4 materialParams = u_weatherCloudMaterial[i];
                vec4 variance0   = u_weatherCloudVariance0[i];
                vec4 variance1   = u_weatherCloudVariance1[i];

                float heightNorm;
                float density = sampleEntryDensity(
                    bounds, shape, noiseParams, colorScale, variance0, variance1,
                    intensity, samplePos, chunkOffsetBlocks, driftDirNorm, driftSpeed, heightNorm)
                * fadeAlpha * rangeFade * farFade;

                if (density <= CLOUD_DENSITY_EPSILON)
                continue;

                float saturation = materialParams.x;
                float fullness   = materialParams.y;

                float vertShape = 1.0 - abs(heightNorm - 0.5) * 2.0;
                vertShape = pow(clamp(vertShape, 0.0, 1.0), mix(2.4, 0.6, fullness));

                if (vertShape <= 0.0)
                continue;

                // Saturation drives shading before anything else — low-
                // saturation archetypes (storm clouds) read denser and
                // darker ahead of sky tint or direct/ambient lighting.
                float stormDarkness  = 1.0 - saturation;
                float absorptionBias = mix(1.0, CLOUD_STORM_ABSORPTION_BOOST, stormDarkness);

                float stepAbsorption = clamp(
                    density * vertShape * shape.z * stepLength * CLOUD_DENSITY_ABSORPTION * absorptionBias,
                    0.0, 1.0);

                float luminance    = dot(colorScale.rgb, vec3(0.299, 0.587, 0.114));
                vec3  tintedAlbedo = mix(vec3(luminance), colorScale.rgb, saturation);
                tintedAlbedo = mix(tintedAlbedo, tintedAlbedo * u_skyCloudColor, CLOUD_SKY_TINT_STRENGTH);

                float ambient  = mix(0.10, 0.22, heightNorm);
                vec3  litColor = tintedAlbedo * (directLight * mix(0.4, 1.0, heightNorm) + ambient);
                litColor *= mix(CLOUD_STORM_DARKEN_MIN, 1.0, saturation);

                accumulatedColor += litColor * stepAbsorption * transmittance;
                transmittance    *= (1.0 - stepAbsorption);
            }
        }

        samplePos += stepVec;
        traveled  += stepLength;
    }

    float coverage = 1.0 - transmittance;

    if (coverage <= 0.003)
    discard;

    vec3 straightColor = accumulatedColor / max(coverage, 0.0001);

    fragColor = vec4(straightColor, coverage);
}