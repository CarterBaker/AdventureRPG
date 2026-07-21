// VolumetricCloudUtility.glsl — clouds/util/VolumetricCloudUtility.glsl
#ifndef VOLUMETRIC_CLOUD_UTILITY_GLSL
#define VOLUMETRIC_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

// Shape and density primitives for the overhead volumetric cloud boxes.
// Each instance raymarches its own oriented box as an inscribed ellipsoid.
// The density field is built to stay mostly solid through the core, with
// noise confined to a thin boundary shell — a wide translucent mid-density
// haze is exactly what turns a per-pixel dithered alpha test into a field
// of flickering dots instead of a puffy, mostly-opaque cloud body.

const float CLOUD_NOISE_WORLD_SCALE = 1.0 / 26.0;
const float CLOUD_DETAIL_FREQUENCY  = 2.6;
const float CLOUD_EROSION_STRENGTH  = 0.55;
const float CLOUD_MIN_SOFTNESS      = 0.12;

// Rotation \\

vec2 rotateWorldToLocal(vec2 relXZ, vec2 rot) {
    return vec2(relXZ.x * rot.x + relXZ.y * rot.y, -relXZ.x * rot.y + relXZ.y * rot.x);
}

// Intersection \\

vec2 intersectCloudBox(vec3 rayOrigin, vec3 rayDir, vec3 boxCenter, vec2 rot, vec3 halfExtent) {
    vec3 rel = rayOrigin - boxCenter;
    vec2 localOriginXZ = rotateWorldToLocal(rel.xz, rot);
    vec2 localDirXZ = rotateWorldToLocal(rayDir.xz, rot);

    vec3 localOrigin = vec3(localOriginXZ.x, rel.y, localOriginXZ.y);
    vec3 localDir = vec3(localDirXZ.x, rayDir.y, localDirXZ.y);

    vec3 invDir = 1.0 / localDir;
    vec3 t0 = (-halfExtent - localOrigin) * invDir;
    vec3 t1 = (halfExtent - localOrigin) * invDir;
    vec3 tMin = min(t0, t1);
    vec3 tMax = max(t0, t1);

    return vec2(max(max(tMin.x, tMin.y), tMin.z), min(min(tMax.x, tMax.y), tMax.z));
}

// Shape \\

// Flat, dense base with a rounded taper at the top — a cumulus profile
// rather than a symmetric blob. The wide plateau keeps most of the box's
// vertical extent at full envelope instead of fading across the whole height.
float cloudHeightEnvelope(float heightT, float coverageBias) {
    float base = smoothstep(0.0, 0.10, heightT);
    float topStart = mix(0.45, 0.90, clamp(coverageBias, 0.0, 1.0));
    float top = 1.0 - smoothstep(topStart, 1.0, heightT);
    return base * top;
}

// Density \\

/*
* worldPos/boxCenter/rot/halfExtent describe the instance's own oriented
 * box. heightT is 0 at the box floor and 1 at the box ceiling. Returns 0
 * outside the ellipsoid inscribed in that box. detailFactor fades the
 * highest-frequency erosion detail out with distance so a raymarch step
 * that now covers many world units per pixel never aliases into visible
 * shimmer — the silhouette itself never changes, only how much cauliflower
 * texture rides on top of it.
 *
 * The macro shape is pushed through a narrow smoothstep rather than a wide
 * linear remap, so most of the interior sits solidly near 1.0 and only a
 * thin band around the silhouette carries partial density. A secondary
 * inverted-Worley "billow" layer rides on top of that core to read as
 * rounded lobes rather than a flat blob, and the finest erosion detail is
 * confined to that same thin edge band so it carves the boundary instead
 * of punching holes through the body.
 */
