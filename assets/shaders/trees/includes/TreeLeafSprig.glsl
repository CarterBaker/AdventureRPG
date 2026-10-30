#ifndef TREE_LEAF_SPRIG_GLSL
#define TREE_LEAF_SPRIG_GLSL

#include "includes/NoiseUtility.glsl"
#include "trees/includes/TreeLeaf.glsl"

// Every leaf card is cut out of a transparent sprig texture drawn right here: fronds radiate from the card's centre out
// toward the cluster's rim, each a thin stem with leaves branching out on both sides, angled toward its tip, widest
// midway along and shrinking to the tip, and on an upright card the fronds reaching down fall short of the rim, so
// every sheaf stands as a half sphere of foliage. Nothing reaches past the rim. A fragment between the leaves is
// discarded, so the sheaf reads as layered foliage the branches and sky show through, and nothing is traced and no
// depth written, so the cards keep the early depth test. The leaves wear the species' leaf texture, repeating once per
// block across the card, and where that texture leaves a gap so does the leaf. Light rounds the sheaf into a dome: each
// fragment's normal leans from its card's facing toward the way out from the cluster's centre, and the clump darkens
// toward its heart and its underside. Far off, where a leaf would shrink below a pixel, leaves and stems widen with the
// fragment's footprint, so a distant sheaf fills in rather than shimmering away, and a stem thinner than its footprint
// wears its leaves' shade, so no spokes show from afar. Once the widened leaves of one frond already overlap row on
// row, the sheaf is read as solid foliage out to each frond's ragged reach, its blades never traced, so a far crown
// costs a fraction of a near one and looks the same. A card turning edge-on to the camera draws in toward its
// centre, so no card ever shows as a streak. Fragment stage only.

const float TREE_LEAF_FROND_ARC       = 0.7;
const float TREE_LEAF_FROND_MIN       = 6.0;
const float TREE_LEAF_FROND_MAX       = 16.0;
const float TREE_LEAF_FROND_JITTER    = 0.45;
const float TREE_LEAF_FROND_RAGGED    = 0.7;
const float TREE_LEAF_FROND_RISE      = 0.3;
const float TREE_LEAF_STEM_BLOCKS     = 0.024;
const float TREE_LEAF_STEM_TAPER      = 0.7;
const float TREE_LEAF_SPACING_SHARE   = 0.16;
const float TREE_LEAF_SPACING_MIN     = 0.12;
const float TREE_LEAF_SPACING_MAX     = 0.5;
const float TREE_LEAF_ANGLE_SIN       = 0.643;
const float TREE_LEAF_ANGLE_COS       = 0.766;
const float TREE_LEAF_FILL            = 1.45;
const float TREE_LEAF_LENGTH_MIN      = 0.1;
const float TREE_LEAF_LENGTH_MAX      = 1.6;
const float TREE_LEAF_LENGTH_SHARE    = 0.4;
const float TREE_LEAF_LENGTH_JITTER   = 0.3;
const float TREE_LEAF_WIDTH           = 0.38;
const float TREE_LEAF_TIP_POWER       = 3.0;
const float TREE_LEAF_WIDEN           = 0.6;
const float TREE_LEAF_GAP_ALPHA       = 0.5;
const float TREE_LEAF_GRAZE_MIN       = 0.06;
const float TREE_LEAF_GRAZE_MAX       = 0.3;
const float TREE_LEAF_STEM_SHADE      = 0.6;
const float TREE_LEAF_RIB_SHADE       = 0.82;
const float TREE_LEAF_RIB_WIDTH       = 0.25;
const float TREE_LEAF_DOME_BLEND      = 0.7;
const float TREE_LEAF_HEART_AO        = 0.5;
const float TREE_LEAF_UNDERSIDE_AO    = 0.55;
const float TREE_LEAF_SPECULAR        = 0.04;
const float TREE_LEAF_SEED_SPREAD     = 37.0;
const float TREE_LEAF_SOLID_SPACING   = 1.0;
const float TREE_LEAF_SOLID_RIB       = 0.16;
const float TREE_LEAF_PI              = 3.14159265359;

const int   TREE_LEAF_MISS            = 0;
const int   TREE_LEAF_BLADE           = 1;
const int   TREE_LEAF_STEM            = 2;

float hashTreeLeaf(float frond, float row, float seed) {
    return hash31(vec3(frond, row, seed * TREE_LEAF_SEED_SPREAD));
}

