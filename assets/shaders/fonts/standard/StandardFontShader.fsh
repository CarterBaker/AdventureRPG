#version 330 core

in vec2 v_uv;

uniform sampler2DArray u_fontAtlas;
uniform vec4 u_color;

out vec4 out_color;

void main() {

    float alpha = texture(u_fontAtlas, vec3(v_uv, 0.0)).a;

    if (alpha <= 0.0)
        discard;

    out_color = vec4(u_color.rgb, u_color.a * alpha);
}