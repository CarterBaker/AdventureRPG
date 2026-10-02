#version 330 core

in vec2 vGrid;

uniform float u_resolution;
uniform float u_fadeDistance;
uniform vec2  u_extent;
uniform vec4  u_lineColor;
uniform vec4  u_borderColor;

out vec4 FragColor;

// vGrid is floor position in block units. A line is drawn on every
// sub-voxel boundary and a heavier line on every block boundary, both
// measured in screen pixels. Lines fade out with distance from the blocks
// the item is built in, u_extent of them across on x and z.
void main() {
    vec2  cell     = vGrid * u_resolution;
    vec2  cellLine = abs(fract(cell - 0.5) - 0.5) / fwidth(cell);
    float minor    = 1.0 - min(min(cellLine.x, cellLine.y), 1.0);

    vec2  blockLine = abs(fract(vGrid - 0.5) - 0.5) / fwidth(vGrid);
    float major     = 1.0 - min(min(blockLine.x, blockLine.y) * 0.5, 1.0);

    vec2  outside = max(max(-vGrid, vGrid - u_extent), 0.0);
    float fade    = 1.0 - clamp(length(outside) / u_fadeDistance, 0.0, 1.0);

    vec4 color = mix(u_lineColor, u_borderColor, major);
    color.a   *= max(minor, major) * fade;

    if (color.a < 0.01)
    discard;

    FragColor = color;
}
