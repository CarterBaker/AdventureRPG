// AtmosphericFog.glsl
#ifndef ATMOSPHERIC_FOG_GLSL
#define ATMOSPHERIC_FOG_GLSL

#include "includes/CameraData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SettingsData.glsl"

/*
* Canonical fog distance curve, driven entirely by true per-fragment
 * world-space horizontal distance from the camera. Replaces the old
 * per-chunk u_distanceFromCenter approximation, which gave every
 * fragment of a chunk the exact same fogT — a hard, chunk-shaped tint
 * that seamed visibly at the near-tessellation-ring boundary and only
 * worsened with larger chunks. Every consumer, forward or deferred, now
 * calls the same function with the same two inputs (a world position and
 * the camera position), so there is nothing left to desync between
 * passes and no per-chunk value to smuggle through the G-buffer.
 *
 * Fog COLOR (u_skyFogColor) is still never derived here — it's computed
 * once per frame by the weather pipeline's SkyColorBranch and read
 * straight from SkyColorData.
 */

const float FOG_NEAR_CURVE_WEIGHT = 0.45;
const float FOG_FAR_CURVE_WEIGHT  = 0.35;
const float FOG_MAX_AMOUNT        = 0.80;

float computeFogAmount(vec3 worldPos) {
    float halfDWorld       = u_renderDistance * 0.5 - 0.5;
    float maxDistanceWorld = max(halfDWorld * u_chunkSize * sqrt(2.0), 0.0001);

    float linearDist = clamp(distance(worldPos.xz, u_cameraPosition.xz) / maxDistanceWorld, 0.0, 1.0);

    float rawFog = smoothstep(0.0, 0.5, linearDist) * FOG_NEAR_CURVE_WEIGHT
    + smoothstep(0.5, 1.0, linearDist) * FOG_FAR_CURVE_WEIGHT;

    return min(rawFog, FOG_MAX_AMOUNT);
}

vec3 applyAtmosphericFog(vec3 litColor, vec3 worldPos) {
    float fogT = computeFogAmount(worldPos);
    return mix(litColor, u_skyFogColor, fogT);
}

#endif