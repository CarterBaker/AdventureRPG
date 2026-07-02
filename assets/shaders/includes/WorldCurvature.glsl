#ifndef WORLD_CURVATURE_GLSL
#define WORLD_CURVATURE_GLSL

/*
* Shared "small planet" curvature — Animal Crossing style horizon bend.
 * Height drops off as the SQUARE of horizontal distance from world
 * center. Lives in includes/ (not surface/includes or
 * postprocessing/includes) because every vertex shader that places
 * something in world space — terrain, entities, props, anything — is
 * meant to call this, not just StandardSurface.
 *
 * PURE RENDERING-TIME OFFSET. Apply this only to the copy of a position
 * that's about to be projected to clip space (gl_Position). Never feed
 * the result back into gameplay-facing data — physics, collision, AI,
 * and any UV/local-space math must all keep using the flat, uncurved
 * position. That split is what stops entities from floating above (or
 * sinking into) curved terrain: an entity's true simulated position
 * stays flat, but its RENDERED vertex gets pushed down by the exact same
 * formula the ground under it uses, so the two curves match and stay
 * visually glued together.
 *
 * Deliberately a plain quadratic of raw world-space distance, not
 * normalized against render distance the way AtmosphericFog's curve is —
 * the bend should get MORE pronounced as render distance is increased in
 * settings, not stay fixed. No UBO/uniform dependency on purpose, so any
 * vertex shader can include this with zero setup.
 *
 * Curvature is smooth everywhere (constant second derivative), unlike
 * noise/height/bevel displacement — so unlike those, it does NOT need
 * extra tessellation density to look right on large greedy-merged quads.
 * See the call site in StandardSurface.tes / StandardSurface.tcs for the
 * sag-vs-quad-size math.
 */

// Block-height drop per (block distance)^2. Keep this small — the goal is
// "the world feels big and round", not a visible dome. For reference:
// distance 100 blocks -> drops 0.8 blocks; distance 300 blocks -> drops
// ~7.2 blocks. Retune against your actual render distance.
const float WORLD_CURVATURE_STRENGTH = 0.00016;

vec3 applyWorldCurvature(vec3 worldPos) {
    float distSq = worldPos.x * worldPos.x + worldPos.z * worldPos.z;
    worldPos.y  -= distSq * WORLD_CURVATURE_STRENGTH;
    return worldPos;
}

#endif