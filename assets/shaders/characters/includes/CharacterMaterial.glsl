#ifndef CHARACTER_MATERIAL_GLSL
#define CHARACTER_MATERIAL_GLSL

#include "characters/includes/CharactersStandard.glsl"
#include "includes/SettingsData.glsl"

// A character's material maps, each its own layer of the character atlas: the normal map bends the smooth normal
// through a tangent frame rebuilt from screen-space derivatives (the mesh carries no tangents), AO darkens, metallic
// and roughness shape the highlight, and the tint mask says which texels take the player's colors. Fine detail fades
// out between CHARACTER_DETAIL_FADE_START of the Character Detail distance and all of it.

const float CHARACTER_DETAIL_FADE_START = 0.75;
const float CHARACTER_ROUGHNESS_NEUTRAL = 0.5;

struct CharacterMaterial {
    vec3  normal;
    float ao;
    float metallic;
    float roughness;
    float tint;
};

float resolveCharacterDetail(vec3 viewPosition) {
    float reach = max(u_characterDetail, 0.001);

    return 1.0 - smoothstep(reach * CHARACTER_DETAIL_FADE_START, reach, length(viewPosition));
}

float sampleCharacterLayer(vec2 uv, int layer) {
    return texture(u_texture, vec3(uv, float(layer))).r;
}

vec3 perturbCharacterNormal(vec3 normal, vec3 viewPosition, vec2 uv, vec3 mapNormal) {
    vec3 dp1 = dFdx(viewPosition);
    vec3 dp2 = dFdy(viewPosition);
    vec2 duv1 = dFdx(uv);
    vec2 duv2 = dFdy(uv);
    vec3 dp2perp = cross(dp2, normal);
    vec3 dp1perp = cross(normal, dp1);
    vec3 tangent = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 bitangent = dp2perp * duv1.y + dp1perp * duv2.y;
    float scale = inversesqrt(max(max(dot(tangent, tangent), dot(bitangent, bitangent)), 1e-20));

    return normalize(mat3(tangent * scale, bitangent * scale, normal) * mapNormal);
}

CharacterMaterial sampleCharacterMaterial(vec2 uv, vec3 viewPosition, vec3 normal) {
    CharacterMaterial material;
    float detail = resolveCharacterDetail(viewPosition);
    vec3 mapNormal = texture(u_texture, vec3(uv, float(u_layer_normal))).xyz * 2.0 - 1.0;

    material.normal    = perturbCharacterNormal(normal, viewPosition, uv, normalize(mix(vec3(0.0, 0.0, 1.0),
                                                                                      mapNormal, detail)));
    material.ao        = sampleCharacterLayer(uv, u_layer_ao);
    material.metallic  = sampleCharacterLayer(uv, u_layer_metallic);
    material.roughness = sampleCharacterLayer(uv, u_layer_roughness);
    material.tint      = sampleCharacterLayer(uv, u_layer_tint);

    return material;
}

// The player's colors reach only the texels the tint mask allows; the rest keep the texture's own color
vec3 resolveCharacterAlbedo(vec3 base, vec3 shaded, CharacterMaterial material) {
    return mix(base, shaded, material.tint);
}

// The zone's highlight scaled by roughness about its neutral, and a metal's highlight taking the place of it
float resolveCharacterSpecular(float zoneSpecular, CharacterMaterial material) {
    float dielectric = zoneSpecular * (1.0 - material.roughness) / (1.0 - CHARACTER_ROUGHNESS_NEUTRAL);

    return mix(dielectric, 1.0 - material.roughness, material.metallic);
}

#endif
