#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in float aNorIndex;
layout (location = 2) in float aColor;
layout (location = 3) in vec2  aUVOrigin;
layout (location = 4) in float aOrient;
layout (location = 5) in float aQuadSize;
layout (location = 6) in float aBevelMaskA0;  // A=0 edge exposure,    bit j = cell j along B
layout (location = 7) in float aBevelMaskA1;  // A=sizeA edge exposure, bit j = cell j along B
layout (location = 8) in float aBevelMaskB0;  // B=0 edge exposure,    bit i = cell i along A
layout (location = 9) in float aBevelMaskB1;  // B=sizeB edge exposure, bit i = cell i along A

#include "includes/GridCoordinateData.glsl"
#include "includes/SettingsData.glsl"

const vec3 NORMALS[6] = vec3[](
    vec3(0, 0, 1),
    vec3(1, 0, 0),
    vec3(0, 0,-1),
    vec3(-1, 0, 0),
    vec3(0, 1, 0),
    vec3(0,-1, 0));

out vec3  tcWorldPos;
out vec3  tcNormal;
out vec2  tcUVOrigin;
out float tcOrient;
out float tcColor;
out float tcQuadSize;
out float tcBevelMaskA0;
out float tcBevelMaskA1;
out float tcBevelMaskB0;
out float tcBevelMaskB1;

// ── Distant terrain rise moved out of this file ─────────────────────────
// This used to also compute a noise-based vertical rise for distant top
// faces, applied right here to worldPos before handing off to the TCS/TES.
// That can't work correctly: a vertex shader runs once per REAL mesh
// vertex, before tessellation ever sees it. Outside the Tier0 ring the TCS
// pins tess level to 1, so any per-vertex height offset computed here only
// ever lands on the 4 corners of a patch — and because this engine greedy-
// merges same-material block faces into much larger quads (aQuadSize),
// those corners can be tens of blocks apart. Bilinearly filling that gap
// with a flat plane between 4 noisy corners is exactly what produced the
// faceted "wireframe" look — no amount of vertex-shader-only tuning fixes
// that, since a VS has no ability to add geometry.
//
// The rise now lives in StandardSurface.tes's evalDisplacedSurface(),
// where it's evaluated per actual tessellated vertex (using that vertex's
// true world XZ), paired with a new light tessellation tier in
// StandardSurface.tcs that only activates in that same far ring. See the
// "Distant terrain rise" block in the .tes for the tunables
// (DISTORT_AMPLITUDE etc.) and TIER1_MAX_SQ_DIST/DISTORT_START gating.
//
// This file goes back to being what it was before either attempt: it just
// places the raw block-face geometry in world space (aPos + grid slot
// offset) and hands it downstream untouched.
// ──────────────────────────────────────────────────────────────────────────

void main() {
    vec3 worldPos  = aPos;
    worldPos.x    += u_gridPosition.x;
    worldPos.z    += u_gridPosition.y;

    vec3 normal = NORMALS[int(aNorIndex)];

    gl_Position    = vec4(worldPos, 1.0);
    tcWorldPos     = worldPos;
    tcNormal       = normal;
    tcUVOrigin     = aUVOrigin;
    tcOrient       = aOrient;
    tcColor        = aColor;
    tcQuadSize     = aQuadSize;
    tcBevelMaskA0  = aBevelMaskA0;
    tcBevelMaskA1  = aBevelMaskA1;
    tcBevelMaskB0  = aBevelMaskB0;
    tcBevelMaskB1  = aBevelMaskB1;
}