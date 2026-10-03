#version 330 core

in vec2 vQuadUV;

uniform vec4  u_ringColor;
uniform float u_ringWidth;

out vec4 FragColor;

// Outlines the brush: a ring just inside the edge of its quad, u_ringWidth
// window pixels wide at any zoom, with edges softened over one pixel.
void main() {
    float distance = length(vQuadUV * 2.0 - 1.0);
    float pixel    = fwidth(distance);
    float inset    = abs(distance - (1.0 - u_ringWidth * pixel * 0.5));
    float ring     = 1.0 - smoothstep(u_ringWidth * pixel * 0.5, u_ringWidth * pixel * 0.5 + pixel, inset);

    if (ring <= 0.0)
    discard;

    FragColor = vec4(u_ringColor.rgb, u_ringColor.a * ring);
}
