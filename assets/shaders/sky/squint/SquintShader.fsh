#version 330 core

in  vec3 v_dir;
in  vec2 v_screenPos;
out vec4 fragColor;

uniform float u_squint;
uniform vec2  u_sunScreen;

uniform sampler2D u_sceneDepth;
uniform sampler2D u_cloudColor;

/*
 * Narrowed eyes over the whole screen while looking into the sun. Two lids
 * close in from the top and bottom by the squint amount, each edge bowed
 * like an eye's opening — widest at the centre and narrowing toward the
 * corners — and softened by a blur of lashes that widens as the eyes
 * close. The lids glow faintly warm, as eyelids do against the sun, and the
 * open middle dims a little with them. Taps across the sun's disc on screen
 * measure how much of it the scene and the clouds leave in view, and the
 * squint eases open as the sun is hidden. Written premultiplied for the
 * composite.
 */

const float SQUINT_LID_CLOSURE   = 0.62;
const float SQUINT_LID_BOW       = 0.22;
const float SQUINT_LASH_SOFTNESS = 0.16;
const float SQUINT_LASH_GROWTH   = 0.12;
const float SQUINT_VIEW_DIMMING  = 0.15;
const vec3  SQUINT_LID_COLOR     = vec3(0.10, 0.035, 0.02);
const float SQUINT_SUN_RADIUS    = 0.025;
const int   SQUINT_SUN_TAPS      = 8;
const float SQUINT_SUN_HIDDEN    = 0.2;
const float SQUINT_SUN_SHOWN     = 0.8;
const float SQUINT_TAU           = 6.28318530718;

// How much of the sun at one point of the screen reaches the eye: none where
// the scene stands in front of the sky, less where clouds cover it.
float resolveSunTap(vec2 uv) {
    uv = clamp(uv, vec2(0.0), vec2(1.0));

    float sky = step(1.0, texture(u_sceneDepth, uv).r);

    return sky * (1.0 - clamp(texture(u_cloudColor, uv).a, 0.0, 1.0));
}

// How much of the sun's disc is left in view, eased so a mostly hidden sun
// brings no squint and a mostly visible one brings it all.
float resolveSunVisibility() {
    float aspect  = float(textureSize(u_sceneDepth, 0).y) / float(textureSize(u_sceneDepth, 0).x);
    float visible = resolveSunTap(u_sunScreen);

    for (int i = 0; i < SQUINT_SUN_TAPS; i++) {
        float angle = SQUINT_TAU * float(i) / float(SQUINT_SUN_TAPS);
        vec2  tap   = vec2(cos(angle) * aspect, sin(angle)) * SQUINT_SUN_RADIUS;

        visible += resolveSunTap(u_sunScreen + tap);
    }

    return smoothstep(SQUINT_SUN_HIDDEN, SQUINT_SUN_SHOWN, visible / float(SQUINT_SUN_TAPS + 1));
}

// How much of one lid covers a point, given how far the point lies toward
// that lid and across the screen, both in half screens from the centre.
float resolveLid(float towardLid, float across, float squint) {
    float softness = SQUINT_LASH_SOFTNESS + SQUINT_LASH_GROWTH * squint;
    float edge     = 1.0 + softness - squint * (SQUINT_LID_CLOSURE + SQUINT_LID_BOW * across * across);

    return smoothstep(edge - softness, edge, towardLid);
}

void main() {
    float squint = clamp(u_squint, 0.0, 1.0) * resolveSunVisibility();
    float lids   = max(
        resolveLid(v_screenPos.y, v_screenPos.x, squint),
        resolveLid(-v_screenPos.y, v_screenPos.x, squint));
    float shade  = clamp(lids + squint * SQUINT_VIEW_DIMMING * (1.0 - lids), 0.0, 1.0);

    fragColor = vec4(SQUINT_LID_COLOR * shade, shade);
}