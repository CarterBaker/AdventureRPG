#ifndef CHARACTER_APPEARANCE_GLSL
#define CHARACTER_APPEARANCE_GLSL

// Every skinned instance owns one row of the palette texture: its bones, then its appearance from u_appearanceBase —
// a header texel (x = hidden bone, -1 for none), one texel per color in SkinnedColor order and the detail amounts four
// to a texel in SkinnedDetail order — then its morph weights four to a texel from u_morphBase.

uniform sampler2D u_bonePalette;
uniform int u_appearanceBase;

const int CHARACTER_COLOR_SKIN     = 0;
const int CHARACTER_COLOR_EYES     = 1;
const int CHARACTER_COLOR_HAIR     = 2;
const int CHARACTER_COLOR_BROWS    = 3;
const int CHARACTER_COLOR_LIPS     = 4;
const int CHARACTER_COLOR_BLUSH    = 5;
const int CHARACTER_COLOR_FRECKLES = 6;
const int CHARACTER_COLOR_CLOTH    = 7;
const int CHARACTER_COLOR_COUNT    = 8;

const int CHARACTER_DETAIL_IRIS_SIZE  = 0;
const int CHARACTER_DETAIL_PUPIL_SIZE = 1;
const int CHARACTER_DETAIL_HAIR_SHINE = 2;
const int CHARACTER_DETAIL_LIPS       = 3;
const int CHARACTER_DETAIL_BLUSH      = 4;
const int CHARACTER_DETAIL_FRECKLES   = 5;
const int CHARACTER_DETAIL_LINER      = 6;
const int CHARACTER_DETAIL_BLEMISHES  = 7;

float fetchHiddenBone(int instance) {
    return texelFetch(u_bonePalette, ivec2(u_appearanceBase, instance), 0).x;
}

vec3 fetchCharacterColor(int instance, int color) {
    return texelFetch(u_bonePalette, ivec2(u_appearanceBase + 1 + color, instance), 0).rgb;
}

float fetchCharacterDetail(int instance, int detail) {
    vec4 texel = texelFetch(u_bonePalette, ivec2(u_appearanceBase + 1 + CHARACTER_COLOR_COUNT + detail / 4, instance), 0);
    return texel[detail % 4];
}

#endif
