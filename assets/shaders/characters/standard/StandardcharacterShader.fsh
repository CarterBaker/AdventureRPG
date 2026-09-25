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

uniform sampler2DArray u_texture;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

/*
 * Characters write the same G-buffer as the world surface so the deferred
 * lighting pass lights them identically. Normals are the flat face normal
 * rebuilt from screen-space derivatives — the low-poly look wants hard
 * facets, and the skinned mesh carries no normal attribute at all.
 *
 * The base texture is a near-white tile multiplied by this part's tint
 * (skin tone for body and head, hair color for hair). Fragments landing
 * inside the head's face tile are re-mapped to face-local 0..1 and every
 * chosen feature overlay (eyes, brows, mouth) is alpha-blended on top,
 * each sampled from its own tile rect in the same array. A zero-width
 * region means that slot is empty and is skipped.
 */
const float CHARACTER_SPECULAR = 0.08;

bool hasRegion(vec4 region) {
    return region.z > region.x;
}

vec4 sampleRegion(vec4 region, vec2 localUV) {
    vec2 uv = mix(region.xy, region.zw, clamp(localUV, 0.0, 1.0));
    return texture(u_texture, vec3(uv, float(u_layer_albedo)));
}

vec3 applyOverlay(vec3 albedo, vec4 region, vec2 faceUV, vec3 tint) {
    if (!hasRegion(region))
    return albedo;

    vec4 overlay = sampleRegion(region, faceUV);
    return mix(albedo, overlay.rgb * tint, overlay.a);
}

void main() {
    vec4 base = texture(u_texture, vec3(v_uv, float(u_layer_albedo)));

    if (base.a < 0.01)
    discard;

    vec3 albedo = base.rgb * v_tint;

    if (hasRegion(v_faceRegion)
        && all(greaterThanEqual(v_uv, v_faceRegion.xy))
        && all(lessThanEqual(v_uv, v_faceRegion.zw))) {

        vec2 faceUV = (v_uv - v_faceRegion.xy) / (v_faceRegion.zw - v_faceRegion.xy);

        albedo = applyOverlay(albedo, v_eyesRegion,  faceUV, vec3(1.0));
        albedo = applyOverlay(albedo, v_browsRegion, faceUV, v_detailTint);
        albedo = applyOverlay(albedo, v_mouthRegion, faceUV, vec3(1.0));
    }

    vec3 normalView = normalize(cross(dFdx(v_viewPosition), dFdy(v_viewPosition)));

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    // gMaterial packs r = sun visibility, g = specular, b = ao (see
    // StandardSurfaceShader.fsh).
    gAlbedo   = vec4(albedo, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(1.0, CHARACTER_SPECULAR, 1.0, 1.0);
}
