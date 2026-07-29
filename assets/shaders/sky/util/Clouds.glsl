#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL

/*
* Distant/horizon cloud pass for the sky dome. Every active weather
 * pattern's cloud entries are read straight from WeatherMapData and flat-
 * plane-intersected against the view ray at that cloud's own altitude —
 * clouds near the horizon read large, clouds further round the ring
 * compress with distance, purely a byproduct of the geometry. No clamping
 * to a ring shape happens here. Volumetric near-range shading is
 * CloudVolumeShader's job; this stays a cheap 2D silhouette.
 */

#include "includes/CameraData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"
#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/NoiseUtility.glsl"

const float SKY_CLOUD_MIN_RAY_Y      = 0.001;
const float SKY_CLOUD_TINT_STRENGTH  = 0.35;
const float SKY_CLOUD_HORIZON_FADE   = 0.5;
const float SKY_CLOUD_ALPHA_SATURATE = 0.98;

bool skyCloudIntersectPlane(vec3 dir, float relativeAltitude, out vec2 hitXZ) {
    if (abs(dir.y) < SKY_CLOUD_MIN_RAY_Y)
    return false;

    float t = relativeAltitude / dir.y;

    if (t <= 0.0)
    return false;

    hitXZ = u_cameraPosition.xz + vec2(dir.x, dir.z) * t;
    return true;
}

float skyCloudFootprint(vec2 hitXZ, vec2 center, vec2 halfExtent, float softness) {
    vec2 norm = (hitXZ - center) / max(halfExtent, vec2(0.01));
    float edgeDist = 1.0 - max(abs(norm.x), abs(norm.y));
    return smoothstep(0.0, max(softness, 0.02), edgeDist);
}

float skyCloudShape(vec2 hitXZ, vec2 windDrift, vec4 noiseParams, float scale, float fullness, float intensity) {
    vec3 basePos  = vec3(hitXZ.x, 0.0, hitXZ.y);
    vec3 evolved  = basePos + vec3(windDrift.x, u_time * 0.4, windDrift.y);
    vec3 noisePos = evolved / max(scale, 1.0) * max(noiseParams.x, 0.01);

    vec3 warp = curlNoise3D(noisePos * 0.6) * noiseParams.y;
    float shapeNoise = perlinWorley3D(noisePos + warp, 3.0);

    float effectiveBias = clamp(noiseParams.z * mix(0.5, 1.0, intensity), 0.0, 0.98);
    float coverage = remapClamped(shapeNoise, 1.0 - effectiveBias, 1.0, 0.0, 1.0);

    return pow(coverage, mix(1.6, 0.7, fullness));
}

vec4 calculateClouds(vec3 dir) {
    vec3  accumulatedColor = vec3(0.0);
    float accumulatedAlpha = 0.0;

    float chunkSizeBlocks = u_chunkSize;
    vec2  windDrift = u_windDriftOffset;

    for (int i = 0; i < u_weatherEntryCount && i < WEATHER_MAP_MAX_ENTRIES; i++) {
        if (accumulatedAlpha >= SKY_CLOUD_ALPHA_SATURATE)
        break;

        vec4 patternState = u_weatherPatternState[i];
        float intensity = patternState.y;
        float fadeAlpha  = patternState.w;

        if (intensity <= 0.001 || fadeAlpha <= 0.001)
        continue;

        vec4 cloudShapeData = u_weatherCloudShape[i];
        float relativeAltitude = cloudShapeData.y - u_cameraPosition.y;

        vec2 hitXZ;
        if (!skyCloudIntersectPlane(dir, relativeAltitude, hitXZ))
        continue;

        vec4 bounds = u_weatherBounds[i];
        float relMinX = (bounds.x - float(u_playerChunkX)) * chunkSizeBlocks;
        float relMinZ = (bounds.y - float(u_playerChunkZ)) * chunkSizeBlocks;
        float relMaxX = (bounds.z - float(u_playerChunkX)) * chunkSizeBlocks;
        float relMaxZ = (bounds.w - float(u_playerChunkZ)) * chunkSizeBlocks;

        vec2 center     = vec2((relMinX + relMaxX) * 0.5, (relMinZ + relMaxZ) * 0.5);
        vec2 halfExtent = vec2(max((relMaxX - relMinX) * 0.5, 0.01), max((relMaxZ - relMinZ) * 0.5, 0.01));

        vec4 cloudNoise = u_weatherCloudNoise[i];
        float thicknessSoftness = clamp(cloudShapeData.x / 64.0, 0.0, 1.0) * 0.15;
        float footprint = skyCloudFootprint(hitXZ, center, halfExtent, max(cloudNoise.w, thicknessSoftness));

        if (footprint <= 0.001)
        continue;

        vec4 cloudColorScale = u_weatherCloudColorScale[i];
        vec4 cloudMaterial   = u_weatherCloudMaterial[i];

        float shape = skyCloudShape(
            hitXZ, windDrift * cloudShapeData.w, cloudNoise,
            cloudColorScale.w, cloudMaterial.y, intensity);

        float coverage = footprint * shape * intensity * fadeAlpha;

        if (coverage <= 0.003)
        continue;

        float luminance = dot(cloudColorScale.rgb, vec3(0.299, 0.587, 0.114));
        vec3  tinted    = mix(vec3(luminance), cloudColorScale.rgb, cloudMaterial.x);
        tinted = mix(tinted, tinted * u_skyCloudColor, SKY_CLOUD_TINT_STRENGTH);

        float horizonT = clamp(patternState.x / max(u_weatherOuterRangeChunks, 0.001), 0.0, 1.0);
        tinted = mix(tinted, u_skyFogColor, horizonT * SKY_CLOUD_HORIZON_FADE);

        float remaining = 1.0 - accumulatedAlpha;
        accumulatedColor += tinted * coverage * remaining;
        accumulatedAlpha += coverage * remaining;
    }

    return vec4(accumulatedColor, clamp(accumulatedAlpha, 0.0, 1.0));
}

#endif