#version 400 core

#include "includes/GridCoordinateData.glsl"
#include "includes/QuadRecord.glsl"

out vec3  tcWorldPos;
out vec3  tcNormal;
out float tcLevel;
out float tcSurface;
out float tcTidal;
out float tcFlow;
out float tcFall;

// Places a liquid face corner in grid space from the face's packed record (see includes/QuadRecord.glsl) and hands
// its liquid flags on to tessellation. Int 0 is the face's first corner in sub-blocks; int 1 holds the face index in
// its low three bits, both extents in blocks less one in the next four each, the fluid level 0..LIQUID_LEVEL_MAX in
// the next seven, the tidal flag above them, the flow code above that (LiquidFlowUtility) and the fall in the top
// eight: on an upright face how far above its top edge its fall's lip stands, handed on as the lip's height, on a
// churning top face the drop poured onto it. Whichever corners sit highest are the face's surface corners — all four
// on an up face, none on a down face and the top pair of an upright one — and only surface corners of a tidal face
// ride the live tide. Every quad is one patch; nothing is lowered or displaced here, since that has to happen per
// tessellated vertex. The layout must match the one LiquidGeometryBranch writes.

void main() {
    int   corner = getQuadCorner(gl_VertexID);
    uvec4 record = texelFetch(u_quadBuffer, getQuadIndex(gl_VertexID));
    uint  word   = record.y;

    int   face   = int(word) & QUAD_FACE_MASK;
    int   sizeA  = int((word >> 3) & 15u) + 1;
    int   sizeB  = int((word >> 7) & 15u) + 1;
    ivec3 offset = resolveQuadCornerOffset(face, corner, sizeA, sizeB);
    ivec3 top    = resolveQuadCornerOffset(face, 2, sizeA, sizeB);

    vec3 origin    = vec3(unpackQuadPosition(record.x)) * QUAD_SUB_BLOCK_SIZE;
    vec3 worldPos  = origin + vec3(offset);
    worldPos.xz   += u_gridPosition;

    float surface  = (face != QUAD_FACE_DOWN && offset.y == top.y) ? 1.0 : 0.0;
    float fall     = float((word >> 24) & 255u);

    gl_Position = vec4(worldPos, 1.0);
    tcWorldPos  = worldPos;
    tcNormal    = vec3(QUAD_NORMALS[face]);
    tcLevel     = float((word >> 11) & 127u);
    tcSurface   = surface;
    tcTidal     = ((word >> 18) & 1u) != 0u ? surface : 0.0;
    tcFlow      = float((word >> 19) & 31u);
    tcFall      = QUAD_NORMALS[face].y == 0 ? origin.y + float(top.y) + fall : fall;
}
