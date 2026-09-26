#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in vec2  aUVOrigin;
layout (location = 2) in float aMeta;
layout (location = 3) in float aColor;
layout (location = 4) in vec4  aEdgeLow;
layout (location = 5) in vec4  aEdgeHigh;

#include "includes/GridCoordinateData.glsl"
#include "includes/SettingsData.glsl"
#include "surface/includes/SurfaceTessellationTier.glsl"

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
out vec3  tcColor;
out float tcSizeA;
out float tcSizeB;
out float tcNatural;
out float tcEdgeCells;
out vec4  tcEdgeLow;
out vec4  tcEdgeHigh;

// Places raw block-face geometry in world space and unpacks the integer-packed vertex attributes. Face
// index, encoded face orientation, both merged quad extents in sub-blocks, the natural-block flag and the
// edge resolution share one 21-bit word and vertex tint is an exact 24-bit RGB triple. Each edge (A0, A1, B0,
// B1 in xyzw) carries four-bit column codes, one per entry across a run capped at ten entries plus one padding
// entry at either end, split into a low word of six entries and a high word of the rest — twenty-four bits
// each and therefore exact in a float32 mantissa. An entry covers two sub-blocks on a block-resolution quad and
// one on a sub-block quad. Extents leave here in blocks. Per-vertex displacement still happens after
// tessellation, since tessellation only ever sees a merged quad's four real corners. The layout must match
// the one SurfaceEmissionBranch writes.

void main() {
    vec3 worldPos  = aPos;
    worldPos.x    += u_gridPosition.x;
    worldPos.z    += u_gridPosition.y;

    int meta = int(aMeta);
    int col  = int(aColor);

    gl_Position = vec4(worldPos, 1.0);
    tcWorldPos  = worldPos;
    tcNormal    = NORMALS[meta & 7];
    tcUVOrigin  = aUVOrigin;
    tcOrient    = float((meta >> 3) & 63);
    tcSizeA     = float(((meta >>  9) & 31) + 1) * SUB_BLOCK_SIZE;
    tcSizeB     = float(((meta >> 14) & 31) + 1) * SUB_BLOCK_SIZE;
    tcNatural   = float((meta >> 19) & 1);
    tcEdgeCells = float(((meta >> 20) & 1) + 1);
    tcColor     = vec3(float((col >> 16) & 255),
        float((col >>  8) & 255),
        float(col        & 255)) * (1.0 / 255.0);
    tcEdgeLow   = aEdgeLow;
    tcEdgeHigh  = aEdgeHigh;
}
