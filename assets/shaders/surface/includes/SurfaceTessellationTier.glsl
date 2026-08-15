#ifndef SURFACE_TESSELLATION_TIER_GLSL
#define SURFACE_TESSELLATION_TIER_GLSL

#include "includes/SettingsData.glsl"

/*
* Shared tier-distance thresholds for StandardSurfaceShader's tcs/tes/fsh,
 * both expressed in u_distanceFromCenter's own units — squared chunk-grid
 * distance from the center grid slot (see GridCoordinateData.glsl).
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
 * StandardSurface.tes). Driven entirely by u_renderDistance and
 * u_chunkSize — never by u_nearTessellationRadius — with
 * DISTANT_RISE_START_MARGIN_BLOCKS held back from the render-distance
 * edge. That margin is the same one StandardSurface.tes subtracts before
 * fading the rise in, worked back into chunk-grid units, so the Far
 * ring's dense geometry is always established before the rise can ever
 * be nonzero, at any render distance, and never shifts in response to
 * the near-tessellation-radius setting.
 *
 * getTier0MaxSqDist() is clamped to getTier1MaxSqDist() so the near ring
 * can never be dialed past the Far ring it feeds into.
 */

// Must match DISTORT_START_MARGIN in StandardSurface.tes.
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

#endif