#version 330 core

in vec4 v_uv;
in vec3 v_viewPosition;
in vec3 v_viewNormal;
in vec3 v_surfacePosition;
in vec4 v_masks;
in float v_hiddenWeight;
flat in int v_zone;
flat in int v_instance;

#include "characters/includes/CharactersStandard.glsl"
#include "characters/includes/CharacterSurface.glsl"
#include "characters/includes/CharacterHidden.glsl"
#include "characters/includes/CharacterNormal.glsl"
#include "includes/GBufferNormal.glsl"

uniform sampler2DArray u_texture;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

/*
 * Characters write the same G-buffer as the world surface so the deferred
 * lighting pass lights them identically. Normals are the smooth normals
 * the mesh bakes (CharacterNormal.glsl), so the skin shades as one
 * continuous surface rather than a set of facets, and they are stored as a
 * smooth surface (GBufferNormal.glsl), so the outline pass inks the body's
 * silhouette but never the folds between its facets. Each face is shaded as
 * the zone it is made of (CharacterSurface.glsl).
 */

void main() {
    if (isHiddenFragment(v_hiddenWeight))
    discard;

    vec4 base = texture(u_texture, vec3(v_uv.xy, float(u_layer_albedo)));

    if (base.a < 0.01)
    discard;

    vec3 normalView = resolveCharacterNormal(v_viewNormal, v_viewPosition);
    CharacterSurface surface = resolveCharacterSurface(
        v_instance, v_zone, base.rgb, v_uv.zw, v_masks, v_surfacePosition, normalView);

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    // gMaterial packs r = sun visibility, g = specular, b = ao (see
    // StandardSurfaceShader.fsh).
    gAlbedo   = vec4(surface.albedo, 1.0);
    gNormal   = vec4(encodeSmoothNormal(normalView), 1.0);
    gMaterial = vec4(1.0, surface.specular, 1.0, 1.0);
}
