#ifndef MACRO_CRINKLE_GLSL
#define MACRO_CRINKLE_GLSL

#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"

// The crinkle that roughens distant macro terrain, moved in the vertex shader so it costs nothing per pixel: three
// octaves of value noise nudge every vertex, the ground straight up and down, a tree stand-in a little every way,
// so the faceted ground and crowns break into finer, less regular facets. The noise is a pure function of world
// position, so two tiles that share an edge move it alike and no crack opens, and a skirt hanging from an edge moves
// with it. It repeats every MACRO_CRINKLE_PERIOD_CHUNKS chunks, the map's pixel, which divides every world, so it
// wraps with the world; positions are anchored on that period the way NaturalNoiseData anchors its own, from the
// player's chunk, so the field never moves as the player walks. u_macroCrinkle scales its height and
// u_macroCrinkleSize its cells, each cell count held whole within the period. The define must match
// EngineSetting.CHUNKS_PER_PIXEL.
#define MACRO_CRINKLE_PERIOD_CHUNKS 32

const vec3  MACRO_CRINKLE_CELLS      = vec3(128.0, 32.0, 8.0);
const vec3  MACRO_CRINKLE_AMPLITUDES = vec3(1.8, 0.8, 0.35);
const float MACRO_CRINKLE_TREE_SHARE = 0.6;
const vec2  MACRO_CRINKLE_PLANE_SEED = vec2(57.0, 113.0);
const uint  MACRO_CRINKLE_HASH_X     = 0x8DA6B343u;
const uint  MACRO_CRINKLE_HASH_Y     = 0xD8163841u;
const uint  MACRO_CRINKLE_HASH_MIX   = 0x2C1B3C6Du;
const float MACRO_CRINKLE_HASH_SCALE = 1.0 / 4294967295.0;

int wrapMacroCrinkleChunk(int chunk) {
    return ((chunk % MACRO_CRINKLE_PERIOD_CHUNKS) + MACRO_CRINKLE_PERIOD_CHUNKS) % MACRO_CRINKLE_PERIOD_CHUNKS;
}

// A lattice corner's value from -1 to 1, its cell folded into the period so the field wraps with the world
float hashMacroCrinkle(ivec2 cell, int cells) {
    uvec2 folded = uvec2(((cell % cells) + cells) % cells);
    uint  h      = folded.x * MACRO_CRINKLE_HASH_X ^ folded.y * MACRO_CRINKLE_HASH_Y;

    h ^= h >> 15;
    h *= MACRO_CRINKLE_HASH_MIX;
    h ^= h >> 13;

    return float(h) * MACRO_CRINKLE_HASH_SCALE * 2.0 - 1.0;
}

// One octave over the period-anchored plane, its cell a whole share of the period
float sampleMacroCrinkleOctave(vec2 position, float cellBlocks, float period) {
    int   cells = max(1, int(round(period / cellBlocks)));
    vec2  grid  = position * (float(cells) / period);
    ivec2 cell  = ivec2(floor(grid));
    vec2  f     = grid - vec2(cell);
    vec2  w     = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(hashMacroCrinkle(cell, cells), hashMacroCrinkle(cell + ivec2(1, 0), cells), w.x),
        mix(hashMacroCrinkle(cell + ivec2(0, 1), cells), hashMacroCrinkle(cell + ivec2(1, 1), cells), w.x),
        w.y);
}

float sampleMacroCrinkle(vec2 position, float period) {
    vec3 cells = MACRO_CRINKLE_CELLS * u_macroCrinkleSize;

    return sampleMacroCrinkleOctave(position, cells.x, period) * MACRO_CRINKLE_AMPLITUDES.x
        + sampleMacroCrinkleOctave(position, cells.y, period) * MACRO_CRINKLE_AMPLITUDES.y
        + sampleMacroCrinkleOctave(position, cells.z, period) * MACRO_CRINKLE_AMPLITUDES.z;
}

// A grid-relative position moved by the crinkle: the ground along its height, a tree stand-in by three planes
vec3 applyMacroCrinkle(vec3 position, bool tree) {
    if (u_macroCrinkle <= 0.0)
    return position;

    float period   = float(MACRO_CRINKLE_PERIOD_CHUNKS) * u_chunkSize;
    vec2  origin   = vec2(
        float(wrapMacroCrinkleChunk(u_playerChunkX)),
        float(wrapMacroCrinkleChunk(u_playerChunkZ))) * u_chunkSize;
    vec3  anchored = vec3(position.x + origin.x, position.y, position.z + origin.y);

    if (!tree)
    return position + vec3(0.0, sampleMacroCrinkle(anchored.xz, period) * u_macroCrinkle, 0.0);

    vec3 offset = vec3(
        sampleMacroCrinkle(vec2(anchored.z, anchored.y) + MACRO_CRINKLE_PLANE_SEED.x, period),
        sampleMacroCrinkle(anchored.xz, period),
        sampleMacroCrinkle(vec2(anchored.x, anchored.y) + MACRO_CRINKLE_PLANE_SEED.y, period));

    return position + offset * (u_macroCrinkle * MACRO_CRINKLE_TREE_SHARE);
}

#endif
