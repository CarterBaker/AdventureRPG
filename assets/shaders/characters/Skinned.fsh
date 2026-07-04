#version 330 core

in vec2 v_uv;
out vec4 fragColor;

uniform sampler2D u_diffuse;

void main() {
    fragColor = texture(u_diffuse, v_uv);
}