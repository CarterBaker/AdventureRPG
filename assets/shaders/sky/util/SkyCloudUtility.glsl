#ifndef SKY_CLOUD_UTILITY_GLSL
#define SKY_CLOUD_UTILITY_GLSL

/*
* Shape, density, and lighting primitives for the sky dome's distant
 * weather clouds. Relies on SkyColorData's u_skyHorizonColor/u_skyZenithColor
 * already being declared earlier in the flattened shader source (see
 * Clouds.glsl's own include order, which always places SkyColorData.glsl
 * ahead of this file). The overhead volumetric system
 * (VolumetricCloudUtility.glsl) is a separate shader pipeline and never
 * shares code with this one, even though both raymarch an oriented box.
 */

const float SKY_SILHOUETTE_MIN_SOFTNESS = 0.12;
const float SKY_DENSITY_EDGE_MIN_SOFTNESS = 0.10;

vec3 skyHash3(vec3 p) {
    p = vec3(
        dot(p, vec3(127.1, 311.7, 74.7)),
        dot(p, vec3(269.5, 183.3, 246.1)),
        dot(p, vec3(113.5, 271.9, 124.6)));
    return fract(sin(p) * 43758.5453123) * 2.0 - 1.0;
}

float skyGradientNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);

    float n000 = dot(skyHash3(i + vec3(0.0, 0.0, 0.0)), f - vec3(0.0, 0.0, 0.0));
    float n100 = dot(skyHash3(i + vec3(1.0, 0.0, 0.0)), f - vec3(1.0, 0.0, 0.0));
    float n010 = dot(skyHash3(i + vec3(0.0, 1.0, 0.0)), f - vec3(0.0, 1.0, 0.0));
    float n110 = dot(skyHash3(i + vec3(1.0, 1.0, 0.0)), f - vec3(1.0, 1.0, 0.0));
    float n001 = dot(skyHash3(i + vec3(0.0, 0.0, 1.0)), f - vec3(0.0, 0.0, 1.0));
    float n101 = dot(skyHash3(i + vec3(1.0, 0.0, 1.0)), f - vec3(1.0, 0.0, 1.0));
    float n011 = dot(skyHash3(i + vec3(0.0, 1.0, 1.0)), f - vec3(0.0, 1.0, 1.0));
    float n111 = dot(skyHash3(i + vec3(1.0, 1.0, 1.0)), f - vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, u.x);
    float nx10 = mix(n010, n110, u.x);
    float nx01 = mix(n001, n101, u.x);
    float nx11 = mix(n011, n111, u.x);
    float nxy0 = mix(nx00, nx10, u.y);
    float nxy1 = mix(nx01, nx11, u.y);

    return mix(nxy0, nxy1, u.z);
}

float skyFbm(vec3 p) {
    float sum = 0.0;
    float amp = 0.5;
    vec3 pos = p;

    for (int i = 0; i < 4; i++) {
        sum += amp * skyGradientNoise(pos);
        pos = pos * 2.02 + vec3(17.0, -9.0, 5.0);
        amp *= 0.5;
    }

    return clamp(sum * 0.5 + 0.5, 0.0, 1.0);
}

float skyWorley(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 dir = sign(f - 0.5);

    float minDistSq = 1.0;

    for (int z = 0; z <= 1; z++)
    for (int y = 0; y <= 1; y++)
    for (int x = 0; x <= 1; x++) {
        vec3 neighbor = vec3(float(x), float(y), float(z)) * dir;
        vec3 point = skyHash3(i + neighbor) * 0.5 + 0.5;
        vec3 diff = neighbor + point - f;
        minDistSq = min(minDistSq, dot(diff, diff));
    }

    return 1.0 - clamp(sqrt(minDistSq), 0.0, 1.0);
}

