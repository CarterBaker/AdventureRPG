#version 330 core

in vec3 vNormalView;
in vec3 vViewPosition;
in vec2 vUV;

#include "items/includes/ItemsStandard.glsl"

uniform sampler2DArray u_textureArray;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

/*
 * World items write the same G-buffer as the terrain they rest on, so the
 * deferred lighting pass lights, shades and fogs them identically. Items draw
 * without culling and a wall is one quad seen from both sides, so the normal
 * is turned to face the viewer; a cube face is only ever seen from the side
 * it already faces.
 */
const float ITEM_SPECULAR = 0.18;

void main() {
    vec4 albedo = texture(u_textureArray, vec3(vUV, float(u_layer_albedo)));

    if (albedo.a < 0.01)
    discard;

    vec3 normalView = normalize(vNormalView);

    if (dot(normalView, vViewPosition) > 0.0)
    normalView = -normalView;

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(1.0, ITEM_SPECULAR, 1.0, 1.0);
}