// One leaf leaving a frond's stem at a row on one side, tested at a point given along and away from the stem;
// rib is how far across the blade the point lies, 0 on its midrib and 1 at its edge
bool traceTreeLeafBlade(
    float along, float away, float row, float side, float frond, float reach, float halfArcPerBlock,
    float spacing, float longest, float widen, float seed, out float rib) {
    float base = (row + 0.5 + (side > 0.0 ? 0.0 : 0.5)) * spacing;

    rib = 1.0;

    if (base <= 0.0 || base >= reach)
    return false;

    float tip    = 1.0 - pow(base / reach, TREE_LEAF_TIP_POWER);
    float jitter = 1.0 - TREE_LEAF_LENGTH_JITTER * hashTreeLeaf(frond, row * 2.0 + side, seed);
    float extent = clamp(base * halfArcPerBlock * TREE_LEAF_FILL / TREE_LEAF_ANGLE_SIN,
        TREE_LEAF_LENGTH_MIN, longest) * tip * jitter;
    vec2  local  = vec2(along - base, away);
    float u      = local.x * TREE_LEAF_ANGLE_COS + local.y * TREE_LEAF_ANGLE_SIN;
    float v      = local.y * TREE_LEAF_ANGLE_COS - local.x * TREE_LEAF_ANGLE_SIN;

    if (u <= 0.0 || u >= extent + widen)
    return false;

    float halfWidth = extent * TREE_LEAF_WIDTH * sin(TREE_LEAF_PI * clamp(u / extent, 0.0, 1.0)) + widen;

    rib = abs(v) / max(halfWidth, 0.0001);

    return rib < 1.0;
}

// The way one frond leaves the card's centre and how far it reaches along it; a frond reaching down the card falls
// short by the underside share
float resolveTreeLeafFrond(float frond, float count, vec2 cardRadii, float underside, float seed, out vec2 axis) {
    float angle = (frond + (hashTreeLeaf(frond, -1.0, seed) - 0.5) * TREE_LEAF_FROND_JITTER) * TREE_LEAF_TWO_PI
                / count + seed * TREE_LEAF_TWO_PI;

    axis = vec2(cos(angle), sin(angle));

    return mix(TREE_LEAF_FROND_RAGGED, 1.0, hashTreeLeaf(frond, -2.0, seed))
         * mix(underside, 1.0, smoothstep(-1.0, TREE_LEAF_FROND_RISE, axis.y)) / length(axis / cardRadii);
}

// What one frond's sprig holds at a point of the card: a blade, its stem, or nothing
int traceTreeLeafFrond(
    vec2 point, float frond, float count, vec2 cardRadii, float spacing, float longest, float underside, float widen,
    float seed, out float rib) {
    vec2  axis;
    float reach  = resolveTreeLeafFrond(frond, count, cardRadii, underside, seed, axis);
    float along  = dot(point, axis);
    float across = dot(point, vec2(-axis.y, axis.x));
    float away   = abs(across);
    float side   = across >= 0.0 ? 1.0 : -1.0;

    rib = 1.0;

    if (along <= 0.0 || along >= reach + widen)
    return TREE_LEAF_MISS;

    float stem = TREE_LEAF_STEM_BLOCKS * (1.0 - TREE_LEAF_STEM_TAPER * along / reach);

    if (away < stem + widen)
    return widen < stem ? TREE_LEAF_STEM : TREE_LEAF_BLADE;

    float halfArcPerBlock = TREE_LEAF_PI / count;
    float row = floor((along - away * TREE_LEAF_ANGLE_COS / TREE_LEAF_ANGLE_SIN) / spacing
                - (side > 0.0 ? 0.5 : 1.0));

    if (traceTreeLeafBlade(
        along, away, row, side, frond, reach, halfArcPerBlock, spacing, longest, widen, seed, rib))
    return TREE_LEAF_BLADE;

    if (traceTreeLeafBlade(
        along, away, row + 1.0, side, frond, reach, halfArcPerBlock, spacing, longest, widen, seed, rib))
    return TREE_LEAF_BLADE;

    return TREE_LEAF_MISS;
}

