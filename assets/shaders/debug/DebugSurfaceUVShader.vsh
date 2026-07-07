#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in vec3  aNormal;
layout (location = 2) in float aColor;
layout (location = 3) in vec2  aTexCoord;

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"

out vec2 vTexCoord;
out float vLayerIndex;

void main() {
    // Pass through UVs
    vTexCoord = aTexCoord;

    // For debug: encode the layer index from aColor if needed
    vLayerIndex = aColor; // this can be ignored or replaced with 0 if not using packed layer

    // Compute world position
    vec3 worldPos = aPos;
    worldPos.x += u_gridPosition.x;
    worldPos.z += u_gridPosition.y;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}