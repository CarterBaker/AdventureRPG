#ifndef SURFACE_TESSELLATION_TIER_GLSL
#define SURFACE_TESSELLATION_TIER_GLSL

#include "includes/SettingsData.glsl"
#include "includes/PlayerPositionData.glsl"

/*
* Shared tier-distance thresholds for StandardSurfaceShader's tcs/tes/fsh
 * (and WaterShader's own distant-rise gate), both expressed in squared
 * chunk-grid units.
 *
 * getTier0MaxSqDist() bounds the near ring (bevel, heightmap, near-terrain
 * noise) and is driven entirely by u_nearTessellationRadius, the
 * user-facing detail setting: radius r gives every chunk within Chebyshev
 * distance r full tessellation detail, the same way a grass-draw-distance
 * slider works. A chunk offset (dx, dz) at Chebyshev radius r has
 * dx² + dz² up to 2r² (corner case), so 2r² + 0.5 is the smallest
 * threshold that includes every chunk through radius r.
 *
 * getTier1MaxSqDist() bounds the Mid ring and marks where the Far ring's
 * block-exact tessellation and distant terrain rise begin (see
 * StandardSurface.tes and WorldDistantRise.glsl). Driven entirely by
 * u_renderDistance and u_chunkSize — never by u_nearTessellationRadius —
 * with DISTANT_RISE_START_MARGIN_BLOCKS held back from the render-distance
 * edge, so the Far ring's dense geometry is always established before the
 * rise can ever be nonzero, at any render distance.
 *
 * getTier0MaxSqDist() is clamped to getTier1MaxSqDist() so the near ring
 * can never be dialed past the Far ring it feeds into.
 *
 * computeDistanceFromCenterSq() replaces comparing against a single
 * squared distance baked once per grid slot. That old value was fixed at
 * grid-build time per chunk-grid slot and shared unchanged by every chunk
 * merged into the same mega batch, so a chunk rendered individually and
 * that same chunk's neighbor rendered as part of a mega could disagree
 * about which ring they were in — and the player's own position inside
 * their chunk never factored into the decision at all. Every caller here
 * already has a fully resolved world position (aPos plus that draw call's
 * own grid-slot offset, whether that offset came from an individual
 * chunk's own slot or from a mega's origin slot plus the chunk's own
 * baked CPU-side merge offset) by the time it needs a tier decision, so
 * measuring true distance from the player's own continuous position
 * directly off that world position — instead of trusting a stale,
 * batch-shared UBO value — makes ring boundaries agree everywhere the
 * same physical location is ever drawn from, and keeps them sliding
 * smoothly as the player moves rather than snapping per grid slot or per
 * mega.
 */

const float DISTANT_RISE_START_MARGIN_BLOCKS = 512.0;

float getTier1MaxSqDist() {
    float halfD        = u_renderDistance * 0.5 - 0.5;
    float marginChunks = DISTANT_RISE_START_MARGIN_BLOCKS / (u_chunkSize * sqrt(2.0));
    float farHalfD      = max(halfD - marginChunks, 1.0);
    return farHalfD * farHalfD * 2.0;
}

float getTier0MaxSqDist() {
    float r = max(u_nearTessellationRadius, 1.0);
    return min(2.0 * r * r + 0.5, getTier1MaxSqDist());
}

float computeDistanceFromCenterSq(vec3 worldPos) {
    vec2 fromPlayerChunks = (worldPos.xz - u_playerPosition.xz) / u_chunkSize;
    return dot(fromPlayerChunks, fromPlayerChunks);
}

#endif