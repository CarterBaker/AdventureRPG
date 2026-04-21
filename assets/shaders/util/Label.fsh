#version 330 core

in vec2 v_uv;
out vec4 out_color;

uniform sampler2D u_fontAtlas;
uniform vec4 u_color;

void main() {
    float alpha = texture(u_fontAtlas, v_uv).a;

    if (alpha <= 0.0)
        discard;

    out_color = vec4(u_color.rgb, u_color.a * alpha);
}
