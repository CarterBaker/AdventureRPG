#version 330 core

in vec2 v_uv;
in vec3 v_viewPosition;
in vec3 v_viewNormal;
in float v_hiddenWeight;
flat in vec3 v_tint;
flat in vec3 v_detailTint;
flat in vec4 v_faceRegion;
flat in vec4 v_eyesRegion;
flat in vec4 v_browsRegion;
flat in vec4 v_mouthRegion;

#include "includes/CameraData.glsl"
#include "characters/includes/CharactersStandard.glsl"
#include "characters/includes/CharacterFace.glsl"
#include "characters/includes/CharacterHidden.glsl"
#include "characters/includes/CharacterNormal.glsl"

uniform sampler2DArray u_texture;
uniform vec3  u_lightDirection;
uniform float u_ambient;

out vec4 FragColor;

// An editor viewport has no deferred lighting pass, so the character is lit here by one fixed light over an ambient
// floor, on the same smooth normals the game shades with, with the same tint and face overlays the game draws.
void main() {
    if (isHiddenFragment(v_hiddenWeight))
    discard;

    vec4 base = texture(u_texture, vec3(v_uv, float(u_layer_albedo)));

    if (base.a < 0.01)
    discard;

    vec3 albedo = resolveCharacterAlbedo(
        u_texture, u_layer_albedo, v_uv, base.rgb, v_tint, v_detailTint,
        v_faceRegion, v_eyesRegion, v_browsRegion, v_mouthRegion);

    vec3 normalView = resolveCharacterNormal(v_viewNormal, v_viewPosition);
    vec3 lightView  = normalize(mat3(u_view) * -u_lightDirection);

    float diffuse  = max(dot(normalView, lightView), 0.0);
    float lighting = u_ambient + (1.0 - u_ambient) * diffuse;

    FragColor = vec4(albedo * lighting, 1.0);
}
