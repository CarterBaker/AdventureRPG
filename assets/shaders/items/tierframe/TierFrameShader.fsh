#version 330 core

in  vec2 v_uv;
out vec4 FragColor;

uniform vec4 u_frameColor;
uniform vec4 u_frameEffects;
uniform vec2 u_frameMotion;

/*
 * The frame a gear tier draws around a slot. u_frameColor is the tier's
 * colour; u_frameEffects carries how strongly it shows its glow, pulse,
 * sweep and motes, and u_frameMotion its sheen and a running time in
 * seconds, all from 0 to 1 but the time. Every tier draws a border; the
 * glow spreads that colour inward from it, the pulse breathes the glow, the
 * sweep runs a band of light across the slot now and then, motes drift
 * around the border as sparks, and the sheen shifts the colour through the
 * whole spectrum along the border over time. The slot's middle stays clear
 * for the item drawn over it, and nothing at all is written where the frame
 * shows nothing.
 */

const float BORDER_WIDTH     = 0.045;
const float BORDER_SOFTNESS  = 0.03;
const float GLOW_WIDTH       = 0.32;
const float GLOW_STRENGTH    = 0.7;
const float PULSE_RATE       = 2.6;
const float PULSE_DEPTH      = 0.45;
const float SWEEP_PERIOD     = 3.4;
const float SWEEP_WIDTH      = 0.09;
const float SWEEP_STRENGTH   = 0.8;
const float SWEEP_SPAN       = 1.6;
const float MOTE_COUNT       = 6.0;
const float MOTE_SPEED       = 0.06;
const float MOTE_SIZE        = 0.035;
const float MOTE_STRENGTH    = 1.1;
const float SHEEN_SPEED      = 0.18;
const float SHEEN_SPREAD     = 1.5;
const float CLEAR_ALPHA      = 0.004;
const vec3  SHEEN_PHASE      = vec3(0.0, 0.33333, 0.66667);
const float TAU              = 6.28318530718;

// How far round the border a point lies, from 0 to 1, starting at the bottom left and running anticlockwise
float resolvePerimeter(vec2 uv) {
    vec2  centred = uv - 0.5;
    float angle   = atan(centred.y, centred.x);

    return fract(angle / TAU + 0.5);
}

// A point on the border at a share of the way round it
vec2 resolveBorderPoint(float share) {
    float side  = floor(share * 4.0);
    float along = fract(share * 4.0);

    if (side < 1.0) return vec2(along, 0.0);
    if (side < 2.0) return vec2(1.0, along);
    if (side < 3.0) return vec2(1.0 - along, 1.0);
    return vec2(0.0, 1.0 - along);
}

vec3 resolveSheen(float perimeter, float time) {
    return 0.5 + 0.5 * cos(TAU * (perimeter * SHEEN_SPREAD + time * SHEEN_SPEED + SHEEN_PHASE));
}

void main() {
    float glow   = u_frameEffects.x;
    float pulse  = u_frameEffects.y;
    float sweep  = u_frameEffects.z;
    float motes  = u_frameEffects.w;
    float sheen  = u_frameMotion.x;
    float time   = u_frameMotion.y;
    float edge   = min(min(v_uv.x, v_uv.y), min(1.0 - v_uv.x, 1.0 - v_uv.y));
    vec3  color  = mix(u_frameColor.rgb, resolveSheen(resolvePerimeter(v_uv), time), sheen);

    // Border
    float border = 1.0 - smoothstep(BORDER_WIDTH, BORDER_WIDTH + BORDER_SOFTNESS, edge);

    // Glow
    float breath = 1.0 - pulse * PULSE_DEPTH * (0.5 + 0.5 * sin(time * PULSE_RATE));
    float inner  = (1.0 - smoothstep(0.0, GLOW_WIDTH, edge)) * glow * GLOW_STRENGTH * breath;

    // Sweep
    float travel = fract(time / SWEEP_PERIOD) * SWEEP_SPAN * 2.0 - SWEEP_SPAN * 0.5;
    float band   = 1.0 - smoothstep(0.0, SWEEP_WIDTH, abs((v_uv.x + v_uv.y) * 0.5 - travel));
    float shine  = band * sweep * SWEEP_STRENGTH * max(border, inner);

    // Motes
    float sparks = 0.0;

    for (float i = 0.0; i < MOTE_COUNT; i += 1.0) {
        vec2 mote = resolveBorderPoint(fract(time * MOTE_SPEED + i / MOTE_COUNT));
        sparks   += 1.0 - smoothstep(0.0, MOTE_SIZE, distance(v_uv, mote));
    }

    sparks *= motes * MOTE_STRENGTH;

    float alpha = clamp(border + inner + shine + sparks, 0.0, 1.0);

    if (alpha < CLEAR_ALPHA)
        discard;

    vec3 lit = color + vec3(shine + sparks);

    FragColor = vec4(min(lit, vec3(1.0)), alpha * u_frameColor.a);
}
