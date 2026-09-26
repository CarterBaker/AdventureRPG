#version 330 core

in vec2 vCell;

uniform vec4  u_color;
uniform vec4  u_lineColor;
uniform float u_lineWidth;

out vec4 FragColor;

/*
 * The inside of a container: a plain face ruled with a grid, one line for
 * every few sub-voxels, so the space items take up can be read at a glance.
 */
void main() {
    vec2  offset = fract(vCell);
    vec2  edge   = min(offset, 1.0 - offset);
    float line   = step(min(edge.x, edge.y), u_lineWidth);

    FragColor = vec4(mix(u_color.rgb, u_lineColor.rgb, line * u_lineColor.a), u_color.a);
}
