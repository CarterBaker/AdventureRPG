#version 330 core

in vec2 v_texCoord;
layout(location = 0) out vec4 o_focus;
layout(location = 1) out vec4 o_bloom;

#include "includes/PostProcessData.glsl"
#include "postprocessing/effects/includes/PostFocus.glsl"

/*
 * Halves the composited scene for the blur pass. Each half-resolution pixel
 * takes the average of the four scene pixels it covers, and the circle of
 * confusion of the nearest of them, so a foreground edge keeps its own blur
 * instead of borrowing the background's; only a pixel covering nothing but
 * sky stays sharp as the sky. The bright parts of the scene are drawn out
 * beside it for bloom, rising softly over a knee rather than cutting in at
 * the threshold.
 */

// The composited scene of every layer.
uniform sampler2D u_sceneColor;

const float BLOOM_THRESHOLD = 0.72;
const float BLOOM_KNEE      = 0.22;
const float BLOOM_EPSILON   = 0.0001;
const int   PREFILTER_TAPS  = 4;

const ivec2 PREFILTER_OFFSETS[PREFILTER_TAPS] = ivec2[](ivec2(0, 0), ivec2(1, 0), ivec2(0, 1), ivec2(1, 1));

float resolveNearestCoc(vec2 uv) {
    float nearest = DOF_SKY_DEPTH;

    for (int i = 0; i < PREFILTER_TAPS; i++)
    nearest = min(nearest, resolveSurfaceDepth(resolveDepthTexel(uv, PREFILTER_OFFSETS[i])));

    return resolveSurfaceCoc(nearest);
}

vec3 resolveBloomSource(vec3 color) {
    float brightness   = max(color.r, max(color.g, color.b));
    float knee         = clamp(brightness - BLOOM_THRESHOLD + BLOOM_KNEE, 0.0, 2.0 * BLOOM_KNEE);
    float soft         = knee * knee / (4.0 * BLOOM_KNEE + BLOOM_EPSILON);
    float contribution = max(soft, brightness - BLOOM_THRESHOLD) / max(brightness, BLOOM_EPSILON);

    return color * contribution;
}

void main() {
    vec3 color = texture(u_sceneColor, v_texCoord).rgb;

    o_focus = vec4(color, resolveNearestCoc(v_texCoord));
    o_bloom = vec4(resolveBloomSource(color), 1.0);
}
