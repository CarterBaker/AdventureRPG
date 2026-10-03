// Outline.glsl
#ifndef OUTLINE_GLSL
#define OUTLINE_GLSL

#include "includes/CameraData.glsl"
#include "includes/GBufferData.glsl"
#include "includes/PostProcessData.glsl"
#include "postprocessing/includes/InverseDistance.glsl"

/*
 * Cartoon ink outlines drawn from the G-buffer. A silhouette is a fragment
 * standing in front of what lies around it: across any plane the inverse
 * distance runs linear, so the four neighbours at the outline's thickness sum
 * to four times the centre, and a fragment in front sums short. The test is
 * relative, so a step of a single block reads as an edge at any distance, and
 * the band is narrow so lines come out solid rather than shaded. Only the
 * nearer side of an edge is inked, so every silhouette, the sky's included,
 * takes one clean line. A crease is a fold between two faces, found from how
 * far their normals turn; creases look one way only, so a fold is inked once
 * rather than on both faces. Outlines fade out far away before the terrain's
 * detail turns them to noise, and the ink is the surface's own lit color
 * pressed nearly to black, so lines read as drawn ink yet still take the fog
 * and clouds laid over them afterwards.
 */

const float OUTLINE_EPSILON           = 0.000001;
const float OUTLINE_SILHOUETTE_START  = 0.012;
const float OUTLINE_SILHOUETTE_END    = 0.030;
const float OUTLINE_CREASE_START      = 0.15;
const float OUTLINE_CREASE_END        = 0.30;
const float OUTLINE_CREASE_WEIGHT     = 1.0;
const float OUTLINE_FADE_START        = 96.0;
const float OUTLINE_FADE_END          = 256.0;
const float OUTLINE_INK_SHADE         = 0.04;
const int   OUTLINE_NEIGHBOUR_COUNT   = 4;

const ivec2 OUTLINE_NEIGHBOURS[OUTLINE_NEIGHBOUR_COUNT] = ivec2[](
    ivec2(1, 0), ivec2(0, 1), ivec2(-1, 0), ivec2(0, -1));

// How far a fold turns between this fragment's normal and a neighbour's, 0 flat to 1 square.
float resolveCrease(ivec2 neighbour, vec3 normalView) {
    return 1.0 - dot(normalView, normalize(texelFetch(u_gNormal, neighbour, 0).rgb));
}

// Ink coverage at one G-buffer texel, 0 to 1.
float resolveOutline(ivec2 texel, float rawDepth, vec3 normalView, float fragDistance) {
    if (!u_outlineEnabled)
    return 0.0;

    float fade = 1.0 - smoothstep(OUTLINE_FADE_START, OUTLINE_FADE_END, fragDistance);

    if (fade <= 0.0)
    return 0.0;

    ivec2 limit         = textureSize(u_gDepth, 0) - 1;
    float centerInverse = resolveInverseDistance(rawDepth);
    float aroundInverse = 0.0;
    float crease        = 0.0;

    for (int i = 0; i < OUTLINE_NEIGHBOUR_COUNT; i++) {
        ivec2 neighbour      = clamp(texel + OUTLINE_NEIGHBOURS[i] * u_outlineThickness, ivec2(0), limit);
        float neighbourDepth = texelFetch(u_gDepth, neighbour, 0).r;

        aroundInverse += resolveInverseDistance(neighbourDepth);

        // The first two neighbours look right and up; the sky behind a silhouette has no normal.
        if (i < OUTLINE_NEIGHBOUR_COUNT / 2 && neighbourDepth < 1.0)
        crease = max(crease, resolveCrease(neighbour, normalView));
    }

    float silhouette = float(OUTLINE_NEIGHBOUR_COUNT) - aroundInverse / max(centerInverse, OUTLINE_EPSILON);
    float ink        = max(
        smoothstep(OUTLINE_SILHOUETTE_START, OUTLINE_SILHOUETTE_END, silhouette),
        smoothstep(OUTLINE_CREASE_START, OUTLINE_CREASE_END, crease) * OUTLINE_CREASE_WEIGHT);

    return ink * fade * u_outlineStrength;
}

vec3 applyOutline(vec3 color, float outline) {
    return mix(color, color * OUTLINE_INK_SHADE, outline);
}

#endif
