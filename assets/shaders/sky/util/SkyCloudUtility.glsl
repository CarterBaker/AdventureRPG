#ifndef SKY_CLOUD_UTILITY_GLSL
#define SKY_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Density and shading primitives for the sky-dome cloud preview. Every ray
 * samples exactly one weather pattern's own real-world box (see Clouds.glsl),
 * so the shape carved here is always a genuine cloud, never an average of
 * unrelated patterns. Kept intentionally separate from the volumetric
 * overhead system — different budget, different math, no shared cloud logic.
 */

vec2 worldToLocalXZ(vec2 relXZ, vec2 rot) {
    return vec2(
        relXZ.x * rot.x + relXZ.y * rot.y,
        -relXZ.x * rot.y + relXZ.y * rot.x);
}

vec2 intersectSkyOBB(vec3 rayOrigin, vec3 rayDir, vec3 boxCenter, vec2 rot, vec3 halfExtent) {
    vec3 rel = rayOrigin - boxCenter;
    vec2 localOriginXZ = worldToLocalXZ(rel.xz, rot);
    vec2 localDirXZ = worldToLocalXZ(rayDir.xz, rot);

    vec3 localOrigin = vec3(localOriginXZ.x, rel.y, localOriginXZ.y);
    vec3 localDir = vec3(localDirXZ.x, rayDir.y, localDirXZ.y);

    vec3 invDir = 1.0 / localDir;
    vec3 t0 = (-halfExtent - localOrigin) * invDir;
    vec3 t1 = (halfExtent - localOrigin) * invDir;
    vec3 tSmall = min(t0, t1);
    vec3 tBig = max(t0, t1);
    float tNear = max(max(tSmall.x, tSmall.y), tSmall.z);
    float tFar = min(min(tBig.x, tBig.y), tBig.z);

    return vec2(tNear, tFar);
}

float skyHeightEnvelope(float heightT, float coverageBias) {
    float base = smoothstep(0.05, 0.25, heightT);
    float topStart = mix(0.5, 0.9, clamp(coverageBias, 0.0, 1.0));
    float top = 1.0 - smoothstep(topStart, 1.0, heightT);
    return base * top;
}

// localPos is already relative to the pattern's own rotated box — see
// worldToLocalXZ / intersectSkyOBB above. Cheaper than the volumetric
// density function by design: two noise octaves instead of four, since this
// runs across the full sky every frame rather than inside a few nearby boxes.
float sampleSkyCloudDensity(
    vec3 localPos,
    float heightT,
    float noiseScale,
    float warpStrength,
    float coverageBias,
    float silhouetteEdge,
    float seed,
    float timeSeconds) {
    vec3 seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 coord = localPos * noiseScale + seedOffset + vec3(0.0, 0.0, timeSeconds * 0.003);

    float warpA = gradientNoise3D(coord.yzx * 0.6 + seedOffset);
    float warpB = gradientNoise3D(coord.zxy * 0.6 + seedOffset + 11.3);
    vec3 warped = coord + vec3(warpA, warpA * 0.3, warpB) * warpStrength;

    float macro = fbmGradient3D(warped);
    float bump = worleyFbm3D(warped * 2.6 + seedOffset.yzx);
    float shape = clamp(mix(macro, bump, 0.3), 0.0, 1.0);

    float threshold = mix(0.72, 0.24, clamp(coverageBias, 0.0, 1.0));
    float edge = clamp(silhouetteEdge, 0.05, 0.4);
    float carved = smoothstep(threshold - edge, threshold + edge, shape);

    return carved * skyHeightEnvelope(heightT, coverageBias);
}

vec3 shadeSkyCloud(
    vec3 baseColor,
    vec3 topColor,
    vec3 shadowColor,
    vec3 lightColor,
    float lightIntensity,
    float heightT,
    float lightLift,
    int toonBands,
    float shadeStrength,
    float rimStrength,
    float ambientOcclusionStrength,
    float brightnessMultiplier) {
    float litAmount = clamp(heightT * 0.5 + lightLift * 0.5, 0.0, 1.0);
    float bands = max(float(toonBands), 1.0);
    float banded = floor(litAmount * bands) / max(bands - 1.0, 1.0);

    vec3 lit = mix(baseColor, topColor, banded);
    vec3 shaded = mix(lit, shadowColor, (1.0 - banded) * shadeStrength);
    shaded *= mix(1.0 - ambientOcclusionStrength, 1.0, banded);

    float rim = (1.0 - banded) * rimStrength;
    shaded += lightColor * lightIntensity * rim;
    shaded *= mix(vec3(1.0), lightColor, 0.3) * max(lightIntensity, 0.2);

    return shaded * brightnessMultiplier;
}

#endif