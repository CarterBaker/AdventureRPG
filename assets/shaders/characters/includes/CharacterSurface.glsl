#ifndef CHARACTER_SURFACE_GLSL
#define CHARACTER_SURFACE_GLSL

#include "characters/includes/CharactersStandard.glsl"
#include "characters/includes/CharacterAppearance.glsl"
#include "characters/includes/CharacterZone.glsl"

uniform sampler2DArray u_texture;

// A character's surface is shaded by the zone its face is made of, from the colors and amounts in its palette row.
// Skin takes its tile tinted by the skin color, then blemishes, blush, freckles, lip color and eyeliner where the
// mesh's masks paint them; freckles and blemishes are cells fixed to the bind-pose surface, so they stay put as the
// face moves. Over the skin go the overlays the model places — the brows, the nipples and the body hair — each an image
// from the character atlas laid on the bind-pose body before any morph, so it stays on the same skin however the body
// is shaped or posed, tinted by its color and shown as far as its detail amount. The eye is drawn from its raw corner UVs: a lid-shadowed sclera around an iris graded dark to light
// with a darker rim, a pupil and two highlights, tall and tucked under the upper lid as an anime eye is drawn. Hair is
// its cards' strand texture with a root-to-tip grade and a broken band of shine where it faces up toward the light.
// Specular rises for the wet eye and the shining hair.

struct CharacterSurface {
    vec3  albedo;
    float specular;
};

const float CHARACTER_SKIN_SPECULAR  = 0.08;
const float CHARACTER_EYE_SPECULAR   = 0.6;
const float CHARACTER_HAIR_SPECULAR  = 0.3;
const float CHARACTER_HAIR_ROOT_SHADE = 0.7;
const float CHARACTER_HAIR_TIP_LIGHT  = 1.1;
const float CHARACTER_TWO_PI         = 6.28318530718;

const float CHARACTER_FRECKLE_CELLS  = 420.0;
const float CHARACTER_BLEMISH_CELLS  = 160.0;
const vec3  CHARACTER_BLEMISH_TINT   = vec3(0.86, 0.68, 0.64);
const float CHARACTER_LINER_SHADE    = 0.45;

const vec2  CHARACTER_IRIS_RADIUS    = vec2(0.255, 0.30);
const vec2  CHARACTER_IRIS_CENTRE    = vec2(0.5, 0.47);
const float CHARACTER_PUPIL_RATIO    = 0.42;
const vec3  CHARACTER_SCLERA         = vec3(0.97, 0.96, 0.95);
const vec3  CHARACTER_PUPIL          = vec3(0.03, 0.03, 0.05);
const vec2  CHARACTER_HIGHLIGHT_MAIN = vec2(-0.30, -0.38);
const vec2  CHARACTER_HIGHLIGHT_SIDE = vec2(0.28, 0.30);

const float CHARACTER_OVERLAY_FACING  = 0.2;
const float CHARACTER_OVERLAY_EDGE    = 0.25;
const float CHARACTER_OVERLAY_SOFTNESS = 0.3;
const int   CHARACTER_PROJECT_FRONT   = 0;
const int   CHARACTER_PROJECT_WRAP    = 2;
const int   CHARACTER_PROJECT_MIRROR  = 4;

const float CHARACTER_MOUTH_SHADE    = 0.35;
const float CHARACTER_LASH_SHADE     = 0.25;

