#version 330 core

in vec2 v_uv;
in vec3 v_viewPosition;
in vec3 v_viewNormal;
in vec3 v_surfacePosition;
in vec4 v_masks;
in float v_maskWeight;
flat in int v_zone;
flat in int v_instance;
flat in int v_tile;

#include "includes/CameraData.glsl"
#include "characters/includes/CharacterMaterial.glsl"
#include "characters/includes/CharacterSurface.glsl"
#include "characters/includes/CharacterHidden.glsl"
#include "characters/includes/CharacterNormal.glsl"
#include "postprocessing/lighting/includes/ToonLighting.glsl"
#include "postprocessing/lighting/includes/SkyReflection.glsl"

uniform vec3  u_lightDirection;
uniform float u_ambient;

out vec4 FragColor;

// An editor viewport has no deferred lighting pass, so the character is lit here by one fixed light over an ambient
// floor, on the same smooth normals and zone shading the game draws with, in the same toon tones and rim the game's
// lighting gives characters (ToonLighting.glsl).
void main() {
    if (isHiddenFragment(v_instance, v_maskWeight))
    discard;

    vec3           smoothView = resolveCharacterNormal(v_viewNormal, v_viewPosition);
    CharacterFrame frame      = resolveCharacterFrame(smoothView, v_viewPosition, v_uv);
    TileRead       tile       = resolveTileReadGrad(resolveCharacterTile(v_instance, v_tile), frame.uvDx, frame.uvDy);
    vec4           base       = sampleTileAlbedo(tile, v_uv, frame.uvDx, frame.uvDy);

    if (base.a < 0.01)
    discard;

    CharacterMaterial material = sampleCharacterMaterial(tile, v_uv, frame);
    CharacterSurface surface = resolveCharacterSurface(
        v_instance, v_zone, base.rgb, v_uv, v_masks, v_surfacePosition, v_viewPosition, frame, material);
    vec3 normalView = surface.normal;
    vec3  albedo    = surface.albedo;
    float specular  = surface.specular;

    vec3  viewDir   = normalize(-v_viewPosition);
    vec3  lightView = normalize(mat3(u_view) * -u_lightDirection);
    vec3  halfway   = normalize(lightView + viewDir);
    float diffuse   = max(dot(normalView, lightView), 0.0);
    float highlight = specular * pow(max(dot(normalView, halfway), 0.0), 32.0);
    vec3  light     = vec3(1.0 - u_ambient);

    vec3 reflection = resolveSkyReflection(mat3(u_inverseView) * normalView, mat3(u_inverseView) * viewDir, specular,
                                           material.ao);

    FragColor = vec4(resolveToonLight(albedo, normalView, viewDir, diffuse, 1.0, highlight, 0.0,
                                      u_ambient * material.ao, light, vec3(0.0)) + reflection, 1.0);
}
