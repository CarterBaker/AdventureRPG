#version 330 core

in vec2 vUVOrigin;
in vec3 vModelPosition;
in vec3 vViewPosition;
flat in int vFace;

#include "items/includes/ItemsStandard.glsl"
#include "items/includes/BlockRepeat.glsl"

uniform sampler2DArray u_textureArray;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

/*
 * A vehicle's faces are merged across block boundaries, so a face carries only
 * the corner of its part's texture and the texture repeats once per block of
 * the model grid, read from the model position the same way a single-block
 * item lays its texels out on each face. Writes the same G-buffer as worn
 * gear, so the deferred lighting pass lights both identically.
 */
const float VEHICLE_SPECULAR = 0.18;

void main() {
    vec2 uv     = resolveRepeatedUV(vUVOrigin, vModelPosition, vFace, u_uvPerBlock);
    vec4 albedo = texture(u_textureArray, vec3(uv, float(u_layer_albedo)));

    if (albedo.a < 0.01)
    discard;

    vec3 normalView = normalize(cross(dFdx(vViewPosition), dFdy(vViewPosition)));

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(1.0, VEHICLE_SPECULAR, 1.0, 1.0);
}
