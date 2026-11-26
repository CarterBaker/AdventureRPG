#version 330 core
layout (location = 0) in vec3 aPos;      // x along the ribbon from 0 to 1, y across it from -1 to 1, z unused
layout (location = 1) in vec4 aInstance0; // start of the path (x and z from the player's chunk), progress 0 to 1
layout (location = 2) in vec4 aInstance1; // heading x, heading z, straight length, loop radius
layout (location = 3) in vec4 aInstance2; // loop's share of the way along, wave, seed, opacity

#include "particles/includes/ParticleScene.glsl"

out float vAcross;
out float vOpacity;
out vec3  vWorldPos;
out vec4  vClip;

/*
 * One wind line, a ribbon bent along a cartoon wind's path: straight downwind with a gentle wave, curling once up and
 * over through a loop that leans forward as it turns, then straight on. Only the stretch between its tail and its head
 * is laid: the head runs out along the path first, inking the line in, and the tail follows it, erasing the line the
 * way it came, so the ribbon's vertices always cover what shows. The ribbon faces the camera across its path, tapers
 * to a point at each end, and is never thinner than a pixel, its opacity thinning instead as it falls away.
 */

const float WIND_LINE_TWO_PI       = 6.28318531;
const float WIND_LINE_DRAW_END     = 0.55;
const float WIND_LINE_ERASE_START  = 0.4;
const float WIND_LINE_LOOP_LEAN    = 0.35;
const float WIND_LINE_WAVE_RATE    = 0.9;
const float WIND_LINE_SIDE_RATE    = 0.45;
const float WIND_LINE_SIDE_SHARE   = 0.6;
const float WIND_LINE_WIDTH        = 0.045;
const float WIND_LINE_MIN_PIXELS   = 1.4;
const float WIND_LINE_TAPER        = 0.6;
const float WIND_LINE_TANGENT_STEP = 0.05;
const float WIND_LINE_NEAR_START   = 1.5;
const float WIND_LINE_NEAR_END     = 4.0;
const float WIND_LINE_FAR_START    = 30.0;
const float WIND_LINE_FAR_END      = 40.0;

// A point of the path a distance along it, in the line's own frame: x downwind, y up, z to its side
vec3 resolveWindPath(float along, float straight, float loopAt, float radius, float wave, float seed) {

    float loopStart  = straight * loopAt;
    float loopLength = WIND_LINE_TWO_PI * radius;
    float phase      = seed * WIND_LINE_TWO_PI;
    float lift       = sin(along * WIND_LINE_WAVE_RATE + phase) * wave;
    float side       = sin(along * WIND_LINE_SIDE_RATE + phase * 2.0) * wave * WIND_LINE_SIDE_SHARE;

    if (along < loopStart)
    return vec3(along, lift, side);

    if (along < loopStart + loopLength) {
        float turn = (along - loopStart) / radius;
        return vec3(
            loopStart + radius * sin(turn) + WIND_LINE_LOOP_LEAN * (along - loopStart),
            radius * (1.0 - cos(turn)) + lift,
            side);
    }

    return vec3(along - loopLength + WIND_LINE_LOOP_LEAN * loopLength, lift, side);
}

void main() {
    float progress = aInstance0.w;
    float straight = aInstance1.z;
    float radius   = aInstance1.w;
    float loopAt   = aInstance2.x;
    float wave     = aInstance2.y;
    float seed     = aInstance2.z;
    float total    = straight + WIND_LINE_TWO_PI * radius;

    float head  = smoothstep(0.0, WIND_LINE_DRAW_END, progress);
    float tail  = smoothstep(WIND_LINE_ERASE_START, 1.0, progress);
    float along = mix(tail, head, aPos.x) * total;

    vec3 forward = vec3(aInstance1.x, 0.0, aInstance1.y);
    vec3 up      = vec3(0.0, 1.0, 0.0);
    vec3 side    = vec3(-forward.z, 0.0, forward.x);
    mat3 frame   = mat3(forward, up, side);

    vec3 point   = aInstance0.xyz + frame * resolveWindPath(along, straight, loopAt, radius, wave, seed);
    vec3 ahead   = frame * resolveWindPath(along + WIND_LINE_TANGENT_STEP, straight, loopAt, radius, wave, seed);
    vec3 behind  = frame * resolveWindPath(along - WIND_LINE_TANGENT_STEP, straight, loopAt, radius, wave, seed);
    vec3 tangent = normalize(ahead - behind);
    vec3 toEye   = u_cameraPosition - point;
    float distance = length(toEye);
    vec3 across  = cross(tangent, toEye / max(distance, 0.0001));

    float acrossLength = length(across);
    across = acrossLength > 0.0001 ? across / acrossLength : side;

    float width = max(WIND_LINE_WIDTH, resolveParticlePixel(distance) * WIND_LINE_MIN_PIXELS);
    float taper = pow(max(sin(aPos.x * WIND_LINE_TWO_PI * 0.5), 0.0), WIND_LINE_TAPER);

    vWorldPos = point + across * (aPos.y * width * taper);
    vAcross   = aPos.y;
    vOpacity  = aInstance2.w * (WIND_LINE_WIDTH / width)
        * smoothstep(WIND_LINE_NEAR_START, WIND_LINE_NEAR_END, distance)
        * (1.0 - smoothstep(WIND_LINE_FAR_START, WIND_LINE_FAR_END, distance));
    vClip     = u_viewProjection * vec4(vWorldPos, 1.0);

    gl_Position = vClip;
}
