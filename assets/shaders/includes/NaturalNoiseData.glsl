#ifndef NATURAL_NOISE_DATA_GLSL
#define NATURAL_NOISE_DATA_GLSL

// Baked once on the CPU by NaturalNoiseSystem, from the exact hash formula
// this file used to evaluate live with sin() — every consumer now only ever
// reads this table, so physics sampling the same table on the CPU can never
// disagree with what gets rendered. PERIOD and SEED_SCALE must match
// EngineSetting.NATURAL_NOISE_LATTICE_PERIOD and NATURAL_NOISE_SEED_SCALE —
// GLSL has no visibility into Java constants.
#define NATURAL_NOISE_LATTICE_PERIOD 8
#define NATURAL_NOISE_SEED_SCALE 0.5

layout(std140) uniform NaturalNoiseData {
    vec4 u_naturalNoiseLattice[16];
};

float sampleNaturalNoiseLattice(int x, int z) {
    int wrappedX = ((x % NATURAL_NOISE_LATTICE_PERIOD) + NATURAL_NOISE_LATTICE_PERIOD) % NATURAL_NOISE_LATTICE_PERIOD;
    int wrappedZ = ((z % NATURAL_NOISE_LATTICE_PERIOD) + NATURAL_NOISE_LATTICE_PERIOD) % NATURAL_NOISE_LATTICE_PERIOD;
    int flatIndex = wrappedX * NATURAL_NOISE_LATTICE_PERIOD + wrappedZ;
    vec4 packedFour = u_naturalNoiseLattice[flatIndex / 4];
    int component = flatIndex - (flatIndex / 4) * 4;
    if (component == 0) return packedFour.x;
    if (component == 1) return packedFour.y;
    if (component == 2) return packedFour.z;
    return packedFour.w;
}

float sampleNaturalNoiseSmooth(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float h00 = sampleNaturalNoiseLattice(int(i.x),     int(i.y));
    float h10 = sampleNaturalNoiseLattice(int(i.x) + 1, int(i.y));
    float h01 = sampleNaturalNoiseLattice(int(i.x),     int(i.y) + 1);
    float h11 = sampleNaturalNoiseLattice(int(i.x) + 1, int(i.y) + 1);

    return mix(mix(h00, h10, f.x), mix(h01, h11, f.x), f.y);
}

#endif