#version 330 core

in vec2 v_uv;
out vec4 o_color;

uniform sampler2D u_source;
uniform bool u_premultiplied;

void main() {

    vec4 color = texture(u_source, v_uv);

    // Premultiplied sources are restored to straight alpha for the window blend
    if (u_premultiplied) {

        if (color.a <= 0.0)
            discard;

        color.rgb /= color.a;
    }

    o_color = color;
}
