#version 330 core
layout (location = 0) in vec3 aPos;      // a corner of the card, -1 to 1 across and up, z unused
layout (location = 1) in vec4 aInstance0; // place (x and z from the player's chunk), age in seconds
layout (location = 2) in vec4 aInstance1; // radius in blocks, seed, fade 0 to 1, unused

#include "particles/includes/ParticleScene.glsl"

out vec2  vUV;
out float vFade;
out vec3  vWorldPos;
out vec4  vClip;

/*
 * One bubble, a card square to the view as wide as the bubble, wobbling side to side as it rises and wobbling its
 * shape, squashed one way then the other, as a cartoon bubble does.
 */

const float BUBBLE_TWO_PI       = 6.28318531;
const float BUBBLE_WOBBLE_RATE  = 3.1;
const float BUBBLE_WOBBLE       = 0.045;
const float BUBBLE_SQUASH_RATE  = 7.3;
const float BUBBLE_SQUASH       = 0.09;

void main() {
    float age    = aInstance0.w;
    float radius = aInstance1.x;
    float phase  = aInstance1.y * BUBBLE_TWO_PI;

    vec3 wobble = vec3(sin(age * BUBBLE_WOBBLE_RATE + phase), 0.0, cos(age * BUBBLE_WOBBLE_RATE * 0.8 + phase))
        * BUBBLE_WOBBLE;
    vec3  center = aInstance0.xyz + wobble;
    vec3  right  = vec3(u_view[0][0], u_view[1][0], u_view[2][0]);
    vec3  up     = vec3(u_view[0][1], u_view[1][1], u_view[2][1]);
    float squash = sin(age * BUBBLE_SQUASH_RATE + phase) * BUBBLE_SQUASH;

    vWorldPos = center + (right * (aPos.x * (1.0 + squash)) + up * (aPos.y * (1.0 - squash))) * radius;
    vUV       = aPos.xy * 0.5 + 0.5;
    vFade     = aInstance1.z;
    vClip     = u_viewProjection * vec4(vWorldPos, 1.0);

    gl_Position = vClip;
}
