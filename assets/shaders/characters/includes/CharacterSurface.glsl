#ifndef CHARACTER_SURFACE_GLSL
#define CHARACTER_SURFACE_GLSL

#include "characters/includes/CharactersStandard.glsl"
#include "characters/includes/CharacterMaterial.glsl"
#include "characters/includes/CharacterAppearance.glsl"
#include "characters/includes/CharacterZone.glsl"

// A character's surface is shaded by the zone its face is made of, from the colors and amounts in its palette row.
// Skin takes its tile tinted by the skin color, then blemishes, blush, freckles, lip color and eyeliner where the
// mesh's masks paint them; freckles and blemishes are cells fixed to the bind-pose surface, so they stay put as the
// face moves. Over the skin go the overlays the model places — the brows, the nipples and the body hair — each an image
// painted in the body's UV layout over one region of it, so it stays on the same skin however the body is shaped or
// posed, tinted by its color and shown as far as its detail amount; an overlay with relief rises from the skin through
// its height map (parallax occlusion) and bends the normal through its normal map, within the Character Detail
// distance. The eye is its variant's image laid on its raw corner UVs, the iris sized, the pupil dilated and the iris
// seen through the cornea (shadeEye). Hair is its cards' strand texture with a root-to-tip grade and a broken band of
// shine where it faces up toward the light. Specular rises for the wet eye and the shining hair.

struct CharacterSurface {
    vec3  albedo;
    float specular;
    vec3  normal;
};

const float CHARACTER_SKIN_SPECULAR  = 0.08;
const float CHARACTER_EYE_SPECULAR   = 0.95;
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
const float CHARACTER_IRIS_DEPTH     = 0.0015;
const float CHARACTER_EYE_SPECULAR_FAR = 0.55;

const float CHARACTER_OVERLAY_SOFTNESS = 0.3;
const int   CHARACTER_RELIEF_STEPS_MIN = 4;
const int   CHARACTER_RELIEF_STEPS_MAX = 16;
const float CHARACTER_RELIEF_GRAZING   = 0.25;

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

// One layer of an overlay's image at a place in its region, scaled into its tile of the atlas with the region's
// screen derivatives
vec4 sampleOverlayTile(vec4 tile, vec2 local, vec2 dx, vec2 dy, int layer) {
    vec2 scale = vec2(tile.y - tile.x, tile.w - tile.z);
    vec2 uv    = vec2(tile.x, tile.z) + clamp(local, 0.0, 1.0) * scale;

    return textureGrad(u_texture, vec3(uv, float(layer)), dx * scale, dy * scale);
}

// Parallax occlusion for relief standing on the skin: the view ray enters the relief's top shifted toward the eye and
// marches down to the skin itself, so bare skin is never shifted; more steps the nearer the eye
vec2 marchOverlayRelief(vec4 tile, vec2 local, vec2 dx, vec2 dy, vec2 shift, float detail) {
    int   steps  = int(mix(float(CHARACTER_RELIEF_STEPS_MIN), float(CHARACTER_RELIEF_STEPS_MAX), detail));
    float stride = 1.0 / float(steps);
    vec2  entry  = local + shift;
    float ray    = 1.0;
    float height = sampleOverlayTile(tile, entry, dx, dy, u_layer_height).r;
    float above  = ray - height;

    for (int i = 0; i < CHARACTER_RELIEF_STEPS_MAX; i++) {
        if (i >= steps || height >= ray)
        break;

        above  = ray - height;
        ray   -= stride;
        height = sampleOverlayTile(tile, mix(local, entry, ray), dx, dy, u_layer_height).r;
    }

    float below = height - ray;
    float t     = clamp(ray + stride * below / max(above + below, 1e-5), 0.0, 1.0);

    return mix(local, entry, t);
}

