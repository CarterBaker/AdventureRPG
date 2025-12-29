#version 150

in vec2 a_position;  // Fullscreen quad positions in clip space (-1 to 1)
out vec2 v_uv;       // UVs for fragment shader

void main() {
    // Convert clip space [-1,1] → [0,1] for UV
    v_uv = a_position * 0.5 + 0.5;
    gl_Position = vec4(a_position, 0.0, 1.0);
}
