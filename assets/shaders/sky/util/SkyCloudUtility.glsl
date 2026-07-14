#ifndef SKY_CLOUD_UTILITY_GLSL
#define SKY_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Sky-dome-only cloud primitives — world-scale density and shading for
 * Clouds.glsl's raymarch. No box-local normalization, so shape stays
 * correct regardless of how flat or wide the blended pattern's bounds are.
 * Never shared with clouds/util/VolumetricCloudUtility.glsl, which is
 * exclusive to the overhead volumetric cloud objects.
 */

// Bounds the raymarch's start/end distance along a view ray against a
// pattern's real world-space box. Never used to shape or orient density.
vec2 intersectSkyAABB(vec3 rayOrigin, vec3 rayDir, vec3 boxMin, vec3 boxMax) {
    vec3 invDir = 1.0 / rayDir;
    vec3 t0 = (boxMin - rayOrigin) * invDir;
    vec3 t1 = (boxMax - rayOrigin) * invDir;
    vec3 tSmall = min(t0, t1);
    vec3 tBig   = max(t0, t1);
    float tNear = max(max(tSmall.x, tSmall.y), tSmall.z);
    float tFar  = min(min(tBig.x, tBig.y), tBig.z);
    return vec2(tNear, tFar);
}

vec3 skyWarpDomain(vec3 p, float strength, vec3 seedOffset) {
    float wx = gradientNoise3D(p.yzx * 0.7 + seedOffset);
    float wz = gradientNoise3D(p.zxy * 0.7 + seedOffset + 11.3);
    return p + vec3(wx, 0.0, wz) * strength;
}

float skyHeightGradient(float heightT, float coverageBias) {
    float baseCutoff = 0.06;
    float baseRamp = smoothstep(baseCutoff, baseCutoff + 0.14, heightT);

    float topStart = mix(0.50, 0.88, clamp(coverageBias, 0.0, 1.0));
    float topRamp = 1.0 - smoothstep(topStart, 1.0, heightT);

    return baseRamp * topRamp;
}

float sampleSkyCloudDensity(
    vec3 worldPos,
    float heightT,
    float noiseScale,
    float warpStrength,
    float detailJitter,
    float coverageBias,
    float silhouetteSoftness,
    float seed,
    float timeSeconds) {
    float stretch = mix(1.0, 2.4, clamp(heightT, 0.0, 1.0));

    vec3 seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 stretchedPos = worldPos * vec3(1.0 / stretch, 1.0, 1.0 / stretch);
    vec3 coord = stretchedPos * noiseScale + seedOffset + vec3(0.0, 0.0, timeSeconds * 0.003);

    vec3 warped = skyWarpDomain(coord, warpStrength, seedOffset);

    float macro = fbmGradient3D(warped);
    float bump = worleyFbm3D(warped * 3.2 + seedOffset.yzx);
    float n = clamp(mix(macro, bump, 0.35) + (macro - 0.5) * detailJitter, 0.0, 1.0);

    float edgeSoftness = clamp(silhouetteSoftness * 1.5, 0.05, 0.35);
    float threshold = mix(0.74, 0.22, clamp(coverageBias, 0.0, 1.0));
    float shape = smoothstep(threshold - edgeSoftness, threshold + edgeSoftness, n);

    return shape * skyHeightGradient(heightT, coverageBias);
}

vec3 shadeSkyCloudLit(
    vec3 baseColor,
    vec3 topColor,
    vec3 shadowColor,
    vec3 lightColor,
    float lightIntensity,
    float heightT,
    float lightLift,
    float density,
    int toonBands,
    float shadeStrength,
    float rimLightStrength,
    float ambientOcclusionStrength,
    float brightnessMultiplier) {
    float litAmount = clamp(heightT * 0.5 + lightLift * 0.5, 0.0, 1.0);

    float bands = max(float(toonBands), 1.0);
    float banded = floor(litAmount * bands) / max(bands - 1.0, 1.0);

    vec3 lit = mix(baseColor, topColor, banded);
    vec3 shaded = mix(lit, shadowColor, (1.0 - banded) * shadeStrength);

    float ao = mix(1.0 - ambientOcclusionStrength, 1.0, banded);
    shaded *= ao;
    shaded = mix(shadowColor, shaded, clamp(density * 2.0, 0.0, 1.0));

    float rim = (1.0 - clamp(density * 2.0, 0.0, 1.0)) * rimLightStrength;
    shaded += lightColor * lightIntensity * rim;

    shaded *= mix(vec3(1.0), lightColor, 0.35) * max(lightIntensity, 0.15);

    return shaded * brightnessMultiplier;
}

#endif