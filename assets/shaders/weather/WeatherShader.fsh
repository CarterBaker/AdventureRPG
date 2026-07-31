#version 330 core

in vec3 v_dir;
in vec2 v_screenPos;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"

uniform float u_cloudAltitudeMin;
uniform float u_cloudAltitudeMax;
uniform float u_cloudMaxDistance;

const int   CLOUD_RAYMARCH_STEPS       = 28;
const float CLOUD_DENSITY_ABSORPTION   = 1.35;
const float CLOUD_TRANSMITTANCE_CUTOFF = 0.01;
const float CLOUD_DENSITY_EPSILON      = 0.001;

// heightNorm is written out for the caller's vertical falloff/ambient use.
float sampleEntryDensity(
    vec4 bounds,
    vec4 shape,
    vec4 noiseParams,
    vec4 colorScale,
    float intensity,
    vec3 worldPos,
    out float heightNorm) {
    float halfThickness = max(shape.x * 0.5, 0.01);
    float rawHeightT = (worldPos.y - shape.y) / halfThickness;
    heightNorm = clamp(rawHeightT * 0.5 + 0.5, 0.0, 1.0);

    if (abs(rawHeightT) > 1.0)
    return 0.0;

    float footprintRadius = max(bounds.z, 1.0);
    vec2 planarNorm = (worldPos.xz - bounds.xy) / footprintRadius;

    float edgeDist = 1.0 - max(abs(planarNorm.x), abs(planarNorm.y));
    float softness = max(noiseParams.w, 0.02);
    float edgeShape = smoothstep(0.0, softness, edgeDist);

    if (edgeShape <= 0.0)
    return 0.0;

    vec2 windDrift = u_windDriftOffset * shape.w;
    float scale = max(colorScale.w, 1.0);
    vec3 evolvePos = worldPos + vec3(windDrift.x, u_time * 0.4, windDrift.y);
    vec3 noisePos  = evolvePos / scale * max(noiseParams.x, 0.01);

    vec3 warp = curlNoise3D(noisePos * 0.6) * noiseParams.y;
    float shapeNoise = perlinWorley3D(noisePos + warp, 3.0);

    float effectiveBias = clamp(noiseParams.z * mix(0.5, 1.0, intensity), 0.0, 0.98);
    float coverage = remapClamped(shapeNoise, 1.0 - effectiveBias, 1.0, 0.0, 1.0);

    return coverage * edgeShape;
}

// Ray-vs-horizontal-slab test — the fullscreen pass has no box mesh to
// bound it anymore, so the raymarch range comes from where the camera
// ray crosses the shared min/max cloud altitude planes instead.
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
    vec3 rayDir    = normalize(v_dir);
    vec3 rayOrigin = u_cameraPosition;

    float tNear, tFar;
    if (!intersectAltitudeSlab(rayOrigin, rayDir, u_cloudAltitudeMin, u_cloudAltitudeMax, tNear, tFar))
    discard;

    tFar = min(tFar, u_cloudMaxDistance);

    if (tFar <= tNear)
    discard;

    // Every entry the CPU wrote this frame already passed the near-range
    // cull in WeatherMapBufferSystem — no per-entry range check needed here.
    int entryCount = min(u_weatherEntryCount, WEATHER_MAP_MAX_ENTRIES);

    if (entryCount == 0)
    discard;

    vec3 entryWorld = rayOrigin + rayDir * tNear;
    vec3 exitWorld  = rayOrigin + rayDir * tFar;

    vec3  stepVec    = (exitWorld - entryWorld) / float(CLOUD_RAYMARCH_STEPS);
    float stepLength = length(stepVec);

    float sunFacing  = clamp(dot(vec3(0.0, 1.0, 0.0), normalize(u_sunDirection)), 0.0, 1.0);
    float moonFacing = clamp(dot(vec3(0.0, 1.0, 0.0), normalize(u_moonDirection)), 0.0, 1.0);
    vec3  moonTint   = vec3(0.58, 0.74, 1.00);
    vec3  directLight = u_sunColor * u_sunIntensity * mix(0.6, 1.0, sunFacing)
    + u_moonColor * moonTint * min(u_moonIntensity, 0.18) * mix(0.6, 1.0, moonFacing);

    float transmittance    = 1.0;
    vec3  accumulatedColor = vec3(0.0);
    vec3  samplePos        = entryWorld + stepVec * 0.5;

    for (int s = 0; s < CLOUD_RAYMARCH_STEPS; s++) {
        if (transmittance < CLOUD_TRANSMITTANCE_CUTOFF)
        break;

        for (int i = 0; i < entryCount; i++) {
            vec4 patternState = u_weatherPatternState[i];
            float intensity  = patternState.x;
            float fadeAlpha  = patternState.y;

            if (intensity <= CLOUD_DENSITY_EPSILON || fadeAlpha <= CLOUD_DENSITY_EPSILON)
            continue;

            vec4 shape = u_weatherCloudShape[i];

            if (shape.z <= CLOUD_DENSITY_EPSILON)
            continue;

            vec4 bounds      = u_weatherBounds[i];
            vec4 noiseParams = u_weatherCloudNoise[i];
            vec4 colorScale  = u_weatherCloudColorScale[i];
            vec4 materialParams = u_weatherCloudMaterial[i];

            float heightNorm;
            float density = sampleEntryDensity(
                bounds, shape, noiseParams, colorScale, intensity, samplePos, heightNorm) * fadeAlpha;

            if (density <= CLOUD_DENSITY_EPSILON)
            continue;

            float fullness  = materialParams.y;
            float vertShape = 1.0 - abs(heightNorm - 0.5) * 2.0;
            vertShape = pow(clamp(vertShape, 0.0, 1.0), mix(2.4, 0.6, fullness));

            if (vertShape <= 0.0)
            continue;

            float stepAbsorption = clamp(
                density * vertShape * shape.z * stepLength * CLOUD_DENSITY_ABSORPTION, 0.0, 1.0);

            float luminance    = dot(colorScale.rgb, vec3(0.299, 0.587, 0.114));
            vec3  tintedAlbedo = mix(vec3(luminance), colorScale.rgb, materialParams.x);
            tintedAlbedo = mix(tintedAlbedo, tintedAlbedo * u_skyCloudColor, 0.35);

            float ambient  = mix(0.10, 0.22, heightNorm);
            vec3  litColor = tintedAlbedo * (directLight * mix(0.4, 1.0, heightNorm) + ambient);

            accumulatedColor += litColor * stepAbsorption * transmittance;
            transmittance    *= (1.0 - stepAbsorption);
        }

        samplePos += stepVec;
    }

    float coverage = 1.0 - transmittance;

    if (coverage <= 0.003)
    discard;

    fragColor = vec4(accumulatedColor, coverage);
}