vec3 shadeOverlays(int instance, vec3 skin, vec2 rawUV, vec3 viewPosition, CharacterFrame frame, inout vec3 normal) {
    vec3 viewDir = normalize(-viewPosition);
    vec3 viewTS  = vec3(dot(viewDir, frame.basis[0]), dot(viewDir, frame.basis[1]), dot(viewDir, frame.basis[2]));

    for (int i = 0; i < CHARACTER_OVERLAY_COUNT; i++) {
        float amount = fetchCharacterDetail(instance, CHARACTER_OVERLAY_DETAIL[i]);
        vec4  region = fetchOverlay(instance, i, 1);
        vec2  size   = region.zw - region.xy;

        if (amount <= 0.001 || size.x <= 0.0 || size.y <= 0.0)
        continue;

        vec2 local = (rawUV - region.xy) / size;

        if (any(lessThan(local, vec2(0.0))) || any(greaterThan(local, vec2(1.0))))
        continue;

        vec4  tile   = fetchOverlay(instance, i, 0);
        float relief = fetchOverlay(instance, i, 2).x * frame.detail;
        vec2  dx     = frame.uvDx / size;
        vec2  dy     = frame.uvDy / size;

        if (relief > 0.0)
        local = marchOverlayRelief(tile, local, dx, dy,
            viewTS.xy / max(viewTS.z, CHARACTER_RELIEF_GRAZING) * relief * frame.uvPerMeter / size, frame.detail);

        vec4  image = sampleOverlayTile(tile, local, dx, dy, u_layer_albedo);
        float cover = smoothstep(1.0 - amount, 1.0 - amount + CHARACTER_OVERLAY_SOFTNESS, image.a) * image.a;

        skin = mix(skin, fetchCharacterColor(instance, CHARACTER_OVERLAY_COLOR[i]) * image.rgb, cover);

        if (relief > 0.0) {
            vec3 mapNormal = sampleOverlayTile(tile, local, dx, dy, u_layer_normal).xyz * 2.0 - 1.0;
            normal = normalize(mix(normal, normalize(frame.basis * mapNormal), cover * frame.detail));
        }
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

// The eye from its image set: the image scaled about the iris by the iris size, the pupil dilated inside the iris by
// the pupil size, and the iris seen through the cornea — sitting CHARACTER_IRIS_DEPTH behind the eye's surface, it
// shifts against the view, within the Character Detail distance. The eye color reaches what the tint mask allows.
vec3 shadeEye(int instance, vec2 rawUV, vec3 viewPosition, CharacterFrame frame) {
    vec4 tile = fetchCharacterVariant(instance);

    if (tile.y <= tile.x)
    return vec3(1.0);

    vec3  viewDir   = normalize(-viewPosition);
    vec3  viewTS    = vec3(dot(viewDir, frame.basis[0]), dot(viewDir, frame.basis[1]), dot(viewDir, frame.basis[2]));
    float irisScale = max(fetchCharacterDetail(instance, CHARACTER_DETAIL_IRIS_SIZE), 0.05);
    vec2  offset    = rawUV - CHARACTER_IRIS_CENTRE;
    float inside    = 1.0 - smoothstep(0.9, 1.1, length(offset / (CHARACTER_IRIS_RADIUS * irisScale)));

    offset -= viewTS.xy / max(viewTS.z, CHARACTER_RELIEF_GRAZING) * CHARACTER_IRIS_DEPTH * frame.uvPerMeter * inside
        * frame.detail;

    vec2  local  = offset / irisScale;
    float radius = length(local / CHARACTER_IRIS_RADIUS);
    float pupil  = clamp(CHARACTER_PUPIL_RATIO * fetchCharacterDetail(instance, CHARACTER_DETAIL_PUPIL_SIZE), 0.02,
        0.95);
    float drawn  = radius < pupil
        ? radius * CHARACTER_PUPIL_RATIO / pupil
        : CHARACTER_PUPIL_RATIO + (radius - pupil) * (1.0 - CHARACTER_PUPIL_RATIO) / (1.0 - pupil);

    local *= radius < 1.0 && radius > 0.00001 ? drawn / radius : 1.0;

    vec2  scale = vec2(tile.y - tile.x, tile.w - tile.z);
    vec2  uv    = vec2(tile.x, tile.z) + clamp(CHARACTER_IRIS_CENTRE + local, 0.0, 1.0) * scale;
    vec2  dx    = frame.uvDx / irisScale * scale;
    vec2  dy    = frame.uvDy / irisScale * scale;
    vec3  image = textureGrad(u_texture, vec3(uv, float(u_layer_albedo)), dx, dy).rgb;
    float tint  = textureGrad(u_texture, vec3(uv, float(u_layer_tint)), dx, dy).r;

    return mix(image, image * fetchCharacterColor(instance, CHARACTER_COLOR_EYES), tint);
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
    vec3 viewPosition,
    CharacterFrame frame,
    CharacterMaterial material) {

    vec3 viewNormal = material.normal;
    CharacterSurface surface;
    surface.specular = CHARACTER_SKIN_SPECULAR;
    surface.normal   = viewNormal;

    // The eye is tinted inside its own image and glistens as far as the detail allows: its highlight and the sky's
    // reflection the lighting pass lays on glossy characters fade together
    if (zone == CHARACTER_ZONE_EYE) {
        surface.albedo   = shadeEye(instance, rawUV, viewPosition, frame);
        surface.specular = mix(CHARACTER_EYE_SPECULAR_FAR, CHARACTER_EYE_SPECULAR, frame.detail);
        return surface;
    }

    if (zone == CHARACTER_ZONE_HAIR) {
        surface.albedo   = shadeHair(instance, base, rawUV, viewNormal);
        surface.specular = CHARACTER_HAIR_SPECULAR;
    } else if (zone == CHARACTER_ZONE_BROW) {
        surface.albedo = fetchCharacterColor(instance, CHARACTER_COLOR_BROWS);
    } else if (zone == CHARACTER_ZONE_LASH) {
        surface.albedo = fetchCharacterColor(instance, CHARACTER_COLOR_BROWS) * CHARACTER_LASH_SHADE;
    } else if (zone == CHARACTER_ZONE_TEETH) {
        surface.albedo = base * fetchCharacterColor(instance, CHARACTER_COLOR_TEETH);
    } else if (zone == CHARACTER_ZONE_MOUTH) {
        surface.albedo = fetchCharacterColor(instance, CHARACTER_COLOR_LIPS) * CHARACTER_MOUTH_SHADE;
    } else if (zone == CHARACTER_ZONE_CLOTH) {
        surface.albedo = base * fetchCharacterColor(instance, CHARACTER_COLOR_CLOTH);
    } else {
        surface.albedo = shadeOverlays(instance, shadeSkin(instance, base, masks, surfacePosition), rawUV, viewPosition,
            frame, surface.normal);
    }


    surface.albedo   = resolveCharacterAlbedo(base, surface.albedo, material);
    surface.specular = resolveCharacterSpecular(surface.specular, material);
    return surface;
}

#endif
