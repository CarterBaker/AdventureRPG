#version 330 core

flat in int vTile;
in vec3 vModelPosition;
in vec3 vViewPosition;
flat in int vFace;

#include "items/includes/BlockAlbedo.glsl"


layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

/*
 * Worn gear writes the same G-buffer as the character wearing it, so the
 * deferred lighting pass lights both identically. The anchor transform
 * stretches the item non-uniformly, so the flat facet normal is rebuilt
 * from screen-space derivatives rather than carried through the matrix.
 * Faces merge across block boundaries, so the tile repeats once per block.
 */
const float EQUIPMENT_SPECULAR = 0.18;

void main() {
    vec4 albedo = sampleBlockAlbedo(vTile, vModelPosition, vFace);

    if (albedo.a < 0.01)
    discard;

    vec3 normalView = normalize(cross(dFdx(vViewPosition), dFdy(vViewPosition)));

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(1.0, EQUIPMENT_SPECULAR, 1.0, 1.0);
}
