#ifndef MACRO_PAINT_GLSL
#define MACRO_PAINT_GLSL

#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"

// The colors of distant macro terrain, painted as a cartoon backdrop's hills and mountains are; the lighting pass then
// lights it as one (see postprocessing/lighting/includes/PaintedLighting.glsl). Ground turns from its top color to its
// slope color by steepness, but along an edge that wanders in soft blobs across the facets rather than following
// them, so grass spills down a gentle spur and rock breaks out through a meadow as broad painted shapes. Meadows carry
// large soft patches a little sunnier or shadier than their color; rock is cooled toward the blue grey of painted
// stone; and high rock pales toward the cream of a sunlit peak. Every field is a pure function of world position,
// anchored on a period of MACRO_PAINT_PERIOD_CHUNKS chunks from the player's chunk, the map's pixel, which divides
// every world, so it wraps with the world and never moves as the player walks; nothing here needs a fragment stage, so
// any stage may include it. The define must match EngineSetting.CHUNKS_PER_PIXEL, and the peak heights sit within
// the reach of the highest mountain biomes above EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS.
#define MACRO_PAINT_PERIOD_CHUNKS 32

const float MACRO_PAINT_SLOPE_EDGE      = 0.42;
const float MACRO_PAINT_SLOPE_SOFTNESS  = 0.07;
const float MACRO_PAINT_EDGE_BLOCKS     = 44.0;
const float MACRO_PAINT_EDGE_DETAIL     = 13.0;
const float MACRO_PAINT_EDGE_WANDER     = 0.14;
const float MACRO_PAINT_EDGE_RIPPLE     = 0.05;
const float MACRO_PAINT_MEADOW_BLOCKS   = 72.0;
const float MACRO_PAINT_MEADOW_EDGE     = 0.12;
const float MACRO_PAINT_MEADOW_SOFTNESS = 0.1;
const vec3  MACRO_PAINT_MEADOW_SUN      = vec3(1.14, 1.12, 0.82);
const vec3  MACRO_PAINT_MEADOW_SHADE    = vec3(0.84, 0.9, 0.9);
const float MACRO_PAINT_MEADOW_SHARE    = 0.7;
const vec3  MACRO_PAINT_ROCK_COOL       = vec3(0.9, 0.97, 1.1);
const float MACRO_PAINT_ROCK_BLOCKS     = 36.0;
const float MACRO_PAINT_ROCK_SHADE      = 0.08;
const float MACRO_PAINT_PEAK_START      = 400.0;
const float MACRO_PAINT_PEAK_END        = 540.0;
const float MACRO_PAINT_PEAK_WANDER     = 50.0;
const vec3  MACRO_PAINT_PEAK_COLOR      = vec3(0.92, 0.87, 0.78);
const float MACRO_PAINT_PEAK_SHARE      = 0.75;
const vec2  MACRO_PAINT_MEADOW_SEED     = vec2(211.0, 73.0);
const vec2  MACRO_PAINT_ROCK_SEED       = vec2(37.0, 151.0);
const vec2  MACRO_PAINT_PEAK_SEED       = vec2(97.0, 19.0);
const uint  MACRO_PAINT_HASH_X          = 0x8DA6B343u;
const uint  MACRO_PAINT_HASH_Y          = 0xD8163841u;
const uint  MACRO_PAINT_HASH_MIX        = 0x2C1B3C6Du;
const float MACRO_PAINT_HASH_SCALE      = 1.0 / 4294967295.0;

// ── Anchor ────────────────────────────────────────────────────────────────

int wrapMacroPaintChunk(int chunk) {
    return ((chunk % MACRO_PAINT_PERIOD_CHUNKS) + MACRO_PAINT_PERIOD_CHUNKS) % MACRO_PAINT_PERIOD_CHUNKS;
}

// A grid-relative position laid on the period-anchored plane the painting is read from
vec3 anchorMacroPaint(vec3 position) {
    vec2 origin = vec2(float(wrapMacroPaintChunk(u_playerChunkX)), float(wrapMacroPaintChunk(u_playerChunkZ)))
        * u_chunkSize;

    return vec3(position.x + origin.x, position.y, position.z + origin.y);
}

// ── Noise ─────────────────────────────────────────────────────────────────

// A lattice corner's value from -1 to 1, its cell folded into the period so the field wraps with the world
float hashMacroPaint(ivec2 cell, int cells) {
    uvec2 folded = uvec2(((cell % cells) + cells) % cells);
    uint  h      = folded.x * MACRO_PAINT_HASH_X ^ folded.y * MACRO_PAINT_HASH_Y;

    h ^= h >> 15;
    h *= MACRO_PAINT_HASH_MIX;
    h ^= h >> 13;

    return float(h) * MACRO_PAINT_HASH_SCALE * 2.0 - 1.0;
}

