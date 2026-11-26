#version 330 core

in vec2  vUV;
in float vFade;
in vec3  vWorldPos;
in vec4  vClip;

#include "particles/includes/ParticleScene.glsl"

uniform sampler2D u_bubbleTexture;

out vec4 fragColor;

// A bubble drawn from its picture, a faint middle under a bright rim and highlight, lit by the sky that reaches down
// through the water, hidden behind the scene and the surface and faded where it meets them, written premultiplied.

const float BUBBLE_SOFT = 0.1;

void main() {
    vec4  picture  = texture(u_bubbleTexture, vUV);
    float distance = length(vWorldPos - u_cameraPosition);
    float alpha    = picture.a * vFade
        * resolveParticleVisibility(resolveParticleScreen(vClip), distance, BUBBLE_SOFT);

    if (alpha <= 0.0)
    discard;

    fragColor = vec4(picture.rgb * resolveParticleLight() * alpha, alpha);
}
