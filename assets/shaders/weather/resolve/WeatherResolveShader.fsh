#version 330 core

in vec2 v_uv;
out vec4 o_color;

uniform sampler2D u_source;

/*
 * Composites the reduced-resolution cloud target onto the window. The cloud
 * march offsets every pixel's first step by interleaved gradient noise, which
 * hides banding but leaves a fine dither wherever a few long steps decide a
 * pixel — toward the horizon, where rays cross the most cloud. That noise is
 * spread so every 3x3 block of pixels holds an even set of offsets, so the
 * block's average is the stratified result of nine offsets and the dither
 * resolves away. The block is gathered with linear taps one source texel
 * apart, which upscales it smoothly at the same time. The source is
 * premultiplied, so edges average against transparency rather than black;
 * straight alpha is restored for the window blend.
 */

const int WEATHER_RESOLVE_RADIUS = 1;

void main() {

    vec2 texelSize = 1.0 / vec2(textureSize(u_source, 0));
    vec4 color     = vec4(0.0);

    for (int y = -WEATHER_RESOLVE_RADIUS; y <= WEATHER_RESOLVE_RADIUS; y++) {
        for (int x = -WEATHER_RESOLVE_RADIUS; x <= WEATHER_RESOLVE_RADIUS; x++)
            color += texture(u_source, v_uv + vec2(x, y) * texelSize);
    }

    float tapCount = float((WEATHER_RESOLVE_RADIUS * 2 + 1) * (WEATHER_RESOLVE_RADIUS * 2 + 1));
    color /= tapCount;

    if (color.a <= 0.0)
        discard;

    color.rgb /= color.a;

    o_color = color;
}
