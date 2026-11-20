#ifndef TREE_VIEWER_LIGHT_GLSL
#define TREE_VIEWER_LIGHT_GLSL

#include "includes/CameraData.glsl"

// An editor viewport has no deferred lighting pass, so a tree shaded exactly as the game's G-buffer receives it is lit
// here by one fixed light over an ambient floor, its own occlusion darkening it as the game's light does: the normal
// is the view-space one the G-buffer would hold, the occlusion its material's.

uniform vec3  u_lightDirection;
uniform float u_ambient;

// The way toward the light, in view space
vec3 resolveTreeViewerLightView() {
    return normalize(mat3(u_view) * -u_lightDirection);
}

float resolveTreeViewerLight(vec3 viewNormal, float occlusion) {
    vec3  lightView = resolveTreeViewerLightView();
    float diffuse   = max(dot(normalize(viewNormal), lightView), 0.0);

    return (u_ambient + (1.0 - u_ambient) * diffuse) * occlusion;
}

#endif
