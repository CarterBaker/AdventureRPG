// SkyCloudUtility.glsl — sky/util/SkyCloudUtility.glsl
#ifndef SKY_CLOUD_UTILITY_GLSL
#define SKY_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

// Shape and lighting primitives for the sky dome's distant weather raymarch
// (see Clouds.glsl). Every pattern is raymarched as a real world-space box —
// the same center/halfExtent the overhead volumetric system streams actual
// cloud objects within — never a blended approximation.

const float SKY_CLOUD_DETAIL_FREQUENCY = 5.0;
const float SKY_CLOUD_EROSION_STRENGTH = 0.55;
const float SKY_CLOUD_MIN_SOFTNESS     = 0.12;
const float SKY_CLOUD_AMBIENT_FLOOR    = 0.30;

vec2 intersectSkyCloudBox(vec3 rayOrigin, vec3 rayDir, vec3 boxMin, vec3 boxMax) {
    vec3 invDir = 1.0 / rayDir;
    vec3 t0 = (boxMin - rayOrigin) * invDir;
    vec3 t1 = (boxMax - rayOrigin) * invDir;
    vec3 tSmall = min(t0, t1);
    vec3 tBig = max(t0, t1);
    float tNear = max(max(tSmall.x, tSmall.y), tSmall.z);
    float tFar = min(min(tBig.x, tBig.y), tBig.z);
    return vec2(tNear, tFar);
}

float skyCloudHeightProfile(float heightT, float coverageBias) {
    float baseCutoff = mix(0.03, 0.16, clamp(coverageBias, 0.0, 1.0));
    float base = smoothstep(baseCutoff, baseCutoff + 0.30, heightT);
    float topStart = mix(0.40, 0.88, clamp(coverageBias, 0.0, 1.0));
    float top = 1.0 - smoothstep(topStart, 1.0, heightT);
    return base * top;
}

float skyCloudSilhouette(vec3 localPos, vec3 halfExtent, float softness) {
    vec3 n = localPos / max(halfExtent, vec3(0.0001));
    float radius = length(n.xz);
    float soft = clamp(softness, SKY_CLOUD_MIN_SOFTNESS, 0.8);
    return 1.0 - smoothstep(1.0 - soft, 1.0, radius);
}

float sampleSkyCloudDensity(
    vec3 worldPos,
    vec3 boxCenter,
    vec3 halfExtent,
    float heightT,
    float noiseScale,
    float warpStrength,
    float coverageBias,
    float silhouetteSoftness,
    float seed,
    float domainRotation,
    float timeSeconds) {
    vec3 local = worldPos - boxCenter;

    float envelope = skyCloudSilhouette(local, halfExtent, silhouetteSoftness)
    * skyCloudHeightProfile(heightT, coverageBias);

    if (envelope <= 0.002)
    return 0.0;

    float cosR = cos(domainRotation);
    float sinR = sin(domainRotation);
    vec2 rotated = vec2(local.x * cosR - local.z * sinR, local.x * sinR + local.z * cosR);

    vec3 seedOffset = vec3(seed * 191.7, seed * 63.13, seed * 107.31);
    vec3 coord = vec3(rotated.x, local.y, rotated.y) * (noiseScale / 180.0)
    + seedOffset + vec3(timeSeconds * 0.012, 0.0, timeSeconds * 0.004);

    float warpA = gradientNoise3D(coord.zyx * 0.5 + seedOffset);
    float warpB = gradientNoise3D(coord.yxz * 0.5 + seedOffset + 23.7);
    vec3 warped = coord + vec3(warpA, warpA * 0.35, warpB) * warpStrength;

    float macro = fbmGradient3D(warped, 4, 2.05, 0.5);
    float coverage = clamp(1.0 - coverageBias, 0.05, 0.95);
    float base = clamp(remapClamped(macro, coverage, 1.0, 0.0, 1.0), 0.0, 1.0) * envelope;

    if (base <= 0.01)
    return 0.0;
    if (base > 0.97)
    return base;

    float detail = worleyFbm3D(warped * SKY_CLOUD_DETAIL_FREQUENCY + seedOffset.yzx, 2, 2.2, 0.5);
    float erosionWeight = smoothstep(0.0, 0.65, 1.0 - base) * smoothstep(0.0, 0.35, base);
    float erosion = mix(1.0, detail, SKY_CLOUD_EROSION_STRENGTH * erosionWeight);

    return clamp(base * erosion, 0.0, 1.0);
}

float skyCloudToonBand(float t, float bands) {
    float scaled = clamp(t, 0.0, 1.0) * bands;
    float index = floor(scaled);
    float frac = scaled - index;
    float edge = smoothstep(0.3, 0.7, frac);
    return (index + edge) / max(bands - 1.0, 1.0);
}

vec3 shadeSkyCloud(
    vec3 baseColor,
    vec3 topColor,
    vec3 shadowColor,
    vec3 skyAmbient,
    vec3 lightColor,
    float lightPower,
    float heightT,
    float lightLift,
    float density,
    float toonBands,
    float shadeStrength,
    float rimLightStrength,
    float ambientOcclusionStrength,
    float brightnessMultiplier) {
    float litAmount = clamp(heightT * 0.5 + lightLift * 0.5, 0.0, 1.0);
    float banded = skyCloudToonBand(litAmount, max(toonBands, 2.0));

    vec3 directTint = mix(vec3(1.0), lightColor, 0.5) * clamp(lightPower, 0.0, 2.0);
    vec3 lit = mix(baseColor, topColor, banded) * mix(vec3(1.0), directTint, 0.6);

    vec3 shadowTint = mix(shadowColor, skyAmbient, 0.55);
    vec3 shaded = mix(lit, shadowTint, (1.0 - banded) * shadeStrength);

    float ao = mix(1.0 - ambientOcclusionStrength * 0.7, 1.0, banded);
    shaded *= ao;

    float ambientFloor = SKY_CLOUD_AMBIENT_FLOOR * (0.6 + 0.4 * heightT);
    shaded += skyAmbient * ambientFloor * (1.0 - density * 0.5);

    float silverLining = (1.0 - density) * lightLift * rimLightStrength;
    shaded += lightColor * clamp(lightPower, 0.0, 2.0) * silverLining;

    return shaded * brightnessMultiplier;
}

#endif