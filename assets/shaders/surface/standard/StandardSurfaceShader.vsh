#version 400 core

#include "includes/GridCoordinateData.glsl"
#include "includes/SettingsData.glsl"
#include "includes/QuadRecord.glsl"
#include "surface/includes/SurfaceStandard.glsl"
#include "surface/includes/SurfaceTessellationTier.glsl"

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
out float tcCoverage;
out float tcContent;

// Places one corner of a solid quad in world space from the quad's packed record (see includes/QuadRecord.glsl),
// three texels of four ints. Int 0 is the quad's origin sub-block. Int 1 is the geometry meta word: face index,
// encoded face orientation, both merged extents in sub-blocks less one, the natural-block flag, the edge
// resolution and the ID of the content its block holds. Int 2 holds the coverage word in its low sixteen bits and the tile's atlas column and row in the high
// two bytes, from which the tile's UV origin is rebuilt. The rest is a stream of twelve 24-bit words: the four corner
// tints as exact RGB triples, then the low and the high word of each edge (A0, A1, B0, B1 in xyzw), whose four-bit
// column codes cover one entry each across a run capped at ten entries plus one padding entry at either end. An entry
// covers two sub-blocks on a block-resolution quad and one on a sub-block quad. A corner on the positive side of its
// face sits one sub-block out from the origin along the normal, so the quad lies on the face it shows. Extents leave
// here in blocks. Per-vertex displacement still happens after tessellation, since tessellation only ever sees a merged
// quad's four real corners. The layout must match the one SurfaceEmissionBranch writes.

void main() {
    int   base   = getQuadIndex(gl_VertexID) * 3;
    int   corner = getQuadCorner(gl_VertexID);
    uvec4 head   = texelFetch(u_quadBuffer, base);
    uvec4 middle = texelFetch(u_quadBuffer, base + 1);
    uvec4 tail   = texelFetch(u_quadBuffer, base + 2);

    int meta  = int(head.y);
    int face  = meta & QUAD_FACE_MASK;
    int sizeA = ((meta >>  9) & 31) + 1;
    int sizeB = ((meta >> 14) & 31) + 1;

    ivec3 cornerSub = unpackQuadPosition(head.x) + max(QUAD_NORMALS[face], ivec3(0))
                    + resolveQuadCornerOffset(face, corner, sizeA, sizeB);

    vec3 worldPos  = vec3(cornerSub) * QUAD_SUB_BLOCK_SIZE;
    worldPos.xz   += u_gridPosition;

    uvec4 tints    = unpackQuadWords(head.w, middle.x, middle.y);
    int   col      = int(tints[corner]);
    uint  surface  = head.z;
    uint  cell     = surface >> 16;

    gl_Position = vec4(worldPos, 1.0);
    tcWorldPos  = worldPos;
    tcNormal    = vec3(QUAD_NORMALS[face]);
    tcUVOrigin  = vec2(float(cell & 255u), float(cell >> 8)) * u_uvPerBlock;
    tcOrient    = float((meta >> 3) & 63);
    tcSizeA     = float(sizeA) * SUB_BLOCK_SIZE;
    tcSizeB     = float(sizeB) * SUB_BLOCK_SIZE;
    tcNatural   = float((meta >> 19) & 1);
    tcEdgeCells = float(((meta >> 20) & 1) + 1);
    tcColor     = vec3(float((col >> 16) & 255),
        float((col >>  8) & 255),
        float(col        & 255)) * (1.0 / 255.0);
    tcEdgeLow   = vec4(unpackQuadWords(middle.z, middle.w, tail.x));
    tcEdgeHigh  = vec4(unpackQuadWords(tail.y, tail.z, tail.w));
    tcCoverage  = float(surface & 65535u);
    tcContent   = float((meta >> 21) & 255);
}
