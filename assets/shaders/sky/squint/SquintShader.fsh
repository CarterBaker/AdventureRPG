#version 330 core

in  vec3 v_dir;
in  vec2 v_screenPos;
out vec4 fragColor;

uniform float u_squint;

/*
 * Narrowed eyes over the whole screen while looking into the sun. Two lids
 * close in from the top and bottom by the squint amount, each edge bowed
 * like an eye's opening — nearest the middle at the centre and falling away
 * toward the sides — and softened by a blur of lashes that widens as the
 * eyes close. The lids glow faintly warm, as eyelids do against the sun,
 * and the open middle dims a little with them. Written premultiplied for
 * the composite.
 */

const float SQUINT_LID_CLOSURE   = 0.62;
const float SQUINT_LID_BOW       = 0.22;
const float SQUINT_LASH_SOFTNESS = 0.16;
const float SQUINT_LASH_GROWTH   = 0.12;
const float SQUINT_VIEW_DIMMING  = 0.15;
const vec3  SQUINT_LID_COLOR     = vec3(0.10, 0.035, 0.02);

// How much of one lid covers a point, given how far the point lies toward
// that lid and across the screen, both in half screens from the centre.
float resolveLid(float towardLid, float across, float squint) {
    float softness = SQUINT_LASH_SOFTNESS + SQUINT_LASH_GROWTH * squint;
    float edge     = 1.0 + softness - squint * SQUINT_LID_CLOSURE + SQUINT_LID_BOW * across * across;

    return smoothstep(edge - softness, edge, towardLid);
}

void main() {
    float squint = clamp(u_squint, 0.0, 1.0);
    float lids   = max(
        resolveLid(v_screenPos.y, v_screenPos.x, squint),
        resolveLid(-v_screenPos.y, v_screenPos.x, squint));
    float shade  = clamp(lids + squint * SQUINT_VIEW_DIMMING * (1.0 - lids), 0.0, 1.0);

    fragColor = vec4(SQUINT_LID_COLOR * shade, shade);
}