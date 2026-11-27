#version 330 core
layout (location = 0) in vec3 aPos;      // a corner of the card, -1 to 1 across and up, z unused
layout (location = 1) in vec4 aInstance0; // place (x and z from the player's chunk), share of its life gone 0 to 1
layout (location = 2) in vec4 aInstance1; // size in blocks, kind (splash, foam, mist), seed, fade 0 to 1

#include "particles/includes/ParticleScene.glsl"

out vec2  vUV;
out float vKind;
out float vSeed;
out float vFade;
out vec3  vWorldPos;
out vec4  vClip;

/*
 * One particle of a fall's spray on a card as wide as the particle: a splash and a puff of mist face the camera,
 * foam lies flat on the water turned by its seed. Each kind grows through its life by its row of the table, the
 * splash shrinking a little as it falls back, foam spreading as it rides away and mist swelling as it rises.
 */

const float SPRAY_TWO_PI    = 6.28318531;
const float SPRAY_FLAT[3]   = float[](0.0, 1.0, 0.0);
const float SPRAY_GROWTH[3] = float[](-0.35, 0.9, 1.3);

void main() {
    int   kind  = int(aInstance1.y + 0.5);
    float size  = aInstance1.x * (1.0 + SPRAY_GROWTH[kind] * aInstance0.w);
    float turn  = aInstance1.z * SPRAY_TWO_PI;
    float lying = SPRAY_FLAT[kind];

    vec3 right = mix(vec3(u_view[0][0], u_view[1][0], u_view[2][0]), vec3(cos(turn), 0.0, sin(turn)), lying);
    vec3 up    = mix(vec3(u_view[0][1], u_view[1][1], u_view[2][1]), vec3(-sin(turn), 0.0, cos(turn)), lying);

    vWorldPos = aInstance0.xyz + (right * aPos.x + up * aPos.y) * size;
    vUV       = aPos.xy;
    vKind     = aInstance1.y;
    vSeed     = aInstance1.z;
    vFade     = aInstance1.w;
    vClip     = u_viewProjection * vec4(vWorldPos, 1.0);

    gl_Position = vClip;
}
