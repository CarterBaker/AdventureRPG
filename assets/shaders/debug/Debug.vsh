#version 330 core

layout(location = 0) in vec3 aPosition;

void main() {
    // Pass through position
    gl_Position = vec4(aPosition, 1.0);
}
