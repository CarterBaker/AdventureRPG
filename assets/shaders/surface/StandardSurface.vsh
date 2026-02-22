#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in vec3  aNormal;
layout (location = 2) in float aColor;
layout (location = 3) in vec2  aTexCoord;

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"

out vec3 vWorldNormal;
out vec2 vTexCoord;
out vec4 vBiomeColor;

vec4 unpackColor(float packedColor) {
    int bits = floatBitsToInt(packedColor);
    float r = float((bits >> 24) & 0xFF) / 255.0;
    float g = float((bits >> 16) & 0xFF) / 255.0;
    float b = float((bits >>  8) & 0xFF) / 255.0;
    float a = float((bits) & 0xFF) / 255.0;
    return vec4(r, g, b, a);
}

void main() {
    vWorldNormal = normalize(aNormal);
    vTexCoord    = aTexCoord;
    vBiomeColor  = unpackColor(aColor);

    vec3 worldPos  = aPos;
    worldPos.x    += u_gridPosition.x;
    worldPos.z    += u_gridPosition.y;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}