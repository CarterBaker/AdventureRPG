#version 330 core

in  vec3 v_dir;
in  vec2 v_screenPos;
out vec4 fragColor;

uniform vec4 u_vitality;
uniform vec3 u_glowColor;

/*
 * How near death the player stands, drawn over the whole screen. u_vitality
 * carries the darkness, the bleeding and the level-up glow, each from 0 to 1,
 * and a running time in seconds. The darkness closes in from the edges as an
 * oval that tightens toward the centre, its rim soft and drawn a little with
 * a slow heartbeat once it is deep; at 1 the whole view is black. Bleeding
 * draws bloodshot veins: crooked trunks reaching in from the edges, each
 * order of finer capillaries forking off the one before it and stopping
 * short of it, every vessel thinning to a point, the whole web reaching
 * further and showing stronger the fresher the bleeding is. The veins are
 * ridges of noise laid around the rim, stretched so they run inward, and
 * kinked by a fine warp so none of them runs smooth. The glow rises in from the edges in
 * the glow colour with a brief flash across the whole view as it starts.
 * Layers are laid glow, then veins, then darkness, each over the last, and
 * written premultiplied for the composite.
 */

const float DARK_EDGE_OPEN      = 1.75;
const float DARK_EDGE_CLOSED    = -0.15;
const float DARK_SOFTNESS       = 0.45;
const float DARK_HEART_RATE     = 4.4;
const float DARK_HEART_DEPTH    = 0.05;
const float DARK_FULL_POWER     = 4.0;
const float DARK_SHADE_GAIN     = 1.25;

const float VEIN_REACH_FAINT    = 1.35;
const float VEIN_REACH_FRESH    = 0.5;
const float VEIN_FADE           = 0.55;
const int   VEIN_ORDERS         = 4;
const float VEIN_AROUND         = 6.0;
const float VEIN_INWARD         = 1.4;
const float VEIN_ORDER_SCALE    = 2.2;
const float VEIN_ORDER_SEED     = 17.3;
const float VEIN_WIDTH          = 0.08;
const float VEIN_ORDER_WIDTH    = 0.72;
const float VEIN_ORDER_REACH    = 0.16;
const float VEIN_ORDER_SHADE    = 0.82;
const float VEIN_CORE           = 0.3;
const float VEIN_FORK_LOW       = 0.5;
const float VEIN_FORK_HIGH      = 0.85;
const float VEIN_WARP_SCALE     = 0.45;
const float VEIN_WARP           = 0.9;
const float VEIN_KINK_SCALE     = 5.0;
const float VEIN_KINK           = 0.22;
const vec3  VEIN_WARP_SEED_A    = vec3(11.0, 3.0, 7.0);
const vec3  VEIN_WARP_SEED_B    = vec3(29.0, 13.0, 5.0);
const vec3  VEIN_KINK_SEED      = vec3(41.0, 19.0, 23.0);
const float VEIN_RADIUS_EPSILON = 0.0001;
const float VEIN_TINT_START     = 0.9;
const float VEIN_TINT_END       = 1.6;
const float VEIN_THROB_RATE     = 2.2;
const float VEIN_THROB_DEPTH    = 0.12;
const float VEIN_EDGE_TINT      = 0.35;
const vec3  VEIN_COLOR          = vec3(0.42, 0.015, 0.02);

const float GLOW_INNER          = 0.25;
const float GLOW_OUTER          = 1.45;
const float GLOW_FLASH_POWER    = 4.0;
const float GLOW_FLASH_STRENGTH = 0.55;

const float HASH_SCALE          = 43758.5453123;
const vec3  HASH_STEP_3D        = vec3(127.1, 311.7, 74.7);

// A smooth hash noise through space, from 0 to 1
float hash3(vec3 p) {
    return fract(sin(dot(p, HASH_STEP_3D)) * HASH_SCALE);
}

float noise3(vec3 p) {
    vec3 cell = floor(p);
    vec3 f    = fract(p);
    vec3 u    = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(
            mix(hash3(cell), hash3(cell + vec3(1.0, 0.0, 0.0)), u.x),
            mix(hash3(cell + vec3(0.0, 1.0, 0.0)), hash3(cell + vec3(1.0, 1.0, 0.0)), u.x),
            u.y),
        mix(
            mix(hash3(cell + vec3(0.0, 0.0, 1.0)), hash3(cell + vec3(1.0, 0.0, 1.0)), u.x),
            mix(hash3(cell + vec3(0.0, 1.0, 1.0)), hash3(cell + vec3(1.0, 1.0, 1.0)), u.x),
            u.y),
        u.z);
}

