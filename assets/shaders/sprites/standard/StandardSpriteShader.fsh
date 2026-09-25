#version 330 core

#include "includes/SliceData.glsl"

in vec2 v_uv;
out vec4 out_color;

uniform sampler2D u_sprite;
uniform mat4 u_transform;
uniform vec4 u_color;

// Maps one axis of a sliced element to texture pixels — borders stay fixed,
// the center either scales to fill (stretch) or repeats (tile)
float sliceAxis(float pixel, float rectSize, float borderLow, float borderHigh, float texSize) {

    if (pixel < borderLow)
        return pixel;

    if (pixel >= rectSize - borderHigh)
        return texSize - borderHigh + (pixel - (rectSize - borderHigh));

    float centerTex = texSize - borderLow - borderHigh;

    if (u_stretch > 0.5) {
        float centerRect = max(rectSize - borderLow - borderHigh, 1.0);
        return borderLow + (pixel - borderLow) / centerRect * centerTex;
    }

    return borderLow + mod(pixel - borderLow, centerTex);
}

void main() {
    float bL = u_border.x;
    float bB = u_border.y;
    float bR = u_border.z;
    float bT = u_border.w;

    // Output is premultiplied — menu targets blend and blit in premultiplied form

    // No border — normal sprite, straight UV passthrough
    if (bL == 0.0 && bB == 0.0 && bR == 0.0 && bT == 0.0) {
        vec4 color = texture(u_sprite, v_uv) * u_color;
        if (color.a <= 0.0) discard;
        out_color = vec4(color.rgb * color.a, color.a);
        return;
    }

    // Read element size from the transform basis — its column lengths stay
    // the width and height when an animated pose rotates the element
    // GLSL column-major: [col][row]
    float rectW = length(u_transform[0].xy);
    float rectH = length(u_transform[1].xy);

    vec2 pixel = v_uv * vec2(rectW, rectH);

    float u = sliceAxis(pixel.x, rectW, bL, bR, u_texSize.x) / u_texSize.x;
    float v = sliceAxis(pixel.y, rectH, bB, bT, u_texSize.y) / u_texSize.y;

    vec4 color = texture(u_sprite, vec2(u, v)) * u_color;
    if (color.a <= 0.0) discard;
    out_color = vec4(color.rgb * color.a, color.a);
}
