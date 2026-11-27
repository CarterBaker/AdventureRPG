#version 330 core

in vec3 vNormalView;
in vec3 vViewPosition;
flat in int vTile;
in vec3 vModelPosition;
flat in int vFace;

#include "items/includes/BlockAlbedo.glsl"
#include "includes/GBufferSurface.glsl"


layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;
layout(location = 3) out vec4 gSurface;

/*
 * World items write the same G-buffer as the terrain they rest on, so the
 * deferred lighting pass lights, shades and fogs them identically. Items draw
 * without culling and a wall is one quad seen from both sides, so the normal
 * is turned to face the viewer; a cube face is only ever seen from the side
 * it already faces. Faces merge across block boundaries, so the texture repeats
 * once per block of the item's own model grid.
 */
const float ITEM_SPECULAR = 0.18;

void main() {
    ItemSurface surface = sampleBlockSurface(vTile, vModelPosition, vFace, ITEM_SPECULAR);
    vec4        albedo  = surface.albedo;

    if (albedo.a < 0.01)
    discard;

    vec3 normalView = normalize(vNormalView);

    if (dot(normalView, vViewPosition) > 0.0)
    normalView = -normalView;

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(1.0, surface.specular, surface.ao, 1.0);
    gSurface  = encodeGBufferSurface(surface.metallic, surface.emissive);
}
