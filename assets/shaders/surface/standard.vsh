#version 150

in vec3 vertPos;

uniform mat4 u_inverseView;
uniform mat4 u_inverseProjection;

void main() {
    gl_Position = inverse(u_inverseProjection) * inverse(u_inverseView) * vec4(vertPos, 1.0);
}
