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
#include "postprocessing/lighting/includes/ToonLighting.glsl"

uniform sampler2DArray u_texture;
uniform vec3  u_lightDirection;
uniform float u_ambient;

out vec4 FragColor;

// An editor viewport has no deferred lighting pass, so the character is lit here by one fixed light over an ambient
// floor, on the same smooth normals and zone shading the game draws with, in the same toon tones and rim the game's
// lighting gives characters (ToonLighting.glsl).
void main() {
    if (isHiddenFragment(v_hiddenWeight))
    discard;

    vec4 base = texture(u_texture, vec3(v_uv.xy, float(u_layer_albedo)));

    if (base.a < 0.01)
    discard;

    vec3 normalView = resolveCharacterNormal(v_viewNormal, v_viewPosition);
    CharacterSurface surface = resolveCharacterSurface(
        v_instance, v_zone, base.rgb, v_uv.zw, v_masks, v_surfacePosition, normalView);

    vec3  viewDir   = normalize(-v_viewPosition);
    vec3  lightView = normalize(mat3(u_view) * -u_lightDirection);
    vec3  halfway   = normalize(lightView + viewDir);
    float diffuse   = max(dot(normalView, lightView), 0.0);
    float highlight = surface.specular * pow(max(dot(normalView, halfway), 0.0), 32.0);
    vec3  light     = vec3(1.0 - u_ambient);

    FragColor = vec4(resolveToonLight(surface.albedo, normalView, viewDir, diffuse, 1.0, highlight, 0.0, u_ambient,
                                      light, vec3(0.0)), 1.0);
}
