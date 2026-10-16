#version 330 core
in vec2 a_position;

// u_model stretches the unit quad over a slot in window pixels, pushed far
// back in depth so every icon draws in front of it; u_projection maps those
// pixels to clip space.
uniform mat4 u_projection;
uniform mat4 u_model;

out vec2 v_uv;

void main() {
    v_uv        = a_position;
    gl_Position = u_projection * u_model * vec4(a_position, 0.0, 1.0);
}
