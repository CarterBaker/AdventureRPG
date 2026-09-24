#version 330 core

in vec2 v_uv;
out vec4 o_color;

uniform sampler2D u_source;
uniform bool u_premultiplied;
uniform bool u_resolve;

const int BLIT_RESOLVE_RADIUS = 1;

// Averages the 3x3 block of source texels around this pixel, gathered with
// linear taps one texel apart so a reduced-resolution source upscales
// smoothly. Raymarched passes jitter each pixel by interleaved gradient
// noise, whose every 3x3 block holds an even set of offsets, so the average
// resolves the jitter instead of showing it as a dither.
vec4 resolveSource(vec2 uv) {

    vec2 texelSize = 1.0 / vec2(textureSize(u_source, 0));
    vec4 color     = vec4(0.0);

    for (int y = -BLIT_RESOLVE_RADIUS; y <= BLIT_RESOLVE_RADIUS; y++) {
        for (int x = -BLIT_RESOLVE_RADIUS; x <= BLIT_RESOLVE_RADIUS; x++)
            color += texture(u_source, uv + vec2(x, y) * texelSize);
    }

    return color / float((BLIT_RESOLVE_RADIUS * 2 + 1) * (BLIT_RESOLVE_RADIUS * 2 + 1));
}

void main() {

    vec4 color = u_resolve ? resolveSource(v_uv) : texture(u_source, v_uv);

    // Premultiplied sources are restored to straight alpha for the window blend
    if (u_premultiplied) {

        if (color.a <= 0.0)
            discard;

        color.rgb /= color.a;
    }

    o_color = color;
}
