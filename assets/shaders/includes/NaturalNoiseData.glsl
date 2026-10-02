#ifndef NATURAL_NOISE_DATA_GLSL
#define NATURAL_NOISE_DATA_GLSL

#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"

// Baked once on the CPU by NaturalNoiseSystem, four independent channels per cell in [-1, 1], laid out x * PERIOD
// + z exactly as NaturalNoiseUtility stores them, so physics sampling the same table can never disagree with what
// gets rendered. The lattice spans NATURAL_NOISE_PERIOD_CHUNKS chunks, which divides every world, so the field
// wraps with the world. Positions here are relative to the player's chunk corner, the same space u_gridPosition
// places every chunk in; folding the player's chunk into the period anchors them on the lattice, so the field is
// a pure function of world position and never moves as the player crosses a chunk. The defines must match
// EngineSetting.NATURAL_NOISE_LATTICE_PERIOD, NATURAL_NOISE_PERIOD_CHUNKS and NATURAL_NOISE_CELL_BLOCKS — GLSL
// has no visibility into Java constants.
#define NATURAL_NOISE_LATTICE_PERIOD 32
#define NATURAL_NOISE_PERIOD_CHUNKS 4
#define NATURAL_NOISE_CELL_BLOCKS 2.0

layout(std140) uniform NaturalNoiseData {
    vec4 u_naturalNoiseLattice[NATURAL_NOISE_LATTICE_PERIOD * NATURAL_NOISE_LATTICE_PERIOD];
};

vec4 fetchNaturalNoise(float cellA, float cellB) {
    int a = int(mod(cellA, float(NATURAL_NOISE_LATTICE_PERIOD)));
    int b = int(mod(cellB, float(NATURAL_NOISE_LATTICE_PERIOD)));
    return u_naturalNoiseLattice[a * NATURAL_NOISE_LATTICE_PERIOD + b];
}

// All four channels at once, periodic on both lattice axes, with a quintic fade so every field built on it is
// smooth to its second derivative
vec4 sampleNaturalNoise(vec2 lattice) {
    vec2 cell = floor(lattice);
    vec2 f    = lattice - cell;
    vec2 w    = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);

    vec4 n00 = fetchNaturalNoise(cell.x,       cell.y);
    vec4 n10 = fetchNaturalNoise(cell.x + 1.0, cell.y);
    vec4 n01 = fetchNaturalNoise(cell.x,       cell.y + 1.0);
    vec4 n11 = fetchNaturalNoise(cell.x + 1.0, cell.y + 1.0);

    return mix(mix(n00, n10, w.x), mix(n01, n11, w.x), w.y);
}

int wrapNaturalNoiseChunk(int chunk) {
    return ((chunk % NATURAL_NOISE_PERIOD_CHUNKS) + NATURAL_NOISE_PERIOD_CHUNKS) % NATURAL_NOISE_PERIOD_CHUNKS;
}

// Lattice coordinates of a player-relative position
vec3 toNaturalNoiseLattice(vec3 position) {
    vec2 origin = vec2(
        float(wrapNaturalNoiseChunk(u_playerChunkX)),
        float(wrapNaturalNoiseChunk(u_playerChunkZ))) * u_chunkSize;

    return vec3(position.x + origin.x, position.y, position.z + origin.y) / NATURAL_NOISE_CELL_BLOCKS;
}

#endif
