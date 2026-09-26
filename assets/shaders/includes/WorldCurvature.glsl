#ifndef WORLD_CURVATURE_GLSL
#define WORLD_CURVATURE_GLSL

#include "includes/PlayerPositionData.glsl"

/*
 * Shared "small planet" curvature — bends height down as the square of true
 * horizontal distance from the player, the gentle rolling horizon of a
 * world seen from its top, applied once to a position about to be projected
 * to clip space, never to gameplay-facing data (physics, collision, and any
 * UV/local-space math must all keep using the flat, uncurved position).
 * Distance is measured against u_playerPosition rather than the vertex's
 * raw frame-relative coordinate, since u_gridPosition only re-centers in
 * whole-chunk steps when the active chunk changes while u_playerPosition
 * tracks the player continuously inside that exact same frame — this is
 * what keeps the bend continuous through a chunk re-center and identical
 * for any two render batches (an individual chunk, a mega, a water surface,
 * a character) that share a boundary vertex. Every world-space renderer —
 * terrain, water, dropped items and characters — bends through this one
 * function, so nothing ever drifts off the ground it stands on at range.
 */

const float WORLD_CURVATURE_STRENGTH = 0.00016;

vec3 applyWorldCurvature(vec3 worldPos) {
    vec2 fromPlayer = worldPos.xz - u_playerPosition.xz;
    float distSq = dot(fromPlayer, fromPlayer);
    worldPos.y -= distSq * WORLD_CURVATURE_STRENGTH;
    return worldPos;
}

#endif