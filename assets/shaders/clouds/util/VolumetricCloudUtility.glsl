#ifndef VOLUMETRIC_CLOUD_UTILITY_GLSL
#define VOLUMETRIC_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Shape and shading for the overhead volumetric cloud objects
 * (CloudVolumeShader.fsh). Every instance is a soft ellipsoid of noise
 * inscribed inside its own oriented box, so the silhouette is always
 * rounded and can never show the box's flat faces or corners regardless
 * of raymarch step count. Noise is always sampled at a fixed physical
 * frequency in true world-space distance, never rescaled by the box's own
 * size, so a cloud's cellular detail reads as the same physical size no
 * matter how large, small, or elongated that instance is. Exclusive to
 * the overhead cloud objects — never shared with sky/util/SkyCloudUtility.glsl.
 */

const float CLOUD_NOISE_WORLD_SCALE  = 1.0 / 26.0;
const float CLOUD_DETAIL_FREQUENCY_A = 3.4;
const float CLOUD_DETAIL_FREQUENCY_B = 8.5;
const float CLOUD_EROSION_STRENGTH   = 0.6;
const float CLOUD_BAND_SOFTNESS      = 0.55;

float remap(float value, float low1, float high1, float low2, float high2) {
    return low2 + (value - low1) * (high2 - low2) / max(high1 - low1, 0.0001);
}

// World -> cloud-local rotation only (about Y), the exact inverse of the
// rotation CloudVolumeShader.vsh bakes into the mesh. relXZ.x/y hold the
// world X/Z components respectively.
vec2 worldToLocalXZ(vec2 relXZ, vec2 rot) {
    return vec2(
        relXZ.x * rot.x + relXZ.y * rot.y,
        -relXZ.x * rot.y + relXZ.y * rot.x);
}

// Ray-OBB intersection. halfExtent is the box's own local half size
// (x = long axis including elongation, y = vertical, z = short axis).
// Rotation preserves length, so the returned t is already a world-space
// distance along rayDir — no conversion needed by the caller.
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

// 0 at the box center, 1 exactly at the ellipsoid inscribed within the
// box. Puffs are only ever carved from noise inside this radius, so the
// silhouette is rounded by construction and never reaches the box's own
// corners — that's what makes the old hard box cutoff impossible here.
float ellipsoidRadius(vec3 localPos, vec3 halfExtent) {
    vec3 n = localPos / max(halfExtent, vec3(0.0001));
    return length(n);
}

// Anvil-style vertical mask: a soft ramp up from the flat base, a softer
// rounded taper into the top. coverageBias pushes the taper higher for a
// thicker, more overcast layer versus a shallow, puffy one.
float heightShape(float heightT, float coverageBias) {
    float baseRamp = smoothstep(0.0, 0.28, heightT);
    float topStart = mix(0.42, 0.88, clamp(coverageBias, 0.0, 1.0));
    float topRamp = 1.0 - smoothstep(topStart, 1.0, heightT);
    return baseRamp * topRamp;
}

/*
* Core density field. Elongation is undone only for the noise coordinate
 * (never for the ellipsoid envelope), so a stretched instance still reads
 * as round cauliflower cells rather than smeared ones. The macro shape
 * comes from a single remap() against a low-frequency FBM — a continuous,
 * gradient-following cutoff rather than a hard threshold, which is what
 * keeps the boundary looking like drifting mist instead of a faceted
 * polygon edge under a coarse raymarch step. Worley detail only blends in
 * near that boundary, where it actually changes the visible silhouette,
 * and is skipped entirely deep inside a fully opaque core.
 */
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

    float softness = clamp(silhouetteSoftness, 0.05, 0.6);
    float envelope = (1.0 - smoothstep(1.0 - softness, 1.0, radius)) * heightShape(heightT, coverageBias);

    if (envelope <= 0.002)
    return 0.0;

    float elongation = max(halfExtent.x / max(halfExtent.z, 0.0001), 1.0);
    vec3 seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 physicalPos = vec3(localXZ.x / elongation, rel.y, localXZ.y);
    vec3 coord = physicalPos * (noiseScale * CLOUD_NOISE_WORLD_SCALE)
    + seedOffset + vec3(0.0, 0.0, timeSeconds * 0.015);

    float warpA = gradientNoise3D(coord.yzx * 0.6 + seedOffset);
    float warpB = gradientNoise3D(coord.zxy * 0.6 + seedOffset + 11.3);
    vec3 warped = coord + vec3(warpA, warpA * 0.35, warpB) * warpStrength;

    float macro = fbmGradient3D(warped);
    float coverage = clamp(1.0 - coverageBias, 0.05, 0.95);
    float base = clamp(remap(macro, coverage, 1.0, 0.0, 1.0), 0.0, 1.0) * envelope;

    if (base <= 0.01)
    return 0.0;

    if (base > 0.985)
    return base;

    float bumpA = worleyFbm3D(warped * CLOUD_DETAIL_FREQUENCY_A + seedOffset.yzx);
    float bumpB = worleyFbm3D(warped * CLOUD_DETAIL_FREQUENCY_B + seedOffset.zxy + 5.1);
    float bump = clamp(bumpA * 0.65 + bumpB * 0.35, 0.0, 1.0);

    float erosionWeight = smoothstep(0.0, 0.65, 1.0 - base);
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

/*
* Shape-only shading for the deferred G-buffer writer. Bakes a soft toon
 * band, a self-shadow lift derived from the density gradient toward the
 * light, and a cheap ambient term pulled from the sky's own horizon/zenith
 * colors so a cloud's body reads as lit by the sky it actually sits in
 * rather than a flat, disconnected gray. Lighting.fsh applies the real
 * sun/moon/sky pass on top of this exactly once — never call this from a
 * forward pass.
 */
vec3 shadeCloudUnlit(
    vec3 baseColor,
    vec3 topColor,
    vec3 shadowColor,
    vec3 skyAmbient,
    float heightT,
    float lightLift,
    float density,
    int toonBands,
    float shadeStrength,
    float ambientOcclusionStrength,
    float brightnessMultiplier,
    out float outAO) {
    float litAmount = clamp(heightT * 0.6 + lightLift * 0.4, 0.0, 1.0);
    float banded = softBand(litAmount, max(float(toonBands), 2.0));

    vec3 lit = mix(baseColor, topColor, banded);
    vec3 shaded = mix(lit, shadowColor, (1.0 - banded) * shadeStrength);
    shaded = mix(shaded, shaded * 0.65 + skyAmbient * 0.35, 0.2);

    float ao = mix(1.0 - ambientOcclusionStrength, 1.0, banded);
    shaded *= ao;
    shaded = mix(shadowColor, shaded, clamp(density * 2.2, 0.0, 1.0));

    outAO = ao;
    return shaded * brightnessMultiplier;
}

#endif