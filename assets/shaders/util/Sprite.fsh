#version 330 core

in vec2 v_uv;
out vec4 out_color;

uniform sampler2D u_sprite;

void main() {
    vec4 color = texture(u_sprite, v_uv);
    if (color.a <= 0.0)
    discard;
    out_color = color;
}