#version 330 core

#include "includes/SliceData.glsl"

in vec2 v_uv;
out vec4 out_color;

uniform sampler2D u_sprite;
uniform mat4 u_transform;

void main() {
    float bL = u_border.x;
    float bB = u_border.y;
    float bR = u_border.z;
    float bT = u_border.w;

    // No border — normal sprite, straight UV passthrough
    if (bL == 0.0 && bB == 0.0 && bR == 0.0 && bT == 0.0) {
        vec4 color = texture(u_sprite, v_uv);
        if (color.a <= 0.0) discard;
        out_color = color;
        return;
    }

    // Read element size directly from the transform scale
    // transform is: [ w  0  0  tx ]
    //               [ 0  h  0  ty ]
    //               [ 0  0  1  0  ]
    //               [ 0  0  0  1  ]
    // GLSL column-major: [col][row]
    float rectW = u_transform[0][0];
    float rectH = u_transform[1][1];

    vec2 pixel = v_uv * vec2(rectW, rectH);

    // X
    float u;
    if (pixel.x < bL) {
        u = pixel.x / u_texSize.x;
    } else if (pixel.x >= rectW - bR) {
        float fromRight = pixel.x - (rectW - bR);
        u = (u_texSize.x - bR + fromRight) / u_texSize.x;
    } else {
        float centerTexW = u_texSize.x - bL - bR;
        u = (bL + mod(pixel.x - bL, centerTexW)) / u_texSize.x;
    }

    // Y
    float v;
    if (pixel.y < bB) {
        v = pixel.y / u_texSize.y;
    } else if (pixel.y >= rectH - bT) {
        float fromTop = pixel.y - (rectH - bT);
        v = (u_texSize.y - bT + fromTop) / u_texSize.y;
    } else {
        float centerTexH = u_texSize.y - bB - bT;
        v = (bB + mod(pixel.y - bB, centerTexH)) / u_texSize.y;
    }

    vec4 color = texture(u_sprite, vec2(u, v));
    if (color.a <= 0.0) discard;
    out_color = color;
}