float characterHash(vec3 cell) {
    return fract(sin(dot(cell, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

vec3 characterHash3(vec3 cell) {
    return vec3(
        characterHash(cell),
        characterHash(cell + vec3(19.19, 7.31, 3.17)),
        characterHash(cell + vec3(5.71, 23.3, 11.9)));
}

// How strongly a cell spot covers this point: each cell holds at most one round spot, present at the given density
float characterSpots(vec3 position, float cells, float density) {
    vec3  scaled = position * cells;
    vec3  origin = floor(scaled - 0.5);
    float cover  = 0.0;

    for (int i = 0; i < 8; i++) {
        vec3 cell = origin + vec3(float(i & 1), float((i >> 1) & 1), float((i >> 2) & 1));

        if (characterHash(cell + vec3(41.0)) > density)
        continue;

        vec3  centre = cell + 0.2 + 0.6 * characterHash3(cell);
        float radius = 0.16 + 0.14 * characterHash(cell + vec3(3.0));
        float edge   = fwidth(scaled.x) + 0.02;

        cover = max(cover, 1.0 - smoothstep(radius - edge, radius + edge, distance(scaled, centre)));
    }

    return cover;
}

// The overlay image at a point of its tile, the tile repeating across whole coordinates without a seam in its mips
vec4 sampleOverlayTile(vec4 tile, vec2 coordinate) {
    vec2 scale = vec2(tile.y - tile.x, tile.w - tile.z);
    vec2 uv    = vec2(tile.x, tile.z) + fract(coordinate) * scale;

    return textureGrad(u_texture, vec3(uv, float(u_layer_albedo)), dFdx(coordinate) * scale, dFdy(coordinate) * scale);
}

// How far one overlay covers a point of the bind-pose body, with the shade its image gives there: a front or back
// overlay is its image stretched once across its box on the faces turned that way, a wrapped one its image repeated
// from all three axes and faded at the box's edge, and a mirrored one lies on both sides of the body. The detail
// amount thins the image's coverage from its faint edges in, so a small amount keeps only its strongest marks.
vec4 shadeOverlay(int instance, int overlay, vec3 bindPosition, vec3 bindNormal) {
    vec4  size   = fetchOverlay(instance, overlay, 2);
    float amount = fetchCharacterDetail(instance, CHARACTER_OVERLAY_DETAIL[overlay]);

    if (size.x <= 0.0 || amount <= 0.001)
    return vec4(0.0);

    vec4 tile  = fetchOverlay(instance, overlay, 0);
    vec4 place = fetchOverlay(instance, overlay, 1);
    int  code  = int(place.w + 0.5);
    vec3 point = bindPosition;
    vec3 facing = normalize(bindNormal);

    if (code >= CHARACTER_PROJECT_MIRROR && point.x * place.x < 0.0) {
        point.x  = -point.x;
        facing.x = -facing.x;
    }

    int  projection = code % CHARACTER_PROJECT_MIRROR;
    vec3 local      = (point - place.xyz) / (size.xyz * 0.5);

    if (any(greaterThan(abs(local), vec3(1.0))))
    return vec4(0.0);

    vec4 image;

    if (projection == CHARACTER_PROJECT_WRAP) {
        vec3 repeat = point * size.w;
        vec3 blend  = pow(abs(facing), vec3(4.0));
        blend /= blend.x + blend.y + blend.z;

        image = sampleOverlayTile(tile, repeat.zy) * blend.x
            + sampleOverlayTile(tile, repeat.xz) * blend.y
            + sampleOverlayTile(tile, repeat.xy) * blend.z;
        image.a *= 1.0 - smoothstep(1.0 - CHARACTER_OVERLAY_EDGE, 1.0, max(abs(local.x), max(abs(local.y), abs(local.z))));
    } else {
        float side = projection == CHARACTER_PROJECT_FRONT ? 1.0 : -1.0;
        vec2  uv   = clamp(vec2(local.x * side, -local.y) * 0.5 + 0.5, 0.0, 0.999);

        image = sampleOverlayTile(tile, uv);
        image.a *= smoothstep(0.0, CHARACTER_OVERLAY_FACING, facing.z * side);
    }

    float cover = smoothstep(1.0 - amount, 1.0 - amount + CHARACTER_OVERLAY_SOFTNESS, image.a);
    return vec4(image.rgb, cover * image.a);
}

vec3 shadeOverlays(int instance, vec3 skin, vec3 bindPosition, vec3 bindNormal) {
    for (int i = 0; i < CHARACTER_OVERLAY_COUNT; i++) {
        vec4 overlay = shadeOverlay(instance, i, bindPosition, bindNormal);

        if (overlay.a > 0.001)
        skin = mix(skin, fetchCharacterColor(instance, CHARACTER_OVERLAY_COLOR[i]) * overlay.rgb, overlay.a);
    }

    return skin;
}

vec3 shadeSkin(int instance, vec3 base, vec4 masks, vec3 surfacePosition) {
    vec3 skin = base * fetchCharacterColor(instance, CHARACTER_COLOR_SKIN);

    float blemishes = fetchCharacterDetail(instance, CHARACTER_DETAIL_BLEMISHES);
    if (blemishes > 0.001)
    skin = mix(skin, skin * CHARACTER_BLEMISH_TINT,
        0.6 * characterSpots(surfacePosition, CHARACTER_BLEMISH_CELLS, blemishes * 0.35));

    skin = mix(skin, fetchCharacterColor(instance, CHARACTER_COLOR_BLUSH),
        clamp(masks.y * fetchCharacterDetail(instance, CHARACTER_DETAIL_BLUSH), 0.0, 1.0));

    float freckles = masks.x * fetchCharacterDetail(instance, CHARACTER_DETAIL_FRECKLES);
    if (freckles > 0.001)
    skin = mix(skin, fetchCharacterColor(instance, CHARACTER_COLOR_FRECKLES),
        0.75 * characterSpots(surfacePosition, CHARACTER_FRECKLE_CELLS, clamp(freckles, 0.0, 1.0) * 0.55));

    skin = mix(skin, fetchCharacterColor(instance, CHARACTER_COLOR_LIPS),
        clamp(masks.z * fetchCharacterDetail(instance, CHARACTER_DETAIL_LIPS), 0.0, 1.0));

    return mix(skin, fetchCharacterColor(instance, CHARACTER_COLOR_BROWS) * CHARACTER_LINER_SHADE,
        clamp(masks.w * fetchCharacterDetail(instance, CHARACTER_DETAIL_LINER), 0.0, 1.0));
}

float characterDisc(vec2 point, vec2 centre, float radius) {
    float distanceToCentre = length(point - centre);
    float edge = fwidth(distanceToCentre) + 0.0001;

    return 1.0 - smoothstep(radius - edge, radius + edge, distanceToCentre);
}

// A tall iris filling the opening and tucked under the upper lid, the way an anime eye is drawn; v grows downward
vec3 shadeEye(int instance, vec2 eyeUV) {
    vec2  offset = eyeUV - CHARACTER_IRIS_CENTRE;
    vec2  radius = CHARACTER_IRIS_RADIUS * fetchCharacterDetail(instance, CHARACTER_DETAIL_IRIS_SIZE);
    float e      = length(offset / radius);
    float edge   = fwidth(e) + 0.0001;
    float pupil  = CHARACTER_PUPIL_RATIO * fetchCharacterDetail(instance, CHARACTER_DETAIL_PUPIL_SIZE);

    float lidShadow = mix(0.72, 1.0, smoothstep(0.15, 0.35, eyeUV.y));
    vec3  color     = CHARACTER_SCLERA * lidShadow;

    vec3 iris   = fetchCharacterColor(instance, CHARACTER_COLOR_EYES);
    vec3 shaded = iris * (0.55 + 0.45 * (1.0 - clamp(e, 0.0, 1.0)))
        * mix(0.62, 1.2, smoothstep(-radius.y, radius.y, offset.y));
    shaded = mix(shaded, shaded * 0.55, smoothstep(0.78, 0.9, e)) * lidShadow;

    color = mix(color, shaded, 1.0 - smoothstep(1.0 - edge, 1.0 + edge, e));
    color = mix(color, CHARACTER_PUPIL, 1.0 - smoothstep(pupil - edge, pupil + edge, e));
    color = mix(color, vec3(1.0), characterDisc(offset, CHARACTER_HIGHLIGHT_MAIN * radius, radius.x * 0.17));
    color = mix(color, vec3(1.0), 0.8 * characterDisc(offset, CHARACTER_HIGHLIGHT_SIDE * radius, radius.x * 0.08));

    return color;
}

// Hair as anime hair is painted: the strand texture tinted by the hair color, darker at the roots and catching the
// light at the tips, with one crisp band of shine around the head where it faces up toward the light, its edge
// broken into the locks' points and dimmed where the strands are dark
vec3 shadeHair(int instance, vec3 base, vec2 strandUV, vec3 viewNormal) {
    vec3  hair = base * fetchCharacterColor(instance, CHARACTER_COLOR_HAIR);
    float root = mix(CHARACTER_HAIR_ROOT_SHADE, 1.0, smoothstep(0.0, 0.35, strandUV.y));
    float tip  = mix(1.0, CHARACTER_HAIR_TIP_LIGHT, smoothstep(0.65, 1.0, strandUV.y));
    hair *= root * tip;

    float jag   = 0.07 * sin(strandUV.x * CHARACTER_TWO_PI * 3.0) + 0.04 * sin(strandUV.x * CHARACTER_TWO_PI * 7.0);
    float band  = smoothstep(0.30 + jag, 0.36 + jag, viewNormal.y)
                * (1.0 - smoothstep(0.62 + jag, 0.68 + jag, viewNormal.y));
    float shine = fetchCharacterDetail(instance, CHARACTER_DETAIL_HAIR_SHINE) * band * mix(0.5, 1.0, base.r);

    return hair + (hair * 0.7 + vec3(0.10)) * shine;
}

CharacterSurface resolveCharacterSurface(
    int instance,
    int zone,
    vec3 base,
    vec2 rawUV,
    vec4 masks,
    vec3 surfacePosition,
    vec3 bindPosition,
    vec3 bindNormal,
    vec3 viewNormal) {

    CharacterSurface surface;
    surface.specular = CHARACTER_SKIN_SPECULAR;

    if (zone == CHARACTER_ZONE_EYE) {
        surface.albedo   = shadeEye(instance, rawUV);
        surface.specular = CHARACTER_EYE_SPECULAR;
    } else if (zone == CHARACTER_ZONE_HAIR) {
        surface.albedo   = shadeHair(instance, base, rawUV, viewNormal);
        surface.specular = CHARACTER_HAIR_SPECULAR;
    } else if (zone == CHARACTER_ZONE_BROW) {
        surface.albedo = fetchCharacterColor(instance, CHARACTER_COLOR_BROWS);
    } else if (zone == CHARACTER_ZONE_LASH) {
        surface.albedo = fetchCharacterColor(instance, CHARACTER_COLOR_BROWS) * CHARACTER_LASH_SHADE;
    } else if (zone == CHARACTER_ZONE_TEETH) {
        surface.albedo = fetchCharacterColor(instance, CHARACTER_COLOR_TEETH);
    } else if (zone == CHARACTER_ZONE_MOUTH) {
        surface.albedo = fetchCharacterColor(instance, CHARACTER_COLOR_LIPS) * CHARACTER_MOUTH_SHADE;
    } else if (zone == CHARACTER_ZONE_CLOTH) {
        surface.albedo = base * fetchCharacterColor(instance, CHARACTER_COLOR_CLOTH);
    } else {
        surface.albedo = shadeOverlays(instance, shadeSkin(instance, base, masks, surfacePosition), bindPosition,
            bindNormal);
    }

    return surface;
}

#endif
