#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL

#include "includes/CameraData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"
#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/NoiseUtility.glsl"

// Distant/horizon cloud pass for the sky dome. Every weather-cloud entry in
// WeatherMapData is flat-plane-intersected against the view ray at that
// cloud's own altitude and shaded here only — the overhead volumetric box
// has its own unrelated raymarch and never shares sampling logic with this.

const float SKY_CLOUD_MIN_RAY_Y         = 0.002;
const float SKY_CLOUD_ALPHA_SATURATE    = 0.98;
const float SKY_CLOUD_HORIZON_FADE_FROM = 0.55;
const float SKY_CLOUD_AMBIENT_FLOOR     = 0.35;
const float SKY_CLOUD_SUN_LIFT          = 0.55;
const float SKY_CLOUD_MOON_LIFT         = 0.12;
const vec3  SKY_CLOUD_MOON_TINT         = vec3(0.58, 0.74, 1.00);
const float SKY_CLOUD_SUN_GLOW_POWER    = 10.0;
const float SKY_CLOUD_SUN_GLOW_STRENGTH = 0.6;

bool cloudPlaneHit(vec3 rayDir, vec3 rayOrigin, float planeAltitude, out vec2 hitXZ) {
    if (abs(rayDir.y) < SKY_CLOUD_MIN_RAY_Y)
    return false;

    float t = (planeAltitude - rayOrigin.y) / rayDir.y;

    if (t <= 0.0)
    return false;

    hitXZ = rayOrigin.xz + rayDir.xz * t;
    return true;
}

float cloudFootprintMask(vec2 hitXZ, vec2 boundsMin, vec2 boundsMax, float softness) {
    vec2 center = (boundsMin + boundsMax) * 0.5;
    vec2 halfExtent = max((boundsMax - boundsMin) * 0.5, vec2(1.0));
    vec2 norm = (hitXZ - center) / halfExtent;
    float edgeDist = 1.0 - max(abs(norm.x), abs(norm.y));
    return smoothstep(0.0, max(softness, 0.02), edgeDist);
}

float cloudCoverageAt(vec2 hitXZ, vec2 windDrift, float scale, vec4 noiseParams, float intensity) {
    vec3 evolved = vec3(hitXZ.x, 0.0, hitXZ.y) + vec3(windDrift.x, u_time * 0.4, windDrift.y);
    vec3 noisePos = evolved / max(scale, 1.0) * max(noiseParams.x, 0.01);

    vec3 warp = curlNoise3D(noisePos * 0.6) * noiseParams.y;
    float shape = perlinWorley3D(noisePos + warp, 3.0);

    float bias = clamp(noiseParams.z * mix(0.45, 1.0, intensity), 0.0, 0.95);
    return remapClamped(shape, 1.0 - bias, 1.0, 0.0, 1.0);
}

vec3 cloudLitColor(vec3 tint, float saturation, float sunGlow) {
    float luminance = dot(tint, vec3(0.299, 0.587, 0.114));
    vec3 desaturated = mix(vec3(luminance), tint, saturation);
    vec3 skyTinted = mix(desaturated, desaturated * u_skyCloudColor, 0.35);

    float dayFactor = clamp(u_sunIntensity, 0.0, 1.0);
    float ambient = mix(SKY_CLOUD_AMBIENT_FLOOR, 1.0, dayFactor);

    vec3 lit = skyTinted * ambient;
    lit += u_sunColor * u_sunIntensity * SKY_CLOUD_SUN_LIFT * sunGlow;
    lit += u_moonColor * SKY_CLOUD_MOON_TINT * min(u_moonIntensity, 0.2) * SKY_CLOUD_MOON_LIFT;

    return lit;
}

vec4 calculateClouds(vec3 dir) {
    vec3 accumulatedColor = vec3(0.0);
    float accumulatedAlpha = 0.0;

    vec3 rayOrigin = u_cameraPosition;
    vec2 windDrift = u_windDriftOffset;
    float chunkSizeBlocks = u_chunkSize;

    vec3 sunDir = normalize(u_sunDirection);
    float sunGlow = pow(clamp(dot(dir, -sunDir), 0.0, 1.0), SKY_CLOUD_SUN_GLOW_POWER) * SKY_CLOUD_SUN_GLOW_STRENGTH;

    int entryCount = min(u_weatherEntryCount, WEATHER_MAP_MAX_ENTRIES);

    for (int i = 0; i < entryCount; i++) {
        if (accumulatedAlpha >= SKY_CLOUD_ALPHA_SATURATE)
        break;

        vec4 patternState = u_weatherPatternState[i];
        float intensity = patternState.y;
        float fadeAlpha  = patternState.w;

        if (intensity <= 0.001 || fadeAlpha <= 0.001)
        continue;

        vec4 shape = u_weatherCloudShape[i];

        if (shape.z <= 0.001)
        continue;

        vec2 hitXZ;
        if (!cloudPlaneHit(dir, rayOrigin, shape.y, hitXZ))
        continue;

        vec4 bounds = u_weatherBounds[i];
        vec2 relMin = (bounds.xy - vec2(u_playerChunkX, u_playerChunkZ)) * chunkSizeBlocks;
        vec2 relMax = (bounds.zw - vec2(u_playerChunkX, u_playerChunkZ)) * chunkSizeBlocks;

        vec4 noise = u_weatherCloudNoise[i];
        float thicknessSoftness = clamp(shape.x / 64.0, 0.05, 1.0) * 0.2;
        float footprint = cloudFootprintMask(hitXZ, relMin, relMax, max(noise.w, thicknessSoftness));

        if (footprint <= 0.001)
        continue;

        vec4 colorScale = u_weatherCloudColorScale[i];
        vec4 material   = u_weatherCloudMaterial[i];

        float coverage = cloudCoverageAt(hitXZ, windDrift * shape.w, colorScale.w, noise, intensity);
        float shaped = pow(coverage, mix(1.6, 0.7, material.y));

        float alpha = footprint * shaped * intensity * fadeAlpha;

        if (alpha <= 0.003)
        continue;

        vec3 tinted = cloudLitColor(colorScale.rgb, material.x, sunGlow);

        float rangeT = clamp(patternState.x / max(u_weatherOuterRangeChunks, 0.001), 0.0, 1.0);
        float horizonT = clamp((rangeT - SKY_CLOUD_HORIZON_FADE_FROM) / max(1.0 - SKY_CLOUD_HORIZON_FADE_FROM, 0.001), 0.0, 1.0);
        tinted = mix(tinted, u_skyFogColor, horizonT * 0.5);

        float remaining = 1.0 - accumulatedAlpha;
        accumulatedColor += tinted * alpha * remaining;
        accumulatedAlpha += alpha * remaining;
    }

    return vec4(accumulatedColor, clamp(accumulatedAlpha, 0.0, 1.0));
}

#endif