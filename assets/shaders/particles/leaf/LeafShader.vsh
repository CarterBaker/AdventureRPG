#version 330 core
layout (location = 0) in vec3 aPos;      // a corner of the card, -1 to 1 across and along, z unused
layout (location = 1) in vec4 aInstance0; // place (x and z from the player's chunk), age in seconds
layout (location = 2) in vec4 aInstance1; // color, seed
layout (location = 3) in vec4 aInstance2; // settled 0 to 1, fade 0 to 1, size in blocks, spin a second

#include "particles/includes/ParticleScene.glsl"

out vec2  vUV;
out vec3  vColor;
out float vShade;
out float vFade;
out vec3  vWorldPos;
out vec4  vClip;

/*
 * One leaf, a flat card turning in the world as a falling leaf does: it rocks to and fro like a pendulum, rising a
 * little at each end of its swing, spins about the upright, and now and then flips over along its length. Once it has
 * come to rest it eases into lying flat on the ground at the turn its seed gave it. A leaf facing the sky is lit
 * fully, one standing on edge a little darker, as the cartoon shades a card.
 */

const float LEAF_TWO_PI      = 6.28318531;
const float LEAF_SWAY_RATE   = 2.1;
const float LEAF_SWAY        = 0.32;
const float LEAF_SWAY_LIFT   = 0.12;
const float LEAF_ROCK        = 0.9;
const float LEAF_FLIP_SHARE  = 0.45;
const float LEAF_SHADE_EDGE  = 0.7;

mat3 rotateLeafX(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

mat3 rotateLeafY(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat3(c, 0.0, -s, 0.0, 1.0, 0.0, s, 0.0, c);
}

mat3 rotateLeafZ(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat3(c, s, 0.0, -s, c, 0.0, 0.0, 0.0, 1.0);
}

void main() {
    float age      = aInstance0.w;
    float seed     = aInstance1.w;
    float settled  = aInstance2.x;
    float airborne = 1.0 - settled;
    float size     = aInstance2.z;
    float spin     = aInstance2.w;
    float phase    = seed * LEAF_TWO_PI;

    float sway    = sin(age * LEAF_SWAY_RATE + phase);
    vec3  rocking = vec3(cos(phase), 0.0, sin(phase));
    vec3  flutter = rocking * (sway * LEAF_SWAY) + vec3(0.0, abs(sway) * LEAF_SWAY_LIFT, 0.0);
    vec3  center  = aInstance0.xyz + flutter * airborne;

    mat3 falling = rotateLeafY(age * spin + phase)
        * rotateLeafX(age * spin * LEAF_FLIP_SHARE)
        * rotateLeafZ(sway * LEAF_ROCK);
    mat3 resting = rotateLeafY(phase);

    vec3 right   = normalize(mix(falling[0], resting[0], settled));
    vec3 along   = mix(falling[2], resting[2], settled);
    along        = normalize(along - right * dot(along, right));
    vec3 normal  = cross(along, right);

    vWorldPos = center + (right * aPos.x + along * aPos.y) * (size * 0.5);
    vUV       = aPos.xy * 0.5 + 0.5;
    vColor    = aInstance1.rgb;
    vShade    = mix(LEAF_SHADE_EDGE, 1.0, abs(normal.y));
    vFade     = aInstance2.y;
    vClip     = u_viewProjection * vec4(vWorldPos, 1.0);

    gl_Position = vClip;
}
