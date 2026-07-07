#version 330 core

layout (location = 0) in vec3 aPos;       // xyz (3 floats)
layout (location = 1) in vec3 aNormal;    // nx, ny, nz (3 floats)
layout (location = 2) in float aColor;    // color (1 float)
layout (location = 3) in vec2 aTexCoord;  // u, v (2 floats)

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"

out vec3 vWorldNormal;

void main() {
    vWorldNormal = normalize(aNormal);

    vec3 worldPos = aPos;
    worldPos.x += u_gridPosition.x;
    worldPos.z += u_gridPosition.y;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}