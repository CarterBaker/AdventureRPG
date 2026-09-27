// AtmosphericFog.glsl
#ifndef ATMOSPHERIC_FOG_GLSL
#define ATMOSPHERIC_FOG_GLSL

#include "includes/CameraData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/WorldCurvature.glsl"
#include "sky/util/SkyColor.glsl"

/*
 * Distance fog measured against the edge of the visible world. The shared
 * world curvature bends ground down by k·d² from the player, so ground h
 * blocks above sea level stays in view out to the eye's horizon reach plus
 * its own, sqrt((h - sea) / k); a fragment's horizon fraction is its true
 * horizontal distance over that limit. Fog rises along that fraction from
 * clear at the player to total where the fragment is about to sink below the
 * horizon, so every ridge fades by how close it stands to the world's edge
 * and low ground melts away sooner than the peaks rising behind it.
 *
 * Fog color follows the same fraction from the weather pipeline's fog tint
 * (u_skyFogColor) near the player to the exact sky color along the view
 * direction at the horizon, glow on the sun side, anti-solar belt opposite,
 * so the world's rim dissolves seamlessly into the sky above it and
 * horizons layer into the sky's own colors.
 *
 * Height is recovered from the curved position the depth buffer holds by
 * undoing the bend, and every distance is taken from the player, the same
 * origin the curvature uses. Reaches are measured above sea level, or above
 * the player where the player stands lower, in a dry basin or underwater, so
 * the horizon never collapses onto the eye.
 */

// Must match EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS and MACRO_HORIZON_EYE_MARGIN_BLOCKS.
const float FOG_SEA_LEVEL_BLOCKS  = 160.0;
const float FOG_EYE_MARGIN_BLOCKS = 8.0;

const float FOG_EPSILON           = 0.0001;
const float FOG_HORIZON_EXPONENT  = 1.5;
const float FOG_EDGE_START        = 0.75;

float resolveFogHorizonReach(float heightBlocks, float referenceBlocks) {
    return sqrt(max(heightBlocks - referenceBlocks, 0.0) / WORLD_CURVATURE_STRENGTH);
}

// Distance from the player over the distance at which this fragment sinks below the horizon, 0 to 1.
float computeHorizonFraction(vec3 curvedWorldPos) {
    vec2  fromPlayer = curvedWorldPos.xz - u_playerPosition.xz;
    float distSq     = dot(fromPlayer, fromPlayer);
    float flatHeight = curvedWorldPos.y + distSq * WORLD_CURVATURE_STRENGTH;
    float reference  = min(FOG_SEA_LEVEL_BLOCKS, u_playerPosition.y);

    float limit = resolveFogHorizonReach(u_cameraPosition.y + FOG_EYE_MARGIN_BLOCKS, reference)
    + resolveFogHorizonReach(flatHeight, reference);

    return clamp(sqrt(distSq) / max(limit, FOG_EPSILON), 0.0, 1.0);
}

float computeFogAmount(float horizonFraction) {
    return pow(horizonFraction, FOG_HORIZON_EXPONENT);
}

// Pulls an already light-weighted fog blend to total fog at the rim of the visible world.
float resolveFogEdge(float fogBlend, float horizonFraction) {
    return mix(clamp(fogBlend, 0.0, 1.0), 1.0, smoothstep(FOG_EDGE_START, 1.0, horizonFraction));
}

vec3 resolveFogColor(vec3 viewDir, float horizonFraction) {
    vec3 skyDir = normalize(vec3(viewDir.x, max(viewDir.y, 0.0), viewDir.z) + vec3(0.0, FOG_EPSILON, 0.0));
    vec3 sky    = resolveSkyColor(skyDir, normalize(u_sunDirection), 0.0);

    return mix(u_skyFogColor, sky, horizonFraction);
}

#endif
