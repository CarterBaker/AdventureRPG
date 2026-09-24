#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;

#include "includes/CameraData.glsl"

out vec2 vGrid;

void main() {
    vGrid       = aUV;
    gl_Position = u_viewProjection * vec4(aPos, 1.0);
}