// 1 along the lines where the noise crosses its middle, falling away either side
float ridge(vec3 p) {
    return 1.0 - abs(noise3(p) * 2.0 - 1.0);
}

// The web of veins: the rim laid out as a cylinder in noise, its way round the screen's edge on a circle so it
// closes without a seam and its height the distance inward, stretched so the ridges run in from the edge. Each
// order is finer than the last, forks only where its parent runs close, stops short of it, and every vessel
// tapers to nothing at its tip
float resolveVeins(vec2 screen, float radius, float reach, float throb) {
    vec2 around = screen / max(radius, VEIN_RADIUS_EPSILON);
    vec3 p      = vec3(around * VEIN_AROUND, radius * VEIN_INWARD);

    p.xy += (vec2(noise3(p * VEIN_WARP_SCALE + VEIN_WARP_SEED_A), noise3(p * VEIN_WARP_SCALE + VEIN_WARP_SEED_B))
        - 0.5) * VEIN_WARP;
    p.xy += (noise3(p * VEIN_KINK_SCALE + VEIN_KINK_SEED) - 0.5) * VEIN_KINK;

    float veins  = 0.0;
    float parent = 1.0;
    float scale  = 1.0;
    float width  = VEIN_WIDTH * throb;
    float shade  = 1.0;

    for (int order = 0; order < VEIN_ORDERS; order++) {
        float line  = ridge(p * scale + float(order) * VEIN_ORDER_SEED);
        float tip   = reach + float(order) * VEIN_ORDER_REACH;
        float taper = width * clamp((radius - tip) / VEIN_FADE, 0.0, 1.0);
        float fork  = order == 0 ? 1.0 : smoothstep(VEIN_FORK_LOW, VEIN_FORK_HIGH, parent);
        float body  = smoothstep(1.0 - taper, 1.0 - taper * VEIN_CORE, line);

        veins  = max(veins, body * fork * shade);
        parent = line * fork;
        scale *= VEIN_ORDER_SCALE;
        width *= VEIN_ORDER_WIDTH;
        shade *= VEIN_ORDER_SHADE;
    }

    return veins;
}

// Lays one premultiplied layer over another
vec4 over(vec4 top, vec4 under) {
    return top + under * (1.0 - top.a);
}

void main() {
    float darkness = clamp(u_vitality.x, 0.0, 1.0);
    float bleed    = clamp(u_vitality.y, 0.0, 1.0);
    float glow     = clamp(u_vitality.z, 0.0, 1.0);
    float time     = u_vitality.w;
    float radius   = length(v_screenPos);

    // Glow
    float glowEdge = smoothstep(GLOW_INNER, GLOW_OUTER, radius) * glow;
    float flash    = pow(glow, GLOW_FLASH_POWER) * GLOW_FLASH_STRENGTH;
    float glowA    = clamp(glowEdge + flash, 0.0, 1.0);
    vec4  layer    = vec4(u_glowColor * glowA, glowA);

    // Veins
    float throb    = 1.0 + sin(time * VEIN_THROB_RATE) * VEIN_THROB_DEPTH;
    float reach    = mix(VEIN_REACH_FAINT, VEIN_REACH_FRESH, bleed);
    float vesselIn = smoothstep(reach, reach + VEIN_FADE, radius);
    float veins    = resolveVeins(v_screenPos, radius, reach, throb);
    float tint     = VEIN_EDGE_TINT * smoothstep(VEIN_TINT_START, VEIN_TINT_END, radius);
    float veinA    = clamp((veins + tint) * vesselIn * bleed, 0.0, 1.0);
    layer = over(vec4(VEIN_COLOR * veinA, veinA), layer);

    // Darkness
    float heart    = sin(time * DARK_HEART_RATE) * DARK_HEART_DEPTH * darkness * darkness;
    float edge     = mix(DARK_EDGE_OPEN, DARK_EDGE_CLOSED, darkness) + heart;
    float shade    = smoothstep(edge - DARK_SOFTNESS, edge + DARK_SOFTNESS, radius);
    float darkA    = clamp(max(shade * darkness * DARK_SHADE_GAIN, pow(darkness, DARK_FULL_POWER)), 0.0, 1.0);
    layer = over(vec4(0.0, 0.0, 0.0, darkA), layer);

    fragColor = layer;
}
