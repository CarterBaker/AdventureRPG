#version 330 core

in vec2  vUV;
in vec3  vColor;
in float vShade;
in float vFade;
in vec3  vWorldPos;
in vec4  vClip;

#include "particles/includes/ParticleScene.glsl"

uniform sampler2D u_leafTexture;

out vec4 fragColor;

// A leaf drawn from its picture, tinted with the leaf color of the tree it fell from and lit by the sky, the picture
// read with four rotated-grid taps over the pixel so a far leaf does not shimmer; hidden behind the scene, its soft
// reach a sliver so a leaf lying on the ground still shows, written premultiplied.

const float LEAF_SOFT = 0.01;

vec4 sampleLeaf(vec2 uv) {
    vec2 span = fwidth(uv);

    return 0.25 * (
        texture(u_leafTexture, uv + vec2(-0.125, -0.375) * span)
        + texture(u_leafTexture, uv + vec2( 0.375, -0.125) * span)
        + texture(u_leafTexture, uv + vec2( 0.125,  0.375) * span)
        + texture(u_leafTexture, uv + vec2(-0.375,  0.125) * span));
}

void main() {
    vec4  picture  = sampleLeaf(vUV);
    float distance = length(vWorldPos - u_cameraPosition);
    float alpha    = picture.a * vFade
        * resolveParticleVisibility(resolveParticleScreen(vClip), distance, LEAF_SOFT);

    if (alpha <= 0.0)
    discard;

    vec3 color = vColor * picture.rgb * vShade * resolveParticleLight();

    fragColor = vec4(color * alpha, alpha);
}
