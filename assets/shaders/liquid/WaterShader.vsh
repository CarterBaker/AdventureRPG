#version 400 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in vec2  aUVOrigin;   // unused for liquid — same ChunkVAO layout StandardSurfaceShader reads
layout (location = 2) in float aMeta;       // face index in bits 0-2, packed exactly like solid geometry
layout (location = 3) in float aColor;      // unused for liquid
layout (location = 4) in vec4  aLiquid;     // x: fluid level 0..LIQUID_LEVEL_MAX, y: 1.0 on surface-height vertices,
                                            // z: 1.0 on tidal ocean surface vertices, w: unused
layout (location = 5) in vec4  aEdgeHigh;   // unused for liquid — solid geometry's high edge words

#include "includes/GridCoordinateData.glsl"

const vec3 NORMALS[6] = vec3[](
    vec3(0, 0, 1),
    vec3(1, 0, 0),
    vec3(0, 0,-1),
    vec3(-1, 0, 0),
    vec3(0, 1, 0),
    vec3(0,-1, 0));

out vec3  tcWorldPos;
out vec3  tcNormal;
out float tcLevel;
out float tcSurface;
out float tcTidal;

// Places a liquid face corner in grid space and hands its liquid flags on to tessellation. Every quad is one
// patch; nothing is lowered or displaced here, since that has to happen per tessellated vertex.

void main() {
    vec3 worldPos = aPos;
    worldPos.x   += u_gridPosition.x;
    worldPos.z   += u_gridPosition.y;

    gl_Position = vec4(worldPos, 1.0);
    tcWorldPos  = worldPos;
    tcNormal    = NORMALS[int(aMeta) & 7];
    tcLevel     = aLiquid.x;
    tcSurface   = aLiquid.y;
    tcTidal     = aLiquid.z;
}
