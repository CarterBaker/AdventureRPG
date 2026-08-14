#ifndef SURFACE_TESSELLATION_TIER_GLSL
#define SURFACE_TESSELLATION_TIER_GLSL

#include "includes/SettingsData.glsl"

/*
* Shared tier-distance thresholds for StandardSurfaceShader's tcs/tes/fsh.
 * TIER1_MAX_SQ_DIST bounds the Mid ring and marks exactly where the Far
 * ring's distant terrain rise begins — fixed by design, never affected by
 * settings, so the world-bend and distant-rise tessellation stay exactly
 * as tuned. getTier0MaxSqDist() bounds the near ring — bevel and heightmap
 * tessellation — derived from u_nearTessellationRadius (SettingsData UBO,
 * user-configurable), replacing what used to be three separately-tuned
 * `const float TIER0_MAX_SQ_DIST = 2.5;` copies in the tcs/tes/fsh, which
 * only stayed correct as long as all three were hand-edited together.
 *
 * For an integer chunk offset (dx, dy) at Chebyshev radius r,
 * max(dx² + dy²) = 2r², and the next real ring begins strictly above that,
 * so 2r² + 0.5 lands exactly between them — the same identity
 * TIER1_MAX_SQ_DIST already relies on for r = 2. That identity only holds
 * through r = 2, so the result is clamped to TIER1_MAX_SQ_DIST: the near
 * ring can never grow past the Mid ring the Far ring's fixed boundary
 * depends on.
 */

const float TIER1_MAX_SQ_DIST = 8.5;

float getTier0MaxSqDist() {
    float r = max(u_nearTessellationRadius, 1.0);
    return min(2.0 * r * r + 0.5, TIER1_MAX_SQ_DIST);
}

#endif