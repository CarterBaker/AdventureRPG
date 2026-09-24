#version 330 core

in vec2 vGrid;

uniform float u_resolution;
uniform vec4  u_lineColor;
uniform vec4  u_borderColor;

out vec4 FragColor;

// One line per sub-voxel boundary across the block floor, with the block's
// own edge drawn as a heavier border. Line width is measured in screen
// pixels so the grid stays crisp at every zoom.
void main() {
    vec2  cell     = vGrid * u_resolution;
    vec2  cellLine = abs(fract(cell - 0.5) - 0.5) / fwidth(cell);
    float line     = 1.0 - min(min(cellLine.x, cellLine.y), 1.0);

    vec2  edge   = min(vGrid, 1.0 - vGrid) / fwidth(vGrid);
    float border = 1.0 - min(min(edge.x, edge.y) * 0.5, 1.0);

    vec4 color = mix(u_lineColor, u_borderColor, border);
    color.a   *= max(line, border);

    if (color.a < 0.01)
    discard;

    FragColor = color;
}
