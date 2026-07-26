// VolumetricCloudUtility.glsl — clouds/util/VolumetricCloudUtility.glsl
#ifndef VOLUMETRIC_CLOUD_UTILITY_GLSL
#define VOLUMETRIC_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Shape, density, and in-scatter lighting primitives for the overhead
 * volumetric cloud boxes. Each instance raymarches its own oriented box.
 * The macro silhouette comes from a low-frequency gradient fbm; two layered
 * Worley octaves erode it into cauliflower lobes. sampleCloudDensityApprox
 * is a cheaper stand-in used only by the light-transmittance self-shadow
 * taps, which don't need that surface detail.
 */

const float CLOUD_NOISE_WORLD_SCALE  = 1.0 / 26.0;
const float CLOUD_DETAIL_FREQUENCY   = 2.6;
const float CLOUD_EROSION_STRENGTH   = 0.5;
const float CLOUD_MIN_SOFTNESS       = 0.14;
const float CLOUD_BASE_DENSITY_BOOST = 0.25;

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

float cloudHeightEnvelope(float heightT, float coverageBias) {
    float base = smoothstep(0.0, 0.10, heightT);
    float topStart = mix(0.45, 0.90, clamp(coverageBias, 0.0, 1.0));
    float top = 1.0 - smoothstep(topStart, 1.0, heightT);
    float baseWeight = 1.0 + CLOUD_BASE_DENSITY_BOOST * (1.0 - smoothstep(0.0, 0.35, heightT));
    return base * top * baseWeight;
}

// Density \\

/*
* Full-detail density: macro fbm shape, domain warp, then two layered
 * Worley erosion passes. elongation, seedOffset, and timeDrift are resolved
 * once per instance in CloudVolumeShader.vsh rather than recomputed here on
 * every one of the many samples taken per fragment.
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
    float elongation,
    vec3 seedOffset,
    vec3 timeDrift) {
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

    vec3 physicalPos = vec3(localXZ.x / elongation, rel.y, localXZ.y);
    vec3 coord = physicalPos * (noiseScale * CLOUD_NOISE_WORLD_SCALE) + seedOffset + timeDrift;

    float warpA = gradientNoise3D(coord.yzx * 0.5 + seedOffset);
    float warpB = gradientNoise3D(coord.zxy * 0.5 + seedOffset - 11.3);
    vec3 warped = coord + vec3(warpA, warpA * 0.25, warpB) * warpStrength;

    float macro = fbmGradient3D(warped, 4, 2.05, 0.52);
    float coverage = clamp(1.0 - coverageBias, 0.10, 0.90);
    float coreBand = mix(0.30, 0.12, clamp(detailFactor, 0.0, 1.0));
    float core = smoothstep(coverage - coreBand * 0.5, coverage + coreBand, macro);

    float base = core * envelope;

    if (base <= 0.01)
    return 0.0;

    float billowCoarse = 1.0 - worleyFbm3D(warped * 1.1 + seedOffset.zxy, 2, 2.1, 0.55);
    float billowed = clamp(mix(base, base * 0.5 + billowCoarse * 0.7, 0.45 * envelope), 0.0, 1.0);

    float detail = worleyFbm3D(warped * CLOUD_DETAIL_FREQUENCY + seedOffset.yzx, 3, 2.3, 0.5);
    float edgeWeight = smoothstep(0.0, 0.35, 1.0 - billowed) * smoothstep(0.0, 0.28, billowed);
    float erosionStrength = CLOUD_EROSION_STRENGTH * mix(0.25, 1.0, clamp(detailFactor, 0.0, 1.0));
    float eroded = mix(billowed, billowed * mix(0.4, 1.0, detail), erosionStrength * edgeWeight);

    return clamp(eroded, 0.0, 1.0);
}

/*
* Cheap stand-in for sampleCloudDensity used only by the light-transmittance
 * self-shadow taps below — no domain warp, one fewer macro octave, and
 * neither Worley erosion pass. A shadow estimate only needs to know where
 * the mass is thick or thin, not the fine surface detail the visible
 * silhouette resolves.
 */
float sampleCloudDensityApprox(
    vec3 worldPos,
    vec3 boxCenter,
    vec2 rot,
    vec3 halfExtent,
    float heightT,
    float noiseScale,
    float coverageBias,
    float silhouetteSoftness,
    float elongation,
    vec3 seedOffset,
    vec3 timeDrift) {
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

    vec3 physicalPos = vec3(localXZ.x / elongation, rel.y, localXZ.y);
    vec3 coord = physicalPos * (noiseScale * CLOUD_NOISE_WORLD_SCALE) + seedOffset + timeDrift;

    float macro = fbmGradient3D(coord, 3, 2.05, 0.52);
    float coverage = clamp(1.0 - coverageBias, 0.10, 0.90);
    float core = smoothstep(coverage - 0.18, coverage + 0.18, macro);

    return core * envelope;
}

// Lighting \\

float henyeyGreenstein(float cosAngle, float g) {
    float g2 = g * g;
    return (1.0 - g2) / (4.0 * 3.14159265 * pow(max(1.0 + g2 - 2.0 * g * cosAngle, 0.0001), 1.5));
}

float cloudPhase(float cosAngle, float forwardG, float backG, float blend) {
    float forward = henyeyGreenstein(cosAngle, forwardG);
    float back = henyeyGreenstein(cosAngle, backG);
    return mix(back, forward, blend);
}

/*
* Multi-tap in-scattering toward the light, using the cheap approximate
 * density above rather than a full shadow raymarch.
 */
float sampleCloudLightTransmittance(
    vec3 p,
    vec3 lightDir,
    vec3 boxCenter,
    vec2 rot,
    vec3 halfExtent,
    float noiseScale,
    float coverageBias,
    float silhouetteSoftness,
    float elongation,
    vec3 seedOffset,
    vec3 timeDrift,
    float stepDistance,
    int tapCount,
    float extinction) {
    float boxHeight = max(halfExtent.y * 2.0, 0.0001);
    float baseY = boxCenter.y - halfExtent.y;

    float opticalDepth = 0.0;
    vec3 samplePos = p;

    for (int i = 0; i < tapCount; i++) {
        samplePos += lightDir * stepDistance;
        float heightT = clamp((samplePos.y - baseY) / boxHeight, 0.0, 1.0);
        opticalDepth += sampleCloudDensityApprox(
            samplePos, boxCenter, rot, halfExtent, heightT,
            noiseScale, coverageBias, silhouetteSoftness,
            elongation, seedOffset, timeDrift);
    }

    return exp(-opticalDepth * extinction * stepDistance);
}

#endif