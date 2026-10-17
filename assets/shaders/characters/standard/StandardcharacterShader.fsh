#version 330 core

in vec2 v_uv;
in vec3 v_viewPosition;
flat in vec3 v_tint;
flat in vec3 v_detailTint;
flat in vec4 v_faceRegion;
flat in vec4 v_eyesRegion;
flat in vec4 v_browsRegion;
flat in vec4 v_mouthRegion;

#include "characters/includes/CharactersStandard.glsl"
#include "characters/includes/CharacterFace.glsl"

uniform sampler2DArray u_texture;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

/*
 * Characters write the same G-buffer as the world surface so the deferred
 * lighting pass lights them identically. Normals are the flat face normal
 * rebuilt from screen-space derivatives — the low-poly look wants hard
 * facets, and the skinned mesh carries no normal attribute at all. The
 * tinted base and its face overlays come from CharacterFace.glsl.
 */
const float CHARACTER_SPECULAR = 0.08;

void main() {
    vec4 base = texture(u_texture, vec3(v_uv, float(u_layer_albedo)));

    if (base.a < 0.01)
    discard;

    vec3 albedo = resolveCharacterAlbedo(
        u_texture, u_layer_albedo, v_uv, base.rgb, v_tint, v_detailTint,
        v_faceRegion, v_eyesRegion, v_browsRegion, v_mouthRegion);

    vec3 normalView = normalize(cross(dFdx(v_viewPosition), dFdy(v_viewPosition)));

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    // gMaterial packs r = sun visibility, g = specular, b = ao (see
    // StandardSurfaceShader.fsh).
    gAlbedo   = vec4(albedo, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(1.0, CHARACTER_SPECULAR, 1.0, 1.0);
}
