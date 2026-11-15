#version 330 core

in vec2 vUV;

#include "includes/TileTextures.glsl"

uniform int   u_tileID;
uniform vec4  u_backgroundColor;
uniform vec4  u_checkerColor;
uniform vec4  u_borderColor;
uniform float u_checkerCells;
uniform float u_borderWidth;
uniform float u_textureShare;

out vec4 FragColor;

// The tile the faces shown are textured with, laid over the unit square of UV space (a checker when no tile is
// known), dimmed by u_textureShare so the UVs drawn over it read clearly, with the square's border measured in
// screen pixels and the backdrop beyond it.
void main() {
    vec2 dx    = dFdx(vUV);
    vec2 dy    = dFdy(vUV);
    vec2 pixel = fwidth(vUV);
    vec4 color = u_backgroundColor;

    if (all(greaterThanEqual(vUV, vec2(0.0))) && all(lessThanEqual(vUV, vec2(1.0)))) {
        vec2  cell    = floor(vUV * u_checkerCells);
        float checker = mod(cell.x + cell.y, 2.0);

        color = mix(u_backgroundColor, u_checkerColor, checker);

        if (u_tileID >= 0) {
            vec4 texel = sampleTileAlbedo(resolveTileReadGrad(u_tileID, dx, dy), vUV, dx, dy);
            color.rgb  = mix(color.rgb, texel.rgb, texel.a * u_textureShare);
        }
    }

    vec2  reach  = pixel * u_borderWidth;
    vec2  within = step(-reach, vUV) * step(vUV, vec2(1.0) + reach);
    vec2  line   = step(min(abs(vUV), abs(vUV - vec2(1.0))), reach);
    float onEdge = max(line.x * within.y, line.y * within.x);

    color.rgb = mix(color.rgb, u_borderColor.rgb, onEdge * u_borderColor.a);
    FragColor = vec4(color.rgb, 1.0);
}
