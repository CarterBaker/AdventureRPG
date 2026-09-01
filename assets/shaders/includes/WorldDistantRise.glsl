#ifndef WORLD_DISTANT_RISE_GLSL
#define WORLD_DISTANT_RISE_GLSL

#include "includes/SettingsData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "surface/includes/SurfaceTessellationTier.glsl"

/*
 * Amplifies the horizon by lifting terrain upward once its true horizontal
 * distance from the player passes the far tessellation ring boundary,
 * measured against u_playerPosition for the same reason WorldCurvature.glsl
 * is — continuous through a chunk re-center and identical for any two
 * render batches sharing a boundary vertex. Callers gate the call on
 * u_distanceFromCenter > getTier1MaxSqDist() themselves, since that per-slot
 * value is already known at the call site and this function does no ring
 * check of its own.
 */

const float DISTANT_RISE_MAX_HEIGHT_BLOCKS = 64.0;
const float DISTANT_RISE_CURVE             = 0.35;

vec3 applyDistantRise(vec3 worldPos) {

    float halfDWorld       = (u_renderDistance * 0.5 - 0.5) * u_chunkSize;
    float maxDistanceWorld = halfDWorld * sqrt(2.0);
    float radialDist       = length(worldPos.xz - u_playerPosition.xz);

    float startDistanceWorld = max(maxDistanceWorld - DISTANT_RISE_START_MARGIN_BLOCKS, 0.0);
    float fadeRange          = max(maxDistanceWorld - startDistanceWorld, 0.0001);
    float farDistortT        = clamp((radialDist - startDistanceWorld) / fadeRange, 0.0, 1.0);

    float curveExponent      = pow(16.0, clamp(DISTANT_RISE_CURVE, 0.0, 1.0) * clamp(DISTANT_RISE_CURVE, 0.0, 1.0));
    float farDistortStrength = pow(farDistortT, curveExponent);

    worldPos.y += DISTANT_RISE_MAX_HEIGHT_BLOCKS * farDistortStrength;
    return worldPos;
}

#endif