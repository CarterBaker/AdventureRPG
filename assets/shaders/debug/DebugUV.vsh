#version 150

in vec2 a_position;  // full-screen triangle [-1,1]
out vec2 v_uv;       // optional UV for fragment shader

void main() {
    // Output clip-space position
    gl_Position = vec4(a_position, 0.0, 1.0);

    // Map from NDC [-1,1] to UV [0,1]
    v_uv = a_position * 0.5 + 0.5;
}
