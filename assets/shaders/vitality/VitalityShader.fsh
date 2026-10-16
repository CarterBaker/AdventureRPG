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
 * draws bloodshot veins: thin, wandering capillaries reaching in from the
 * edges, finer branches beside the trunks, reaching further and showing
 * stronger the fresher the bleeding is. The glow rises in from the edges in
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
const float VEIN_TRUNKS         = 11.0;
const float VEIN_BRANCHES       = 29.0;
const float VEIN_TRUNK_WIDTH    = 0.07;
const float VEIN_BRANCH_WIDTH   = 0.045;
const float VEIN_WANDER         = 2.6;
const float VEIN_WANDER_SCALE   = 4.0;
const float VEIN_BRANCH_SEED    = 1.7;
const float VEIN_BRANCH_SHARE   = 0.7;
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

const float PI                  = 3.14159265359;
const float HASH_SCALE          = 43758.5453123;
const vec2  HASH_STEP           = vec2(127.1, 311.7);

// A smooth hash noise over the plane, from 0 to 1
float hash(vec2 p) {
    return fract(sin(dot(p, HASH_STEP)) * HASH_SCALE);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f    = fract(p);
    vec2 u    = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(hash(cell), hash(cell + vec2(1.0, 0.0)), u.x),
        mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0, 1.0)), u.x),
        u.y);
}

// One family of capillaries: count lines running in from the edge, wandering as they come by noise over the
// screen, so they bend without a seam, and thinnest at their tips
float resolveVeins(vec2 screen, float angle, float radius, float count, float width, float seed) {
    float wander = (noise(screen * VEIN_WANDER_SCALE + seed) - 0.5) * VEIN_WANDER;
    float line   = abs(sin(angle * count / 2.0 + wander + seed));
    float taper  = width * clamp(radius, 0.0, 1.0);

    return 1.0 - smoothstep(0.0, taper, line);
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
    float angle    = atan(v_screenPos.y, v_screenPos.x) + PI;

    // Glow
    float glowEdge = smoothstep(GLOW_INNER, GLOW_OUTER, radius) * glow;
    float flash    = pow(glow, GLOW_FLASH_POWER) * GLOW_FLASH_STRENGTH;
    float glowA    = clamp(glowEdge + flash, 0.0, 1.0);
    vec4  layer    = vec4(u_glowColor * glowA, glowA);

    // Veins
    float throb    = 1.0 + sin(time * VEIN_THROB_RATE) * VEIN_THROB_DEPTH;
    float reach    = mix(VEIN_REACH_FAINT, VEIN_REACH_FRESH, bleed);
    float vesselIn = smoothstep(reach, reach + VEIN_FADE, radius);
    float veins    = max(
        resolveVeins(v_screenPos, angle, radius, VEIN_TRUNKS, VEIN_TRUNK_WIDTH * throb, 0.0),
        resolveVeins(v_screenPos, angle, radius, VEIN_BRANCHES, VEIN_BRANCH_WIDTH * throb, VEIN_BRANCH_SEED)
            * VEIN_BRANCH_SHARE);
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
