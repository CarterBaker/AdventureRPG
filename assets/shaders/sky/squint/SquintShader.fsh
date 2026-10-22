#version 330 core

in  vec3 v_dir;
in  vec2 v_screenPos;
out vec4 fragColor;

uniform float u_squint;
uniform vec2  u_sunScreen;

uniform sampler2D u_sceneColor;

#include "includes/NoiseUtility.glsl"
#include "sky/util/SkySunVisibility.glsl"

/*
 * Narrowed eyes over the whole screen while looking into the sun. Two lids
 * close in from the top and bottom by the squint amount, each edge bowed
 * like an eye's opening — widest at the centre and narrowing toward the
 * corners — and softened by a blur of lashes that widens as the eyes
 * close. The lids glow faintly warm, as eyelids do against the sun, and the
 * open middle dims a little with them. Between the lids the finished frame
 * is blurred, the clear part shrinking toward the very middle and the blur
 * widening as the eyes close, until at a full squint only the lids' solid
 * color, a blurred ring and a small clear centre remain. The squint eases
 * open as the scene or the clouds hide the sun's disc. Written premultiplied
 * for the composite.
 */

const float SQUINT_LID_CLOSURE   = 0.62;
const float SQUINT_LID_BOW       = 0.22;
const float SQUINT_LASH_SOFTNESS = 0.16;
const float SQUINT_LASH_GROWTH   = 0.12;
const float SQUINT_VIEW_DIMMING  = 0.15;
const vec3  SQUINT_LID_COLOR     = vec3(0.10, 0.035, 0.02);
const float SQUINT_TAU           = 6.28318530718;
const float SQUINT_CLEAR_OPEN    = 1.6;
const float SQUINT_CLEAR_CLOSED  = 0.12;
const float SQUINT_CLEAR_FALLOFF = 0.45;
const float SQUINT_BLUR_RADIUS   = 0.035;
const int   SQUINT_BLUR_TAPS     = 24;
const float SQUINT_GOLDEN_ANGLE  = 2.39996323;

// How much of one lid covers a point, given how far the point lies toward
// that lid and across the screen, both in half screens from the centre.
float resolveLid(float towardLid, float across, float squint) {
    float softness = SQUINT_LASH_SOFTNESS + SQUINT_LASH_GROWTH * squint;
    float edge     = 1.0 + softness - squint * (SQUINT_LID_CLOSURE + SQUINT_LID_BOW * across * across);

    return smoothstep(edge - softness, edge, towardLid);
}

// How blurred a point between the lids is, from 0 inside the clear middle
// to 1 once the point lies well outside it. The clear middle shrinks from
// beyond the screen's edges toward the very centre as the eyes close.
float resolveBlurAmount(vec2 screenPos, float aspect, float squint) {
    float clear    = mix(SQUINT_CLEAR_OPEN, SQUINT_CLEAR_CLOSED, squint);
    float distance = length(vec2(screenPos.x / aspect, screenPos.y));

    return smoothstep(clear, clear + SQUINT_CLEAR_FALLOFF, distance) * squint;
}

// The finished frame gathered over a golden-angle disc, turned per pixel by
// a fine dither so the taps blend into a smooth blur instead of copies.
vec3 sampleBlurredScene(vec2 uv, float aspect, float amount) {
    vec2  scale = vec2(aspect, 1.0) * SQUINT_BLUR_RADIUS * amount;
    float turn  = interleavedGradientNoise(gl_FragCoord.xy) * SQUINT_TAU;
    vec3  color = vec3(0.0);

    for (int i = 0; i < SQUINT_BLUR_TAPS; i++) {
        float radius = sqrt((float(i) + 0.5) / float(SQUINT_BLUR_TAPS));
        float angle  = float(i) * SQUINT_GOLDEN_ANGLE + turn;

        color += texture(u_sceneColor, uv + vec2(cos(angle), sin(angle)) * radius * scale).rgb;
    }

    return color / float(SQUINT_BLUR_TAPS);
}

void main() {
    float aspect = float(textureSize(u_sceneColor, 0).y) / float(textureSize(u_sceneColor, 0).x);
    float squint = clamp(u_squint, 0.0, 1.0) * resolveSunVisibility(u_sunScreen);
    float lids   = max(
        resolveLid(v_screenPos.y, v_screenPos.x, squint),
        resolveLid(-v_screenPos.y, v_screenPos.x, squint));
    float shade  = clamp(lids + squint * SQUINT_VIEW_DIMMING * (1.0 - lids), 0.0, 1.0);
    float blur   = lids < 1.0 ? resolveBlurAmount(v_screenPos, aspect, squint) : 0.0;
    vec3  scene  = vec3(0.0);

    if (blur > 0.0)
    scene = sampleBlurredScene(v_screenPos * 0.5 + 0.5, aspect, blur) * blur;

    fragColor = vec4(SQUINT_LID_COLOR * shade + scene * (1.0 - shade), shade + blur * (1.0 - shade));
}