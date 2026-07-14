#ifndef VOLUMETRIC_CLOUD_UTILITY_GLSL
#define VOLUMETRIC_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Shape and shading for the overhead volumetric cloud objects. Every
 * instance is a soft ellipsoid of noise inscribed inside its own oriented
 * box so the silhouette is always rounded, and noise is sampled at a fixed
 * physical frequency in true world-space distance so a cloud's detail reads
 * as the same physical size regardless of instance scale or elongation.
 * Exclusive to the overhead cloud objects — never shared with the sky dome.
 */

const float CLOUD_NOISE_WORLD_SCALE = 1.0 / 26.0;
const float CLOUD_DETAIL_FREQUENCY  = 4.4;
const float CLOUD_EROSION_STRENGTH  = 0.55;
const float CLOUD_MIN_SOFTNESS      = 0.12;
const float CLOUD_BAND_SOFTNESS     = 0.75;

float remap(float value, float low1, float high1, float low2, float high2) {
    return low2 + (value - low1) * (high2 - low2) / max(high1 - low1, 0.0001);
}

vec2 worldToLocalXZ(vec2 relXZ, vec2 rot) {
    return vec2(
        relXZ.x * rot.x + relXZ.y * rot.y,
        -relXZ.x * rot.y + relXZ.y * rot.x);
}

vec2 intersectCloudOBB(vec3 rayOrigin, vec3 rayDir, vec3 boxCenter, vec2 rot, vec3 halfExtent) {
    vec3 rel = rayOrigin - boxCenter;
    vec2 localOriginXZ = worldToLocalXZ(rel.xz, rot);
    vec2 localDirXZ    = worldToLocalXZ(rayDir.xz, rot);

    vec3 localOrigin = vec3(localOriginXZ.x, rel.y, localOriginXZ.y);
    vec3 localDir    = vec3(localDirXZ.x, rayDir.y, localDirXZ.y);

    vec3 invDir = 1.0 / localDir;
    vec3 t0 = (-halfExtent - localOrigin) * invDir;
    vec3 t1 = (halfExtent - localOrigin) * invDir;
    vec3 tSmall = min(t0, t1);
    vec3 tBig   = max(t0, t1);
    float tNear = max(max(tSmall.x, tSmall.y), tSmall.z);
    float tFar  = min(min(tBig.x, tBig.y), tBig.z);
    return vec2(tNear, tFar);
}

float ellipsoidRadius(vec3 localPos, vec3 halfExtent) {
    vec3 n = localPos / max(halfExtent, vec3(0.0001));
    return length(n);
}

float heightShape(float heightT, float coverageBias) {
    float baseRamp = smoothstep(0.0, 0.22, heightT);
    float topStart = mix(0.45, 0.9, clamp(coverageBias, 0.0, 1.0));
    float topRamp = 1.0 - smoothstep(topStart, 1.0, heightT);
    return baseRamp * topRamp;
}

float sampleVolumetricCloudDensity(
    vec3 worldPos,
    vec3 boxCenter,
    vec2 rot,
    vec3 halfExtent,
    float heightT,
    float noiseScale,
    float warpStrength,
    float coverageBias,
    float silhouetteSoftness,
    float seed,
    float timeSeconds) {
    vec3 rel = worldPos - boxCenter;
    vec2 localXZ = worldToLocalXZ(rel.xz, rot);
    vec3 localPos = vec3(localXZ.x, rel.y, localXZ.y);

    float radius = ellipsoidRadius(localPos, halfExtent);

    if (radius >= 1.0)
    return 0.0;

    float softness = clamp(silhouetteSoftness, CLOUD_MIN_SOFTNESS, 0.7);
    float envelope = (1.0 - smoothstep(1.0 - softness, 1.0, radius)) * heightShape(heightT, coverageBias);

    if (envelope <= 0.002)
    return 0.0;

    float elongation = max(halfExtent.x / max(halfExtent.z, 0.0001), 1.0);
    vec3 seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 physicalPos = vec3(localXZ.x / elongation, rel.y, localXZ.y);
    vec3 coord = physicalPos * (noiseScale * CLOUD_NOISE_WORLD_SCALE)
    + seedOffset + vec3(0.0, 0.0, timeSeconds * 0.015);

    float warpA = gradientNoise3D(coord.yzx * 0.55 + seedOffset);
    float warpB = gradientNoise3D(coord.zxy * 0.55 + seedOffset + 19.1);
    vec3 warped = coord + vec3(warpA, warpA * 0.4, warpB) * warpStrength;

    float macro = fbmGradient3D(warped);
    float coverage = clamp(1.0 - coverageBias, 0.05, 0.95);
    float base = clamp(remap(macro, coverage, 1.0, 0.0, 1.0), 0.0, 1.0) * envelope;

    if (base <= 0.01)
    return 0.0;

    if (base > 0.98)
    return base;

    float bump = worleyFbm3D(warped * CLOUD_DETAIL_FREQUENCY + seedOffset.yzx);
    float erosionWeight = smoothstep(0.0, 0.7, 1.0 - base) * smoothstep(0.0, 0.3, base);
    float erosion = mix(1.0, bump, CLOUD_EROSION_STRENGTH * erosionWeight);

    return clamp(base * erosion, 0.0, 1.0);
}

