#ifndef MACRO_PAINT_GLSL
#define MACRO_PAINT_GLSL

#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"

// Distant macro terrain painted as a cartoon backdrop's mountains are, in its albedo alone, so the deferred light and
// fog fall on it as on any ground. Its facets step from their top color through one mixed band to their slope color
// in flat steps, grass capping the gentle ground and rock the steep; broad brush patches swing every facet a little
// lighter or darker, warmer or cooler; and steep rock is banded into wandering strata. Near the grid's rim, where a
// block still spans a few pixels, a block pattern shows as faintly as the voxel world it continues: each block its
// own slight shade, a thin line where block meets block across the tops and where layer meets layer up the slopes,
// fading out as blocks shrink toward a pixel, so it never shimmers. u_macroPaint scales the painting, the flat steps
// included (none at 0 blends smoothly from top to slope as before), and u_macroBlocks the block pattern. Every field
// is a pure function of world position, anchored on a period of MACRO_PAINT_PERIOD_CHUNKS chunks from the player's
// chunk, the map's pixel, which divides every world, so it wraps with the world and never moves as the player walks.
// The engine prepends a program's includes to every one of its stages, so nothing here touches what only a fragment
// stage has: the caller measures the footprint of the anchored position with fwidth and hands it in. The define must
// match EngineSetting.CHUNKS_PER_PIXEL.
#define MACRO_PAINT_PERIOD_CHUNKS 32

const float MACRO_PAINT_SLOPE_START   = 0.35;
const float MACRO_PAINT_SLOPE_END     = 0.75;
const float MACRO_PAINT_STEP_LOW      = 0.3;
const float MACRO_PAINT_STEP_HIGH     = 0.6;
const float MACRO_PAINT_STEP_MIX      = 0.45;
const float MACRO_PAINT_PATCH_BLOCKS  = 56.0;
const float MACRO_PAINT_DAB_BLOCKS    = 14.0;
const float MACRO_PAINT_DAB_SHARE     = 0.35;
const float MACRO_PAINT_PATCH_SHADE   = 0.1;
const float MACRO_PAINT_HUE_BLOCKS    = 112.0;
const float MACRO_PAINT_HUE_SHARE     = 0.5;
const vec3  MACRO_PAINT_WARM          = vec3(1.06, 1.0, 0.9);
const vec3  MACRO_PAINT_COOL          = vec3(0.93, 0.98, 1.07);
const float MACRO_PAINT_STRATA_BLOCKS = 6.0;
const float MACRO_PAINT_STRATA_WARP   = 4.0;
const float MACRO_PAINT_WARP_BLOCKS   = 28.0;
const float MACRO_PAINT_STRATA_SHADE  = 0.12;
const float MACRO_PAINT_STRATA_FADE   = 0.35;
const float MACRO_PAINT_EDGE_MIN      = 0.01;
const float MACRO_BLOCK_SHADE         = 0.05;
const float MACRO_BLOCK_LINE_WIDTH    = 0.08;
const float MACRO_BLOCK_LINE_SHADE    = 0.18;
const float MACRO_BLOCK_FADE_START    = 0.25;
const float MACRO_BLOCK_FADE_END      = 0.8;
const vec2  MACRO_PAINT_HUE_SEED      = vec2(211.0, 73.0);
const vec2  MACRO_PAINT_WARP_SEED     = vec2(37.0, 151.0);
const uint  MACRO_PAINT_HASH_X        = 0x8DA6B343u;
const uint  MACRO_PAINT_HASH_Y        = 0xD8163841u;
const uint  MACRO_PAINT_HASH_Z        = 0xCB1AB31Fu;
const uint  MACRO_PAINT_HASH_MIX      = 0x2C1B3C6Du;
const float MACRO_PAINT_HASH_SCALE    = 1.0 / 4294967295.0;

// ── Anchor ────────────────────────────────────────────────────────────────

int wrapMacroPaintChunk(int chunk) {
    return ((chunk % MACRO_PAINT_PERIOD_CHUNKS) + MACRO_PAINT_PERIOD_CHUNKS) % MACRO_PAINT_PERIOD_CHUNKS;
}

float resolveMacroPaintPeriod() {
    return float(MACRO_PAINT_PERIOD_CHUNKS) * u_chunkSize;
}

// A grid-relative position laid on the period-anchored plane the painting is read from
vec3 anchorMacroPaint(vec3 position) {
    vec2 origin = vec2(float(wrapMacroPaintChunk(u_playerChunkX)), float(wrapMacroPaintChunk(u_playerChunkZ)))
        * u_chunkSize;

    return vec3(position.x + origin.x, position.y, position.z + origin.y);
}

// ── Noise ─────────────────────────────────────────────────────────────────

// A cell's value from -1 to 1, its x and z folded into the period so the field wraps with the world
float hashMacroPaint(ivec3 cell, int cells) {
    uvec3 folded = uvec3(((cell.x % cells) + cells) % cells, cell.y, ((cell.z % cells) + cells) % cells);
    uint  h      = folded.x * MACRO_PAINT_HASH_X ^ folded.y * MACRO_PAINT_HASH_Y ^ folded.z * MACRO_PAINT_HASH_Z;

    h ^= h >> 15;
    h *= MACRO_PAINT_HASH_MIX;
    h ^= h >> 13;

    return float(h) * MACRO_PAINT_HASH_SCALE * 2.0 - 1.0;
}

