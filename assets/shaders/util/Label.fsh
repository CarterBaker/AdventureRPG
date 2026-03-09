#version 330 core

in vec2 v_uv;
out vec4 out_color;

uniform sampler2D u_fontAtlas;
uniform mat4      u_transform;
uniform vec4      u_color;

void main() {
    // Sample alpha from the font atlas — glyphs are rasterized as RGBA white
    // on transparent so alpha carries the glyph shape
    float alpha = texture(u_fontAtlas, v_uv).a;

    if (alpha <= 0.0)
        discard;

    out_color = vec4(u_color.rgb, u_color.a * alpha);
}