vec3 volumetricCloudGradientNormal(
    vec3 p,
    vec3 boxCenter,
    vec2 rot,
    vec3 halfExtent,
    float noiseScale,
    float warpStrength,
    float coverageBias,
    float silhouetteSoftness,
    float seed,
    float timeSeconds,
    float epsilon) {
    float boxHeight = max(halfExtent.y * 2.0, 0.0001);
    float baseY = boxCenter.y - halfExtent.y;

    float heightTHere = clamp((p.y - baseY) / boxHeight, 0.0, 1.0);
    float heightTUp    = clamp(((p.y + epsilon) - baseY) / boxHeight, 0.0, 1.0);
    float heightTDown  = clamp(((p.y - epsilon) - baseY) / boxHeight, 0.0, 1.0);

    float dx = sampleVolumetricCloudDensity(p + vec3(epsilon, 0.0, 0.0), boxCenter, rot, halfExtent, heightTHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds)
    - sampleVolumetricCloudDensity(p - vec3(epsilon, 0.0, 0.0), boxCenter, rot, halfExtent, heightTHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds);

    float dy = sampleVolumetricCloudDensity(p + vec3(0.0, epsilon, 0.0), boxCenter, rot, halfExtent, heightTUp, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds)
    - sampleVolumetricCloudDensity(p - vec3(0.0, epsilon, 0.0), boxCenter, rot, halfExtent, heightTDown, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds);

    float dz = sampleVolumetricCloudDensity(p + vec3(0.0, 0.0, epsilon), boxCenter, rot, halfExtent, heightTHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds)
    - sampleVolumetricCloudDensity(p - vec3(0.0, 0.0, epsilon), boxCenter, rot, halfExtent, heightTHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds);

    vec3 gradient = vec3(dx, dy, dz);
    float gradLen = length(gradient);

    if (gradLen < 0.0001)
    return vec3(0.0, 1.0, 0.0);

    return -gradient / gradLen;
}

float softBand(float t, float bands) {
    float scaled = clamp(t, 0.0, 1.0) * bands;
    float index = floor(scaled);
    float frac = scaled - index;
    float edge = smoothstep(0.5 - CLOUD_BAND_SOFTNESS * 0.5, 0.5 + CLOUD_BAND_SOFTNESS * 0.5, frac);
    return (index + edge) / max(bands - 1.0, 1.0);
}

vec3 shadeCloudFluffy(
    vec3 baseColor,
    vec3 topColor,
    vec3 shadowColor,
    vec3 skyAmbient,
    float heightT,
    float lightLift,
    float density,
    float powder,
    int toonBands,
    float shadeStrength,
    float ambientOcclusionStrength,
    float brightnessMultiplier,
    out float outAO) {
    float litAmount = clamp(heightT * 0.55 + lightLift * 0.45, 0.0, 1.0);
    float banded = softBand(litAmount, max(float(toonBands), 2.0));

    vec3 lit = mix(baseColor, topColor, banded);
    vec3 shaded = mix(lit, shadowColor, (1.0 - banded) * shadeStrength);
    shaded = mix(shaded, skyAmbient, 0.2 + 0.18 * (1.0 - banded));

    float ao = mix(1.0 - ambientOcclusionStrength, 1.0, banded);
    shaded *= ao;

    shaded += skyAmbient * powder * 0.45;
    shaded = mix(mix(shadowColor, skyAmbient, 0.4), shaded, clamp(density * 2.2, 0.0, 1.0));

    outAO = ao;
    return shaded * brightnessMultiplier;
}

#endif