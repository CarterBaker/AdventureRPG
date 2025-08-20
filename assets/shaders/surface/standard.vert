#version 150

in vec2 position;        // your vertex positions
out vec2 fragUV;         // UVs for fragment shader

void main() {
    fragUV = position * 0.5 + 0.5;  // convert from [-1,1] to [0,1]
    gl_Position = vec4(position, 0.0, 1.0);
}
