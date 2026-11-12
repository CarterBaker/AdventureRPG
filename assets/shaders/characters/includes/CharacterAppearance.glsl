#ifndef CHARACTER_APPEARANCE_GLSL
#define CHARACTER_APPEARANCE_GLSL

// Every skinned instance owns one row of the palette texture: its bones, then its appearance from u_appearanceBase —
// a header texel (x = SkinnedMaskMode), one texel per color in SkinnedColor order, the detail amounts four to a texel
// in SkinnedDetail order, the bone mask (SKINNED_BONE_MASK_BITS bones to a channel), three texels per overlay in
// SkinnedOverlay order and the tile of the material variant the part drawn shows — then its morph weights four to a
// texel from u_morphBase.

uniform sampler2D u_bonePalette;
uniform int u_appearanceBase;

const int CHARACTER_COLOR_SKIN      = 0;
const int CHARACTER_COLOR_EYES      = 1;
const int CHARACTER_COLOR_HAIR      = 2;
const int CHARACTER_COLOR_BROWS     = 3;
const int CHARACTER_COLOR_LIPS      = 4;
const int CHARACTER_COLOR_BLUSH     = 5;
const int CHARACTER_COLOR_FRECKLES  = 6;
const int CHARACTER_COLOR_CLOTH     = 7;
const int CHARACTER_COLOR_NIPPLES   = 8;
const int CHARACTER_COLOR_BODY_HAIR = 9;
const int CHARACTER_COLOR_TEETH     = 10;
const int CHARACTER_COLOR_COUNT     = 11;

const int CHARACTER_DETAIL_IRIS_SIZE     = 0;
const int CHARACTER_DETAIL_PUPIL_SIZE    = 1;
const int CHARACTER_DETAIL_HAIR_SHINE    = 2;
const int CHARACTER_DETAIL_LIPS          = 3;
const int CHARACTER_DETAIL_BLUSH         = 4;
const int CHARACTER_DETAIL_FRECKLES      = 5;
const int CHARACTER_DETAIL_LINER         = 6;
const int CHARACTER_DETAIL_BLEMISHES     = 7;
const int CHARACTER_DETAIL_BROWS         = 8;
const int CHARACTER_DETAIL_NIPPLES       = 9;
const int CHARACTER_DETAIL_CHEST_HAIR    = 10;
const int CHARACTER_DETAIL_STOMACH_HAIR  = 11;
const int CHARACTER_DETAIL_PUBIC_HAIR    = 12;
const int CHARACTER_DETAIL_UNDERARM_HAIR = 13;
const int CHARACTER_DETAIL_ARM_HAIR      = 14;
const int CHARACTER_DETAIL_LEG_HAIR      = 15;
const int CHARACTER_DETAIL_TEXELS        = 4;

const int CHARACTER_MASK_NONE  = 0;
const int CHARACTER_MASK_HIDE  = 1;
const int CHARACTER_MASK_SHOW  = 2;
const int CHARACTER_MASK_BITS  = 24;
const int CHARACTER_MASK_TEXELS = 4;

const int CHARACTER_OVERLAY_COUNT  = 8;
const int CHARACTER_OVERLAY_TEXELS = 3;

// Each overlay's color and detail, in SkinnedOverlay order
const int CHARACTER_OVERLAY_COLOR[CHARACTER_OVERLAY_COUNT] = int[](
    CHARACTER_COLOR_BROWS, CHARACTER_COLOR_NIPPLES, CHARACTER_COLOR_BODY_HAIR, CHARACTER_COLOR_BODY_HAIR,
    CHARACTER_COLOR_BODY_HAIR, CHARACTER_COLOR_BODY_HAIR, CHARACTER_COLOR_BODY_HAIR, CHARACTER_COLOR_BODY_HAIR);
const int CHARACTER_OVERLAY_DETAIL[CHARACTER_OVERLAY_COUNT] = int[](
    CHARACTER_DETAIL_BROWS, CHARACTER_DETAIL_NIPPLES, CHARACTER_DETAIL_CHEST_HAIR, CHARACTER_DETAIL_STOMACH_HAIR,
    CHARACTER_DETAIL_PUBIC_HAIR, CHARACTER_DETAIL_UNDERARM_HAIR, CHARACTER_DETAIL_ARM_HAIR,
    CHARACTER_DETAIL_LEG_HAIR);

int fetchMaskMode(int instance) {
    return int(texelFetch(u_bonePalette, ivec2(u_appearanceBase, instance), 0).x + 0.5);
}

vec3 fetchCharacterColor(int instance, int color) {
    return texelFetch(u_bonePalette, ivec2(u_appearanceBase + 1 + color, instance), 0).rgb;
}

float fetchCharacterDetail(int instance, int detail) {
    vec4 texel = texelFetch(u_bonePalette, ivec2(u_appearanceBase + 1 + CHARACTER_COLOR_COUNT + detail / 4, instance), 0);
    return texel[detail % 4];
}

int characterMaskBase() {
    return u_appearanceBase + 1 + CHARACTER_COLOR_COUNT + CHARACTER_DETAIL_TEXELS;
}

// Whether a bone is in the instance's mask: its bit in the channel that holds it
bool isMaskedBone(int instance, int bone) {
    int  channel = bone / CHARACTER_MASK_BITS;
    vec4 texel   = texelFetch(u_bonePalette, ivec2(characterMaskBase() + channel / 4, instance), 0);
    int  bits    = int(texel[channel % 4] + 0.5);

    return ((bits >> (bone % CHARACTER_MASK_BITS)) & 1) != 0;
}

// The atlas tile (u0, u1, v0, v1) of the material variant the part drawn shows, all zero for its own texture
vec4 fetchCharacterVariant(int instance) {
    int base = characterMaskBase() + CHARACTER_MASK_TEXELS + CHARACTER_OVERLAY_COUNT * CHARACTER_OVERLAY_TEXELS;
    return texelFetch(u_bonePalette, ivec2(base, instance), 0);
}

// Where a corner samples the atlas: its own place, or its raw UV laid into the variant's tile (a texture swap)
vec2 resolveCharacterUV(int instance, vec2 atlasUV, vec2 rawUV) {
    vec4 variant = fetchCharacterVariant(instance);

    if (variant.y <= variant.x)
    return atlasUV;

    return vec2(variant.x, variant.z) + clamp(rawUV, 0.0, 1.0) * vec2(variant.y - variant.x, variant.w - variant.z);
}

// One texel of an overlay's placement: 0 the atlas tile of the style worn, 1 its UV region, 2 its relief in blocks
vec4 fetchOverlay(int instance, int overlay, int texel) {
    int base = characterMaskBase() + CHARACTER_MASK_TEXELS + overlay * CHARACTER_OVERLAY_TEXELS;
    return texelFetch(u_bonePalette, ivec2(base + texel, instance), 0);
}

#endif
