#version 330 core

in vec2 v_texCoord;
layout(location = 0) out vec4 o_depthOfField;
layout(location = 1) out vec4 o_bloom;

#include "includes/PostProcessData.glsl"
#include "postprocessing/effects/includes/PostBlurRadius.glsl"

/*
 * Gathers the depth of field and the bloom at half resolution. The depth of
 * field samples a golden-angle disc as wide as the widest blur; a sample
 * reaches this pixel only when its own blur is wide enough to cover the
 * distance between them, and a sample behind this pixel may spread no
 * further than this pixel's own blur allows, so a sharp subject never takes
 * on the blurred background behind it while a blurred foreground still
 * spills softly over it. How much blurred foreground covers the pixel rides
 * in alpha for the composite. Bloom gathers a wide disc with weights falling
 * off toward its rim.
 */

// The prefilter's half-resolution scene, circle of confusion in alpha, and its bright parts.
uniform sampler2D u_prefilterFocus;
uniform sampler2D u_prefilterBloom;

const float GOLDEN_ANGLE            = 2.39996323;
const int   DOF_SAMPLE_COUNT        = 40;
const float DOF_REACH_SOFTNESS      = 0.5;
const float DOF_BACKGROUND_CLAMP    = 2.0;
const float DOF_NEAR_COVERAGE_SCALE = 2.0;
const int   BLOOM_SAMPLE_COUNT      = 16;
const float BLOOM_RADIUS_FRACTION   = 0.03;
const float BLOOM_RIM_FALLOFF       = 0.75;

// A point of a golden-angle disc of unit radius, spread evenly over its area.
vec2 resolveDiscOffset(int index, int count) {
    float radius = sqrt((float(index) + 0.5) / float(count));
    float theta  = float(index) * GOLDEN_ANGLE;
    return vec2(cos(theta), sin(theta)) * radius;
}

vec4 gatherDepthOfField(vec2 texel, float maxRadius) {
    vec4  center     = texture(u_prefilterFocus, v_texCoord);
    float centerSize = abs(center.a) * maxRadius;
    vec3  color      = center.rgb;
    float total      = 1.0;
    float coverage   = 0.0;

    for (int i = 0; i < DOF_SAMPLE_COUNT; i++) {
        vec2  offset     = resolveDiscOffset(i, DOF_SAMPLE_COUNT) * maxRadius;
        vec4  tap        = texture(u_prefilterFocus, v_texCoord + offset * texel);
        float sampleSize = abs(tap.a) * maxRadius;
        bool  behind     = tap.a > center.a;

        if (behind)
        sampleSize = min(sampleSize, centerSize * DOF_BACKGROUND_CLAMP);

        float distance = length(offset);
        float reach    = smoothstep(distance - DOF_REACH_SOFTNESS, distance + DOF_REACH_SOFTNESS, sampleSize);

        color += mix(color / total, tap.rgb, reach);
        total += 1.0;

        if (!behind)
        coverage += reach * clamp(-tap.a * DOF_NEAR_COVERAGE_SCALE, 0.0, 1.0);
    }

    return vec4(color / total, clamp(coverage / float(DOF_SAMPLE_COUNT) * DOF_NEAR_COVERAGE_SCALE, 0.0, 1.0));
}

vec3 gatherBloom(vec2 texel, float radius) {
    vec3  color  = texture(u_prefilterBloom, v_texCoord).rgb;
    float weight = 1.0;

    for (int i = 0; i < BLOOM_SAMPLE_COUNT; i++) {
        vec2  unit      = resolveDiscOffset(i, BLOOM_SAMPLE_COUNT);
        float tapWeight = 1.0 - dot(unit, unit) * BLOOM_RIM_FALLOFF;

        color  += texture(u_prefilterBloom, v_texCoord + unit * radius * texel).rgb * tapWeight;
        weight += tapWeight;
    }

    return color / weight;
}

void main() {
    vec2 size  = vec2(textureSize(u_prefilterFocus, 0));
    vec2 texel = 1.0 / size;

    o_depthOfField = u_dofEnabled ? gatherDepthOfField(texel, resolveDofRadius(size.y)) : vec4(0.0);
    o_bloom        = u_bloomEnabled ? vec4(gatherBloom(texel, size.y * BLOOM_RADIUS_FRACTION), 1.0) : vec4(0.0);
}
