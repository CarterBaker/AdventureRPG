#version 330 core

in float vAcross;
in float vOpacity;
in vec3  vWorldPos;
in vec4  vClip;

#include "particles/includes/ParticleScene.glsl"

out vec4 fragColor;

// A wind line's ribbon: solid white down its middle, its edges eased off so a line a pixel or two wide stays smooth,
// lit by the sky, hidden behind the scene and faded where it meets it, written premultiplied.

const float WIND_LINE_EDGE  = 0.45;
const float WIND_LINE_SOFT  = 0.35;
const vec3  WIND_LINE_COLOR = vec3(1.0);

void main() {
    float distance = length(vWorldPos - u_cameraPosition);
    float body     = 1.0 - smoothstep(WIND_LINE_EDGE, 1.0, abs(vAcross));
    float alpha    = body * vOpacity
        * resolveParticleVisibility(resolveParticleScreen(vClip), distance, WIND_LINE_SOFT);

    if (alpha <= 0.0)
    discard;

    fragColor = vec4(WIND_LINE_COLOR * resolveParticleLight() * alpha, alpha);
}
