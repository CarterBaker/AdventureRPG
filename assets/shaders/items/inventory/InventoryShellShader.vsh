#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;

// The unit quad spans x and z; u_model stretches it over one face of a
// container and carries it into window pixels.
uniform mat4 u_projection;
uniform mat4 u_model;
uniform vec2 u_cells;

out vec2 vCell;

void main() {
    vCell       = aPos.xz * u_cells;
    gl_Position = u_projection * u_model * vec4(aPos, 1.0);
}