// Smooth value noise from -1 to 1 over the anchored plane, its cell a whole share of the period
float sampleMacroPaintNoise(vec2 position, float cellBlocks) {
    float period = resolveMacroPaintPeriod();
    int   cells  = max(1, int(round(period / cellBlocks)));
    vec2  grid   = position * (float(cells) / period);
    ivec2 cell   = ivec2(floor(grid));
    vec2  f      = grid - vec2(cell);
    vec2  w      = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(hashMacroPaint(ivec3(cell.x, 0, cell.y), cells),
            hashMacroPaint(ivec3(cell.x + 1, 0, cell.y), cells), w.x),
        mix(hashMacroPaint(ivec3(cell.x, 0, cell.y + 1), cells),
            hashMacroPaint(ivec3(cell.x + 1, 0, cell.y + 1), cells), w.x),
        w.y);
}

// ── Paint ─────────────────────────────────────────────────────────────────

// How far a facet has turned from its top color to its slope color: in flat steps as paint rises, smoothly at none
float resolveMacroSlopeShare(float steepness) {
    float smoothShare = smoothstep(MACRO_PAINT_SLOPE_START, MACRO_PAINT_SLOPE_END, steepness);
    float stepShare   = mix(0.0, MACRO_PAINT_STEP_MIX, step(MACRO_PAINT_STEP_LOW, steepness))
        + (1.0 - MACRO_PAINT_STEP_MIX) * step(MACRO_PAINT_STEP_HIGH, steepness);

    return mix(smoothShare, stepShare, clamp(u_macroPaint, 0.0, 1.0));
}

// The broad brush patches: a little lighter or darker, warmer or cooler, patch by patch
vec3 resolveMacroBrush(vec2 position) {
    float patch = sampleMacroPaintNoise(position, MACRO_PAINT_PATCH_BLOCKS) * (1.0 - MACRO_PAINT_DAB_SHARE)
        + sampleMacroPaintNoise(position, MACRO_PAINT_DAB_BLOCKS) * MACRO_PAINT_DAB_SHARE;
    float hue   = sampleMacroPaintNoise(position + MACRO_PAINT_HUE_SEED, MACRO_PAINT_HUE_BLOCKS) * 0.5 + 0.5;
    vec3  tint  = mix(vec3(1.0), mix(MACRO_PAINT_COOL, MACRO_PAINT_WARM, hue), MACRO_PAINT_HUE_SHARE);

    return mix(vec3(1.0), tint * (1.0 + patch * MACRO_PAINT_PATCH_SHADE), u_macroPaint);
}

// The strata banding steep rock, lighter and darker layers wandering about the level, faded out before a band thins
// to a pixel
float resolveMacroStrata(vec3 position, float footprintY) {
    float warp  = sampleMacroPaintNoise(position.xz + MACRO_PAINT_WARP_SEED, MACRO_PAINT_WARP_BLOCKS);
    float band  = (position.y + warp * MACRO_PAINT_STRATA_WARP) / MACRO_PAINT_STRATA_BLOCKS;
    float edge  = max(footprintY / MACRO_PAINT_STRATA_BLOCKS, MACRO_PAINT_EDGE_MIN);
    float layer = smoothstep(0.5 - edge, 0.5 + edge, fract(band)) * 2.0 - 1.0;
    float fade  = 1.0 - smoothstep(MACRO_PAINT_STRATA_FADE * 0.5, MACRO_PAINT_STRATA_FADE, edge);

    return 1.0 + layer * MACRO_PAINT_STRATA_SHADE * fade * u_macroPaint;
}

// ── Blocks ────────────────────────────────────────────────────────────────

// How near a coordinate lies to a whole block, as a line of the given share of a block, cut cleanly over its footprint
float resolveMacroBlockLine(float coordinate, float footprint) {
    float gap       = 0.5 - abs(fract(coordinate) - 0.5);
    float halfWidth = MACRO_BLOCK_LINE_WIDTH * 0.5;

    return 1.0 - smoothstep(halfWidth - footprint * 0.5, halfWidth + footprint * 0.5, gap);
}

// The block pattern: each block its own slight shade, lines between the blocks of a top and the layers of a slope,
// shown only while a block spans a few pixels
float resolveMacroBlocks(vec3 position, vec3 footprint, float slopeShare) {
    float size  = max(max(footprint.x, footprint.z), footprint.y);
    float fade  = (1.0 - smoothstep(MACRO_BLOCK_FADE_START, MACRO_BLOCK_FADE_END, size)) * u_macroBlocks;

    if (fade <= 0.0)
    return 1.0;

    float cells = resolveMacroPaintPeriod();
    float shade = hashMacroPaint(ivec3(floor(position)), int(cells)) * MACRO_BLOCK_SHADE;
    float grid  = max(resolveMacroBlockLine(position.x, footprint.x), resolveMacroBlockLine(position.z, footprint.z));
    float layer = resolveMacroBlockLine(position.y, footprint.y);
    float line  = mix(grid, layer, slopeShare);

    return 1.0 + (shade - line * MACRO_BLOCK_LINE_SHADE) * fade;
}

// The albedo of a macro facet painted: its top and slope colors stepped by its steepness, brushed, and, on ground
// rather than a tree stand-in, banded into strata where it is rock and patterned into blocks near the rim
vec3 paintMacroGround(vec3 top, vec3 side, vec3 normal, vec3 position, vec3 footprint, bool ground) {
    float slopeShare = resolveMacroSlopeShare(1.0 - normal.y);
    vec3  albedo     = mix(top, side, slopeShare) * resolveMacroBrush(position.xz);

    if (!ground)
    return albedo;

    float strata = mix(1.0, resolveMacroStrata(position, footprint.y), slopeShare);

    return albedo * strata * resolveMacroBlocks(position, footprint, slopeShare);
}

#endif
