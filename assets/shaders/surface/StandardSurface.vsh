#version 400 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in float aNorIndex;
layout (location = 2) in float aColor;
layout (location = 3) in vec2  aUVOrigin;
layout (location = 4) in float aOrient;
layout (location = 5) in float aQuadSize;   // packed: sizeA | (sizeB << 8)

#include "includes/GridCoordinateData.glsl"

const vec3 NORMALS[6] = vec3[](
    vec3(0, 0, 1),    // NORTH 0
    vec3(1, 0, 0),    // EAST  1
    vec3(0, 0,-1),    // SOUTH 2
    vec3(-1, 0, 0),    // WEST  3
    vec3(0, 1, 0),    // UP    4
    vec3(0,-1, 0));   // DOWN  5

out vec3  tcWorldPos;
out vec3  tcNormal;
out vec2  tcUVOrigin;
out float tcOrient;
out float tcColor;
out float tcQuadSize;

void main() {
    vec3 worldPos  = aPos;
    worldPos.x    += u_gridPosition.x;
    worldPos.z    += u_gridPosition.y;

    // Store world pos in gl_Position — TES does the projection
    gl_Position = vec4(worldPos, 1.0);

    tcWorldPos = worldPos;
    tcNormal   = NORMALS[int(aNorIndex)];
    tcUVOrigin = aUVOrigin;
    tcOrient   = aOrient;
    tcColor    = aColor;
    tcQuadSize = aQuadSize;
}