#version 330 core

layout (location = 0) in vec2 a_localPos;
layout (location = 1) in vec2 a_screenPos;
layout (location = 2) in vec2 a_screenSize;
layout (location = 3) in vec4 a_atlasUV;

#include "includes/CameraData.glsl"

out vec2 v_uv;

void main() {

    vec2 pixelPos = a_screenPos + (a_localPos * a_screenSize);

    vec2 ndc;
    ndc.x =  (pixelPos.x / u_viewport.x) * 2.0 - 1.0;
    ndc.y = 1.0 - (pixelPos.y / u_viewport.y) * 2.0;

    v_uv = a_atlasUV.xy + (a_localPos * a_atlasUV.zw);

    gl_Position = vec4(ndc, 0.0, 1.0);
}