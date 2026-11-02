#version 330 core

in vec4 v_uv;
in vec3 v_viewPosition;
in vec3 v_viewNormal;
in vec3 v_surfacePosition;
in vec4 v_masks;
in float v_hiddenWeight;
flat in int v_zone;
flat in int v_instance;

#include "includes/CameraData.glsl"
#include "characters/includes/CharactersStandard.glsl"
#include "characters/includes/CharacterSurface.glsl"
#include "characters/includes/CharacterHidden.glsl"
#include "characters/includes/CharacterNormal.glsl"

uniform sampler2DArray u_texture;
uniform vec3  u_lightDirection;
uniform float u_ambient;

out vec4 FragColor;

// An editor viewport has no deferred lighting pass, so the character is lit here by one fixed light over an ambient
// floor, on the same smooth normals and zone shading the game draws with, plus a highlight for the specular.
void main() {
    if (isHiddenFragment(v_hiddenWeight))
    discard;

    vec4 base = texture(u_texture, vec3(v_uv.xy, float(u_layer_albedo)));

    if (base.a < 0.01)
    discard;

    vec3 normalView = resolveCharacterNormal(v_viewNormal, v_viewPosition);
    CharacterSurface surface = resolveCharacterSurface(
        v_instance, v_zone, base.rgb, v_uv.zw, v_masks, v_surfacePosition, normalView);

    vec3  lightView = normalize(mat3(u_view) * -u_lightDirection);
    vec3  halfway   = normalize(lightView + normalize(-v_viewPosition));
    float diffuse   = max(dot(normalView, lightView), 0.0);
    float lighting  = u_ambient + (1.0 - u_ambient) * diffuse;
    float highlight = surface.specular * pow(max(dot(normalView, halfway), 0.0), 32.0);

    FragColor = vec4(surface.albedo * lighting + vec3(highlight), 1.0);
}