vec2 intersectSkyCloudBox(vec3 rayOrigin, vec3 rayDir, vec3 boxCenter, vec2 rot, vec3 halfExtent) {
    vec3 rel = rayOrigin - boxCenter;
    vec2 localOriginXZ = vec2(rel.x * rot.x + rel.z * rot.y, -rel.x * rot.y + rel.z * rot.x);
    vec2 localDirXZ = vec2(rayDir.x * rot.x + rayDir.z * rot.y, -rayDir.x * rot.y + rayDir.z * rot.x);

    vec3 localOrigin = vec3(localOriginXZ.x, rel.y, localOriginXZ.y);
    vec3 localDir = vec3(localDirXZ.x, rayDir.y, localDirXZ.y);

    vec3 invDir = 1.0 / localDir;
    vec3 t0 = (-halfExtent - localOrigin) * invDir;
    vec3 t1 = (halfExtent - localOrigin) * invDir;
    vec3 tMin = min(t0, t1);
    vec3 tMax = max(t0, t1);

    return vec2(max(max(tMin.x, tMin.y), tMin.z), min(min(tMax.x, tMax.y), tMax.z));
}

float skyHeightProfile(float heightT, float coverageBias) {
    float baseCutoff = mix(0.02, 0.12, clamp(coverageBias, 0.0, 1.0));
    float baseRamp = smoothstep(baseCutoff, baseCutoff + 0.25, heightT);
    float topStart = mix(0.40, 0.90, clamp(coverageBias, 0.0, 1.0));
    float topRamp = 1.0 - smoothstep(topStart, 1.0, heightT);
    return baseRamp * topRamp;
}

float skySilhouette(vec3 localPos, vec3 halfExtent, float softness) {
    vec3 normalized = localPos / max(halfExtent, vec3(0.001));
    float radius = length(normalized);
    float edge = clamp(softness, SKY_SILHOUETTE_MIN_SOFTNESS, 0.9);
    return 1.0 - smoothstep(1.0 - edge, 1.0, radius);
}

float sampleSkyCloudDensity(
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
    vec2 localXZ = vec2(rel.x * rot.x + rel.z * rot.y, -rel.x * rot.y + rel.z * rot.x);
    vec3 localPos = vec3(localXZ.x, rel.y, localXZ.y);

    float silhouette = skySilhouette(localPos, halfExtent, silhouetteSoftness);
    float envelope = silhouette * skyHeightProfile(heightT, coverageBias);

    if (envelope <= 0.002)
    return 0.0;

    vec3 seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 drift = vec3(timeSeconds * 0.004, 0.0, timeSeconds * 0.0015);
    vec3 coord = worldPos * noiseScale + seedOffset + drift;

    float warpA = skyGradientNoise(coord.yzx * 0.6 + seedOffset);
    float warpB = skyGradientNoise(coord.zxy * 0.6 + seedOffset - 11.3);
    vec3 warped = coord + vec3(warpA, warpA * 0.25, warpB) * warpStrength;

    float macro = skyFbm(warped);
    float detail = skyWorley(warped * 3.0 + seedOffset.yzx);
    float shape = clamp(mix(macro, detail, 0.3), 0.0, 1.0);

    float threshold = mix(0.30, 0.62, clamp(coverageBias, 0.0, 1.0));
    float edgeSoft = clamp(silhouetteSoftness * 1.3, SKY_DENSITY_EDGE_MIN_SOFTNESS, 0.4);
    float density = smoothstep(threshold - edgeSoft, threshold + edgeSoft, shape);

    return density * envelope;
}

/*
* Real (non-toon) lighting for one raymarch sample. Ambient is the sky's own
 * current horizon/zenith color, blended toward zenith as heightT rises — a
 * cloud's underside reads like it's catching horizon-tinted bounce light,
 * its top like open sky, matching the overhead volumetric system's own
 * sky/ground tint technique. Direct is the real sun/moon color and
 * intensity, modulated by lightLift — the caller's own density-gradient
 * self-shadow term — so a cloud's lit side is genuinely brighter than its
 * shadowed side without any material-level lighting knobs.
 */
vec3 shadeSkyCloudLit(
    vec3 baseColor,
    vec3 lightColor,
    float lightPower,
    float heightT,
    float lightLift) {
    vec3 ambientTint = mix(u_skyHorizonColor, u_skyZenithColor, heightT);
    vec3 ambient = ambientTint * mix(0.5, 1.0, heightT);
    vec3 direct = lightColor * lightPower * lightLift;

    return baseColor * (ambient + direct);
}

#endif