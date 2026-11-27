#version 330 core

in vec2 vUV;
flat in int vTile;
in vec3 vViewPosition;

#include "items/includes/ItemSurface.glsl"
#include "includes/GBufferSurface.glsl"


layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;
layout(location = 3) out vec4 gSurface;

/*
 * Worn painted gear writes the same G-buffer as the character wearing it, so
 * the deferred lighting pass lights both identically. The anchor transform
 * stretches the item non-uniformly, so the facet normal is rebuilt from
 * screen-space derivatives rather than carried through the matrix.
 */
const float EQUIPMENT_SPECULAR = 0.18;

void main() {
    ItemSurface surface = sampleItemSurface(vTile, vUV, EQUIPMENT_SPECULAR);
    vec4        albedo  = surface.albedo;

    if (albedo.a < 0.01)
    discard;

    vec3 normalView = normalize(cross(dFdx(vViewPosition), dFdy(vViewPosition)));

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(1.0, surface.specular, surface.ao, 1.0);
    gSurface  = encodeGBufferSurface(surface.metallic, surface.emissive);
}
