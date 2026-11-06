#version 330 core

in vec2 vQuadUV;

uniform vec4  u_ringColor;
uniform float u_ringWidth;
uniform float u_shape;
uniform float u_ratio;
uniform float u_angle;

out vec4 FragColor;

// Outlines the brush in its own shape, u_shape indexing BrushShape's order:
// 0 a disc, 1 a square, 2 a diamond, each turned u_angle radians
// counterclockwise and squashed across its length by u_ratio exactly as
// BrushUtility.measure does. The ring sits just inside the edge of its quad,
// u_ringWidth window pixels wide at any zoom, with edges softened over one
// pixel.
void main() {
    vec2  offset   = vQuadUV * 2.0 - 1.0;
    float c        = cos(u_angle);
    float s        = sin(u_angle);
    vec2  tip      = vec2(offset.x * c + offset.y * s, (offset.y * c - offset.x * s) / u_ratio);
    float distance = u_shape < 0.5
            ? length(tip)
            : u_shape < 1.5 ? max(abs(tip.x), abs(tip.y)) : abs(tip.x) + abs(tip.y);
    float pixel    = fwidth(distance);
    float inset    = abs(distance - (1.0 - u_ringWidth * pixel * 0.5));
    float ring     = 1.0 - smoothstep(u_ringWidth * pixel * 0.5, u_ringWidth * pixel * 0.5 + pixel, inset);

    if (ring <= 0.0)
    discard;

    FragColor = vec4(u_ringColor.rgb, u_ringColor.a * ring);
}