// Smooth value noise from -1 to 1 over the anchored plane, its cell a whole share of the period
float sampleMacroPaintNoise(vec2 position, float cellBlocks) {
    float period = float(MACRO_PAINT_PERIOD_CHUNKS) * u_chunkSize;
    int   cells  = max(1, int(round(period / cellBlocks)));
    vec2  grid   = position * (float(cells) / period);
    ivec2 cell   = ivec2(floor(grid));
    vec2  f      = grid - vec2(cell);
    vec2  w      = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(hashMacroPaint(cell, cells), hashMacroPaint(cell + ivec2(1, 0), cells), w.x),
        mix(hashMacroPaint(cell + ivec2(0, 1), cells), hashMacroPaint(cell + ivec2(1, 1), cells), w.x),
        w.y);
}

// ── Paint ─────────────────────────────────────────────────────────────────

// How far ground has turned from its top color to its slope color: by steepness, along an edge that wanders in soft
// blobs across the facets
float resolveMacroSlopeShare(float steepness, vec2 position) {
    float wander = sampleMacroPaintNoise(position, MACRO_PAINT_EDGE_BLOCKS) * MACRO_PAINT_EDGE_WANDER
        + sampleMacroPaintNoise(position, MACRO_PAINT_EDGE_DETAIL) * MACRO_PAINT_EDGE_RIPPLE;

    return smoothstep(MACRO_PAINT_SLOPE_EDGE - MACRO_PAINT_SLOPE_SOFTNESS,
        MACRO_PAINT_SLOPE_EDGE + MACRO_PAINT_SLOPE_SOFTNESS, steepness + wander);
}

// A meadow's color in its soft patches, a little sunnier in some and shadier in others
vec3 paintMacroMeadow(vec3 top, vec2 position) {
    float blot  = sampleMacroPaintNoise(position + MACRO_PAINT_MEADOW_SEED, MACRO_PAINT_MEADOW_BLOCKS);
    float sunny = smoothstep(MACRO_PAINT_MEADOW_EDGE - MACRO_PAINT_MEADOW_SOFTNESS,
        MACRO_PAINT_MEADOW_EDGE + MACRO_PAINT_MEADOW_SOFTNESS, blot);
    float shady = smoothstep(MACRO_PAINT_MEADOW_EDGE - MACRO_PAINT_MEADOW_SOFTNESS,
        MACRO_PAINT_MEADOW_EDGE + MACRO_PAINT_MEADOW_SOFTNESS, -blot);
    vec3  tint  = mix(mix(vec3(1.0), MACRO_PAINT_MEADOW_SUN, sunny), MACRO_PAINT_MEADOW_SHADE, shady);

    return top * mix(vec3(1.0), tint, MACRO_PAINT_MEADOW_SHARE);
}

// A rock face's color cooled toward painted stone, shaded a little in broad patches, and paling toward a cream peak
// high up
vec3 paintMacroRock(vec3 side, vec3 position) {
    float shade = 1.0 + sampleMacroPaintNoise(position.xz + MACRO_PAINT_ROCK_SEED, MACRO_PAINT_ROCK_BLOCKS)
        * MACRO_PAINT_ROCK_SHADE;
    float lift  = sampleMacroPaintNoise(position.xz + MACRO_PAINT_PEAK_SEED, MACRO_PAINT_EDGE_BLOCKS)
        * MACRO_PAINT_PEAK_WANDER;
    float peak  = smoothstep(MACRO_PAINT_PEAK_START, MACRO_PAINT_PEAK_END, position.y + lift);

    return mix(side * MACRO_PAINT_ROCK_COOL * shade, MACRO_PAINT_PEAK_COLOR, peak * MACRO_PAINT_PEAK_SHARE);
}

// The albedo of a macro facet: ground painted meadow and rock, blended by its wandering slope edge; a tree stand-in
// keeps its own two colors, turned by steepness alone
vec3 paintMacroGround(vec3 top, vec3 side, vec3 normal, vec3 position, bool ground) {
    float steepness = 1.0 - normal.y;

    if (!ground)
    return mix(top, side, smoothstep(MACRO_PAINT_SLOPE_EDGE - MACRO_PAINT_SLOPE_SOFTNESS,
        MACRO_PAINT_SLOPE_EDGE + MACRO_PAINT_SLOPE_SOFTNESS, steepness));

    return mix(paintMacroMeadow(top, position.xz), paintMacroRock(side, position),
        resolveMacroSlopeShare(steepness, position.xz));
}

#endif
