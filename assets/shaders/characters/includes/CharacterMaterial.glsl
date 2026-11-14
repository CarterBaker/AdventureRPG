#ifndef CHARACTER_MATERIAL_GLSL
#define CHARACTER_MATERIAL_GLSL

#include "includes/TileTextures.glsl"
#include "includes/SettingsData.glsl"

// A character's material maps, read from its tile: the normal map bends the smooth normal
// through a tangent frame rebuilt from screen-space derivatives (the mesh carries no tangents), AO darkens, metallic
// and roughness shape the highlight, and the tint mask says which texels take the player's colors. Ambient occlusion
// and metallic come from the tile's material map, roughness and the tint mask from its extra set. Fine detail fades
// out between CHARACTER_DETAIL_FADE_START of the Character Detail distance and all of it. The frame is resolved once
// per fragment, in uniform control flow, so overlays sampled inside branches take its derivatives explicitly.

const float CHARACTER_DETAIL_FADE_START = 0.75;
const float CHARACTER_ROUGHNESS_NEUTRAL = 0.5;

struct CharacterFrame {
    mat3  basis;
    vec2  uvDx;
    vec2  uvDy;
    float uvPerMeter;
    float detail;
};

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

// The tangent frame of the corner UVs about the smooth normal (the cotangent frame), the UVs' screen derivatives, how
// many UV units span a block there, and how much fine detail the distance allows
CharacterFrame resolveCharacterFrame(vec3 normal, vec3 viewPosition, vec2 uv) {
    CharacterFrame frame;
    vec3 dp1 = dFdx(viewPosition);
    vec3 dp2 = dFdy(viewPosition);
    vec2 duv1 = dFdx(uv);
    vec2 duv2 = dFdy(uv);
    vec3 dp2perp = cross(dp2, normal);
    vec3 dp1perp = cross(normal, dp1);
    vec3 tangent = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 bitangent = dp2perp * duv1.y + dp1perp * duv2.y;
    float scale = inversesqrt(max(max(dot(tangent, tangent), dot(bitangent, bitangent)), 1e-20));

    frame.basis      = mat3(tangent * scale, bitangent * scale, normal);
    frame.uvDx       = duv1;
    frame.uvDy       = duv2;
    frame.uvPerMeter = length(duv1) / max(length(dp1), 1e-6);
    frame.detail     = resolveCharacterDetail(viewPosition);

    return frame;
}

CharacterMaterial sampleCharacterMaterial(TileRead tile, vec2 uv, CharacterFrame frame) {
    CharacterMaterial material;
    vec3 mapNormal = sampleTileNormal(tile, uv, frame.uvDx, frame.uvDy);
    vec4 maps      = sampleTileMaterial(tile, uv, frame.uvDx, frame.uvDy);
    vec4 extra     = sampleTileExtra(tile, uv, frame.uvDx, frame.uvDy);

    material.normal    = normalize(frame.basis * normalize(mix(vec3(0.0, 0.0, 1.0), mapNormal, frame.detail)));
    material.ao        = maps.r;
    material.metallic  = maps.b;
    material.roughness = extra.g;
    material.tint      = extra.b;

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
