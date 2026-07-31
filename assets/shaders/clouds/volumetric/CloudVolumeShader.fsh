#version 330 core

in vec3 vLocalPos;

out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"

uniform vec3 u_boxCenter;
uniform vec3 u_boxHalfExtent;

const int   CLOUD_RAYMARCH_STEPS       = 28;
const float CLOUD_DENSITY_ABSORPTION   = 1.35;
const float CLOUD_TRANSMITTANCE_CUTOFF = 0.01;
const float CLOUD_DENSITY_EPSILON      = 0.001;

// Local horizontal/coverage density for one weather-map entry at a world
// sample position. heightNorm is written out so the caller can reuse it
// for that same entry's vertical falloff and ambient lighting without
// re-deriving it from the entry's own altitude/thickness a second time.
float sampleEntryDensity(
    vec4 bounds,
    vec4 shape,
    vec4 noiseParams,
    vec4 colorScale,
    float intensity,
    vec3 worldPos,
    out float heightNorm) {
    float footprintRadius = max(bounds.z, 1.0);
    vec2 planarNorm = (worldPos.xz - bounds.xy) / footprintRadius;

    float edgeDist = 1.0 - max(abs(planarNorm.x), abs(planarNorm.y));
    float softness = max(noiseParams.w, 0.02);
    float edgeShape = smoothstep(0.0, softness, edgeDist);

    float halfThickness = max(shape.x * 0.5, 0.01);
    heightNorm = clamp(((worldPos.y - shape.y) / halfThickness) * 0.5 + 0.5, 0.0, 1.0);

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

void main() {
    vec3 localCamPos  = (u_cameraPosition - u_boxCenter) / u_boxHalfExtent;
    vec3 localFragPos = vLocalPos * 2.0;
    vec3 rayDirLocal  = normalize(localFragPos - localCamPos);

    vec3 invDir = 1.0 / rayDirLocal;
    vec3 t0 = (vec3(-1.0) - localCamPos) * invDir;
    vec3 t1 = (vec3(1.0) - localCamPos) * invDir;
    vec3 tMinV = min(t0, t1);
    vec3 tMaxV = max(t0, t1);
    float tNear = max(max(tMinV.x, tMinV.y), tMinV.z);
    float tFar  = min(min(tMaxV.x, tMaxV.y), tMaxV.z);

    if (tFar <= max(tNear, 0.0))
    discard;

    tNear = max(tNear, 0.0);

    vec3 entryWorld = u_boxCenter + (localCamPos + rayDirLocal * tNear) * u_boxHalfExtent;
    vec3 exitWorld  = u_boxCenter + (localCamPos + rayDirLocal * tFar)  * u_boxHalfExtent;

    // Entries are written nearest-first every frame, so the near-range
    // cutoff is just wherever that sorted distance first exceeds it.
    int entryCount = min(u_weatherEntryCount, WEATHER_MAP_MAX_ENTRIES);
    int nearEntryCount = 0;

    for (int i = 0; i < entryCount; i++) {
        if (u_weatherPatternState[i].x > u_weatherNearRangeChunks)
        break;
        nearEntryCount++;
    }

    if (nearEntryCount == 0)
    discard;

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

        for (int i = 0; i < nearEntryCount; i++) {
            vec4 patternState = u_weatherPatternState[i];
            float intensity  = patternState.y;
            float fadeAlpha  = patternState.w;

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