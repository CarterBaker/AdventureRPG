#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in float aNorIndex;
layout (location = 2) in float aColor;
layout (location = 3) in vec2  aUVOrigin;
layout (location = 4) in float aOrient;   // encodedFace 0-23

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"

const vec3 NORMALS[6] = vec3[](

    // NORTH  0
    vec3(0, 0, 1),

    // EAST   1
    vec3(1, 0, 0),

    // SOUTH  2
    vec3(0, 0,-1),

    // WEST   3
    vec3(-1, 0, 0),

    // UP     4
    vec3(0, 1, 0),

    // DOWN   5
    vec3(0,-1, 0));

out vec3       vLocalPos;
out vec3       vNormal;
flat out vec2  vUVOrigin;
flat out float vOrient;

void main() {
    vLocalPos = aPos;
    vNormal   = NORMALS[int(aNorIndex)];
    vUVOrigin = aUVOrigin;
    vOrient   = aOrient;

    vec3 worldPos  = aPos;
    worldPos.x    += u_gridPosition.x;
    worldPos.z    += u_gridPosition.y;
    gl_Position    = u_viewProjection * vec4(worldPos, 1.0);
}