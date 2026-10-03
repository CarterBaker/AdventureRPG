#ifndef POST_ANTI_ALIASING_GLSL
#define POST_ANTI_ALIASING_GLSL

/*
 * Fast approximate anti-aliasing over the composited scene. The four
 * diagonal neighbours give the direction an edge runs, and the pixel is
 * blended along that edge, never across it; a wider blend is kept only while
 * its brightness stays within the neighbourhood's, so thin lines and the
 * outlines' ink are never smeared away. Pixels with too little contrast
 * around them are left untouched, so texture detail stays crisp.
 */

const vec3  FXAA_LUMA           = vec3(0.299, 0.587, 0.114);
const float FXAA_SPAN_MAX       = 8.0;
const float FXAA_REDUCE_MUL     = 1.0 / 8.0;
const float FXAA_REDUCE_MIN     = 1.0 / 128.0;
const float FXAA_EDGE_THRESHOLD = 0.125;
const float FXAA_EDGE_MINIMUM   = 0.03125;
const float FXAA_NEAR_TAP       = 1.0 / 3.0 - 0.5;
const float FXAA_FAR_TAP        = 2.0 / 3.0 - 0.5;

vec3 applyAntiAliasing(sampler2D source, vec2 uv, vec2 texel) {
    vec3 rgbM  = texture(source, uv).rgb;
    vec3 rgbNW = texture(source, uv + vec2(-1.0, -1.0) * texel).rgb;
    vec3 rgbNE = texture(source, uv + vec2( 1.0, -1.0) * texel).rgb;
    vec3 rgbSW = texture(source, uv + vec2(-1.0,  1.0) * texel).rgb;
    vec3 rgbSE = texture(source, uv + vec2( 1.0,  1.0) * texel).rgb;

    float lumaM  = dot(rgbM, FXAA_LUMA);
    float lumaNW = dot(rgbNW, FXAA_LUMA);
    float lumaNE = dot(rgbNE, FXAA_LUMA);
    float lumaSW = dot(rgbSW, FXAA_LUMA);
    float lumaSE = dot(rgbSE, FXAA_LUMA);

    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

    if (lumaMax - lumaMin < max(FXAA_EDGE_MINIMUM, lumaMax * FXAA_EDGE_THRESHOLD))
    return rgbM;

    vec2 direction = vec2(
        -((lumaNW + lumaNE) - (lumaSW + lumaSE)),
        (lumaNW + lumaSW) - (lumaNE + lumaSE));

    float directionReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * 0.25 * FXAA_REDUCE_MUL, FXAA_REDUCE_MIN);
    float inverseDirectionMin = 1.0 / (min(abs(direction.x), abs(direction.y)) + directionReduce);

    direction = clamp(direction * inverseDirectionMin, vec2(-FXAA_SPAN_MAX), vec2(FXAA_SPAN_MAX)) * texel;

    vec3 rgbA = 0.5 * (
        texture(source, uv + direction * FXAA_NEAR_TAP).rgb +
        texture(source, uv + direction * FXAA_FAR_TAP).rgb);
    vec3 rgbB = rgbA * 0.5 + 0.25 * (
        texture(source, uv - direction * 0.5).rgb +
        texture(source, uv + direction * 0.5).rgb);

    float lumaB = dot(rgbB, FXAA_LUMA);

    return (lumaB < lumaMin || lumaB > lumaMax) ? rgbA : rgbB;
}

#endif