float sampleCloudDensity(
    vec3 worldPos,
    vec3 boxCenter,
    vec2 rot,
    vec3 halfExtent,
    float heightT,
    float noiseScale,
    float warpStrength,
    float coverageBias,
    float silhouetteSoftness,
    float detailFactor,
    float seed,
    float timeSeconds) {
    vec3 rel = worldPos - boxCenter;
    vec2 localXZ = rotateWorldToLocal(rel.xz, rot);
    vec3 localPos = vec3(localXZ.x, rel.y, localXZ.y);

    vec3 normalizedPos = localPos / max(halfExtent, vec3(0.0001));
    float radius = length(normalizedPos);

    if (radius >= 1.0)
    return 0.0;

    float softness = clamp(silhouetteSoftness, CLOUD_MIN_SOFTNESS, 0.85);
    float silhouette = 1.0 - smoothstep(1.0 - softness, 1.0, radius);
    float envelope = silhouette * cloudHeightEnvelope(heightT, coverageBias);

    if (envelope <= 0.002)
    return 0.0;

    float elongation = max(halfExtent.x / max(halfExtent.z, 0.0001), 1.0);
    vec3 seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 drift = vec3(timeSeconds * 0.006, 0.0, timeSeconds * 0.004);
    vec3 physicalPos = vec3(localXZ.x / elongation, rel.y, localXZ.y);
    vec3 coord = physicalPos * (noiseScale * CLOUD_NOISE_WORLD_SCALE) + seedOffset + drift;

    // Low-frequency warp only — keeps macro lobes coherent instead of
    // shredding them with high-frequency displacement.
    float warpA = gradientNoise3D(coord.yzx * 0.5 + seedOffset);
    float warpB = gradientNoise3D(coord.zxy * 0.5 + seedOffset - 11.3);
    vec3 warped = coord + vec3(warpA, warpA * 0.25, warpB) * warpStrength;

    float macro = fbmGradient3D(warped, 4, 2.05, 0.52);
    float coverage = clamp(1.0 - coverageBias, 0.08, 0.92);
    float coreBand = mix(0.22, 0.08, clamp(detailFactor, 0.0, 1.0));
    float core = smoothstep(coverage - coreBand * 0.5, coverage + coreBand, macro);

    float base = core * envelope;

    if (base <= 0.015)
    return 0.0;

    float billow = 1.0 - worleyFbm3D(warped * 1.6 + seedOffset.zxy, 2, 2.1, 0.55);
    float billowed = clamp(mix(base, base * 0.55 + billow * 0.65, 0.35 * envelope), 0.0, 1.0);

    float detail = worleyFbm3D(warped * CLOUD_DETAIL_FREQUENCY + seedOffset.yzx, 2, 2.3, 0.5);
    float edgeWeight = smoothstep(0.0, 0.30, 1.0 - billowed) * smoothstep(0.0, 0.22, billowed);
    float erosionStrength = CLOUD_EROSION_STRENGTH * mix(0.2, 1.0, clamp(detailFactor, 0.0, 1.0));
    float eroded = mix(billowed, billowed * detail, erosionStrength * edgeWeight);

    return clamp(eroded, 0.0, 1.0);
}

// Normal \\

vec3 sampleCloudGradientNormal(
    vec3 p,
    vec3 boxCenter,
    vec2 rot,
    vec3 halfExtent,
    float noiseScale,
    float warpStrength,
    float coverageBias,
    float silhouetteSoftness,
    float detailFactor,
    float seed,
    float timeSeconds,
    float epsilon) {
    float boxHeight = max(halfExtent.y * 2.0, 0.0001);
    float baseY = boxCenter.y - halfExtent.y;

    float heightHere = clamp((p.y - baseY) / boxHeight, 0.0, 1.0);
    float heightUp    = clamp(((p.y + epsilon) - baseY) / boxHeight, 0.0, 1.0);
    float heightDown  = clamp(((p.y - epsilon) - baseY) / boxHeight, 0.0, 1.0);

    float dx = sampleCloudDensity(p + vec3(epsilon, 0.0, 0.0), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds)
    - sampleCloudDensity(p - vec3(epsilon, 0.0, 0.0), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds);

    float dy = sampleCloudDensity(p + vec3(0.0, epsilon, 0.0), boxCenter, rot, halfExtent, heightUp, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds)
    - sampleCloudDensity(p - vec3(0.0, epsilon, 0.0), boxCenter, rot, halfExtent, heightDown, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds);

    float dz = sampleCloudDensity(p + vec3(0.0, 0.0, epsilon), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds)
    - sampleCloudDensity(p - vec3(0.0, 0.0, epsilon), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds);

    vec3 gradient = vec3(dx, dy, dz);
    float gradientLength = length(gradient);

    if (gradientLength < 0.0001)
    return vec3(0.0, 1.0, 0.0);

    return -gradient / gradientLength;
}

// Self-Shadow \\

/*
* Cheap directional occlusion proxy: compares density here against density
 * a short hop toward the light. If density falls off moving toward the
 * light, this point sits on the near/lit side of the body and light reaches
 * it fairly directly; if density climbs, this point is behind more of the
 * cloud's own mass as seen from the light and reads as shadowed. One extra
 * density sample, evaluated once per pixel at the raymarch's peak-
 * contribution position rather than per step, so it stays cheap.
 */
float sampleCloudLightLift(
    vec3 p,
    vec3 boxCenter,
    vec2 rot,
    vec3 halfExtent,
    float noiseScale,
    float warpStrength,
    float coverageBias,
    float silhouetteSoftness,
    float detailFactor,
    float seed,
    float timeSeconds,
    vec3 lightDir,
    float tapDistance,
    float densityHere) {
    float boxHeight = max(halfExtent.y * 2.0, 0.0001);
    float baseY = boxCenter.y - halfExtent.y;

    vec3 litPos = p + lightDir * tapDistance;
    float heightLit = clamp((litPos.y - baseY) / boxHeight, 0.0, 1.0);

    float densityLit = sampleCloudDensity(
        litPos, boxCenter, rot, halfExtent, heightLit,
        noiseScale, warpStrength, coverageBias, silhouetteSoftness,
        detailFactor, seed, timeSeconds);

    return clamp((densityHere - densityLit) * 2.0 + 0.5, 0.0, 1.0);
}

#endif