// What the card holds at a point within the share of its rim it shows: the nearest frond's sprig, then the one
// beside it the point leans toward
int traceTreeLeafCard(
    vec2 point, vec2 cardRadii, float shown, float underside, float widen, float seed, out float rib) {
    vec2  rim        = point / cardRadii;
    float meanRadius = (cardRadii.x + cardRadii.y) * 0.5;
    float count      = clamp(floor(TREE_LEAF_TWO_PI * meanRadius / TREE_LEAF_FROND_ARC),
        TREE_LEAF_FROND_MIN, TREE_LEAF_FROND_MAX);
    float spacing    = clamp(meanRadius * TREE_LEAF_SPACING_SHARE, TREE_LEAF_SPACING_MIN, TREE_LEAF_SPACING_MAX);
    float longest    = min(TREE_LEAF_LENGTH_MAX, meanRadius * TREE_LEAF_LENGTH_SHARE);
    float sector     = (atan(point.y, point.x) / TREE_LEAF_TWO_PI - seed) * count;
    float nearest    = floor(sector + 0.5);
    float beside     = sector >= nearest ? nearest + 1.0 : nearest - 1.0;

    rib = 1.0;

    if (dot(rim, rim) > shown * shown)
    return TREE_LEAF_MISS;

    if (widen >= spacing * TREE_LEAF_SOLID_SPACING) {
        vec2  axis;
        float reach = resolveTreeLeafFrond(mod(nearest, count), count, cardRadii, underside, seed, axis);
        float along = dot(point, axis);

        rib = TREE_LEAF_SOLID_RIB;

        return along > 0.0 && along < reach + widen ? TREE_LEAF_BLADE : TREE_LEAF_MISS;
    }

    int kind = traceTreeLeafFrond(
        point, mod(nearest, count), count, cardRadii, spacing, longest, underside, widen, seed, rib);

    if (kind != TREE_LEAF_MISS)
    return kind;

    return traceTreeLeafFrond(
        point, mod(beside, count), count, cardRadii, spacing, longest, underside, widen, seed, rib);
}

// Every G-buffer target for one fragment of a leaf card — false where the card is clear; card is the fragment's
// corner on its card from -1 to 1, underside the share of the rim its downward fronds reach, and rotation turns the
// cluster's own axes into the world's
bool shadeTreeLeaf(
    vec3 worldPos, vec2 card, vec3 center, vec2 radii, vec2 cardRadii, float underside, float seed, vec2 uvOrigin,
    vec3 color, mat3 rotation, out vec4 albedoOut, out vec4 normalOut, out vec4 materialOut) {
    vec2  point  = card * cardRadii;
    float widen  = length(fwidth(point)) * TREE_LEAF_WIDEN;
    vec3  facing = normalize(cross(dFdx(worldPos), dFdy(worldPos)));
    vec2  gradX  = dFdx(point) * u_uvPerBlock;
    vec2  gradY  = dFdy(point) * u_uvPerBlock;
    vec3  camera = u_inverseView[3].xyz;
    float shown  = smoothstep(TREE_LEAF_GRAZE_MIN, TREE_LEAF_GRAZE_MAX,
        abs(dot(facing, normalize(camera - worldPos))));
    float rib;

    albedoOut   = vec4(0.0);
    normalOut   = vec4(0.0);
    materialOut = vec4(0.0);

    int kind = traceTreeLeafCard(point, cardRadii, shown, underside, widen, seed, rib);

    if (kind == TREE_LEAF_MISS)
    return false;

    vec4 albedo = textureGrad(u_textureArray,
        vec3(uvOrigin + fract(point) * u_uvPerBlock, float(u_layer_albedo)), gradX, gradY);

    if (albedo.a < TREE_LEAF_GAP_ALPHA)
    return false;

    if (dot(facing, camera - worldPos) < 0.0)
    facing = -facing;

    vec3  local  = transpose(rotation) * (worldPos - center) / vec3(radii.x, radii.y, radii.x);
    vec3  dome   = rotation * (local / max(length(local), 0.0001));
    vec3  normal = normalize(mix(facing, dome, TREE_LEAF_DOME_BLEND));
    float shade  = kind == TREE_LEAF_STEM
                 ? TREE_LEAF_STEM_SHADE
                 : mix(TREE_LEAF_RIB_SHADE, 1.0, smoothstep(0.0, TREE_LEAF_RIB_WIDTH, rib));
    float ao     = mix(TREE_LEAF_HEART_AO, 1.0, clamp(length(local), 0.0, 1.0))
                 * mix(TREE_LEAF_UNDERSIDE_AO, 1.0, dome.y * 0.5 + 0.5);

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    albedoOut   = vec4(albedo.rgb * color * shade, 1.0);
    normalOut   = vec4(normalize(mat3(u_view) * normal), 1.0);
    materialOut = vec4(resolveTreeSunVisibility(worldPos), TREE_LEAF_SPECULAR, ao, 1.0);

    return true;
}

#endif
