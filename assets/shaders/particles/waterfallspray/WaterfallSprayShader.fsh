#version 330 core

in vec2  vUV;
in float vKind;
in float vSeed;
in float vFade;
in vec3  vWorldPos;
in vec4  vClip;

#include "particles/includes/ParticleScene.glsl"

out vec4 fragColor;

// A particle of a fall's spray drawn as a cartoon shape with no picture, each kind from its row of the tables: a
// splash a solid droplet with a hard edge and a bluer rim, foam a lumpy patch thinner in its middle, mist a faint soft
// puff. All are lit by the sky, hidden behind the scene and the surface and faded where they meet them, written
// premultiplied.

const float SPRAY_TWO_PI         = 6.28318531;
const float SPRAY_LUMP_LOBES     = 5.0;
const float SPRAY_HOLLOW_START   = 0.12;
const float SPRAY_HOLLOW_END     = 0.38;
const vec3  SPRAY_COLOR[3]       = vec3[](vec3(0.88, 0.96, 1.00), vec3(0.97, 0.99, 1.00), vec3(0.93, 0.97, 1.00));
const vec3  SPRAY_RIM_COLOR      = vec3(0.55, 0.80, 0.97);
const float SPRAY_RIM[3]         = float[](0.45, 0.0, 0.0);
const float SPRAY_OPACITY[3]     = float[](0.95, 0.9, 0.3);
const float SPRAY_EDGE_START[3]  = float[](0.62, 0.58, 0.0);
const float SPRAY_EDGE_END[3]    = float[](0.8, 0.74, 1.0);
const float SPRAY_LUMP[3]        = float[](0.0, 0.22, 0.12);
const float SPRAY_HOLLOW[3]      = float[](0.0, 0.35, 0.0);
const float SPRAY_SOFT[3]        = float[](0.05, 0.03, 0.8);

void main() {
    int   kind     = int(vKind + 0.5);
    float angle    = atan(vUV.y, vUV.x);
    float radius   = length(vUV) * (1.0 + SPRAY_LUMP[kind] * sin(angle * SPRAY_LUMP_LOBES + vSeed * SPRAY_TWO_PI));
    float shape    = 1.0 - smoothstep(SPRAY_EDGE_START[kind], SPRAY_EDGE_END[kind], radius);
    float hollow   = 1.0 - SPRAY_HOLLOW[kind] * (1.0 - smoothstep(SPRAY_HOLLOW_START, SPRAY_HOLLOW_END, radius));
    float distance = length(vWorldPos - u_cameraPosition);
    float alpha    = shape * hollow * SPRAY_OPACITY[kind] * vFade
        * resolveParticleVisibility(resolveParticleScreen(vClip), distance, SPRAY_SOFT[kind]);

    if (alpha <= 0.0)
    discard;

    float rim   = smoothstep(SPRAY_HOLLOW_START, SPRAY_EDGE_END[kind], radius) * SPRAY_RIM[kind];
    vec3  color = mix(SPRAY_COLOR[kind], SPRAY_RIM_COLOR, rim) * resolveParticleLight();

    fragColor = vec4(color * alpha, alpha);
}
