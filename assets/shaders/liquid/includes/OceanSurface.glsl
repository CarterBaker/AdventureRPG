#ifndef OCEAN_SURFACE_GLSL
#define OCEAN_SURFACE_GLSL

#include "includes/OceanData.glsl"
#include "includes/NoiseUtility.glsl"

/*
 * The GPU side of WaveManager, kept formula for formula with OceanWaveUtility so a tessellated vertex and a
 * gameplay query at the same position agree on where the sea stands. Sea state is weather turbulence plus a
 * calm floor, scaled by a drifting periodic noise (how much wave this patch of sea is raising right now) and
 * by the exposure of the water (a pond is never more than glassy). It splits into short chop and long swell,
 * and every wave is a crest-sharpened sine with a zero mean, so peaks are pointed and troughs are broad.
 * Ripples, foam breakup and caustics are visual layers that ride the same world-anchored lattice as the sea
 * noise, an exact multiple of it, so they stay put while the grid's reference chunk moves and close up
 * seamlessly across the world's wrap.
 */

struct OceanState {
    float seaState;
    float chop;
    float swell;
};

const float OCEAN_RIPPLE_LATTICE_SCALE   = 64.0;
const float OCEAN_RIPPLE_DETAIL_SCALE    = 160.0;
const float OCEAN_RIPPLE_DETAIL_SPEED    = 0.35;
const float OCEAN_RIPPLE_PROBE_BLOCKS    = 0.2;
const float OCEAN_RIPPLE_CALM_STRENGTH   = 0.12;
const float OCEAN_RIPPLE_STATE_STRENGTH  = 0.06;
const float OCEAN_RIPPLE_STATE_MAX       = 4.0;
const uint  OCEAN_RIPPLE_SEED            = 3571u;
const uint  OCEAN_RIPPLE_DETAIL_SEED     = 6007u;

// ── Fields ────────────────────────────────────────────────────────────────

float sampleOceanTurbulence(vec2 pos) {
    float weightedStrength = u_oceanSurface.z;
    float totalWeight      = 1.0;
    int   cellCount        = min(u_oceanTurbulenceCount, OCEAN_TURBULENCE_MAX_ENTRIES);

    for (int i = 0; i < cellCount; i++) {
        vec4  cell = u_oceanTurbulenceCells[i];
        float t    = 1.0 - length(pos - cell.xy) / cell.z;

        if (t <= 0.0)
        continue;

        float influence = t * t * (3.0 - 2.0 * t) * cell.w;
        float strength  = u_oceanTurbulenceStrengths[i / OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR]
                                                    [i % OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR];

        weightedStrength += strength * influence;
        totalWeight      += influence;
    }

    return weightedStrength / totalWeight;
}

float readOceanExposure(int x, int z) {
    int index = z * OCEAN_EXPOSURE_GRID_SIZE + x;
    return u_oceanExposure[index / OCEAN_EXPOSURE_VALUES_PER_VECTOR][index % OCEAN_EXPOSURE_VALUES_PER_VECTOR];
}

float sampleOceanExposure(vec2 pos) {
    float maxCell = float(OCEAN_EXPOSURE_GRID_SIZE - 1);
    vec2  cell    = clamp((pos - u_oceanExposureGrid.xy) / u_oceanExposureGrid.z - 0.5, 0.0, maxCell);
    ivec2 cell0   = ivec2(floor(cell));
    ivec2 cell1   = min(cell0 + 1, ivec2(OCEAN_EXPOSURE_GRID_SIZE - 1));
    vec2  t       = cell - vec2(cell0);

    float top    = mix(readOceanExposure(cell0.x, cell0.y), readOceanExposure(cell1.x, cell0.y), t.x);
    float bottom = mix(readOceanExposure(cell0.x, cell1.y), readOceanExposure(cell1.x, cell1.y), t.x);

    return mix(top, bottom, t.y);
}

// The sea noise lattice at a position: world-anchored, drifting downwind, periodic over the world.
vec2 resolveOceanLattice(vec2 pos) {
    return pos * u_oceanNoise.zw + u_oceanNoise.xy;
}

float sampleOceanSeaNoise(vec2 pos) {
    vec2  lattice = resolveOceanLattice(pos);
    float coarse  = periodicGradientNoise2D(lattice, u_oceanNoisePeriod, OCEAN_SEA_NOISE_SEED);
    float detail  = periodicGradientNoise2D(
        lattice * 2.0, u_oceanNoisePeriod * 2.0, OCEAN_SEA_NOISE_SEED + OCEAN_NOISE_OCTAVE_SEED_STEP);

    float noise   = (coarse + detail * u_oceanNoiseShape.w) / (1.0 + u_oceanNoiseShape.w);
    float noise01 = clamp(noise * u_oceanNoiseShape.z + 0.5, 0.0, 1.0);

    return mix(u_oceanNoiseShape.x, u_oceanNoiseShape.y, noise01);
}

OceanState resolveOceanState(vec2 pos) {
    OceanState state;

    state.seaState = (u_oceanSeaState.x + sampleOceanTurbulence(pos)) * sampleOceanSeaNoise(pos)
        * sampleOceanExposure(pos);
    state.chop     = min(state.seaState * u_oceanWaveScale.x, u_oceanWaveScale.y);
    state.swell    = clamp((state.seaState - u_oceanSeaState.y) * u_oceanWaveScale.z, 0.0, u_oceanWaveScale.w);

    return state;
}

// ── Waves ─────────────────────────────────────────────────────────────────

float oceanWaveShape(float theta) {
    return (exp(u_oceanSeaState.z * (sin(theta) - 1.0)) - u_oceanSeaState.w) / (1.0 - u_oceanSeaState.w);
}

float oceanWaveShapeSlope(float theta) {
    return u_oceanSeaState.z * cos(theta) * exp(u_oceanSeaState.z * (sin(theta) - 1.0))
        / (1.0 - u_oceanSeaState.w);
}

float oceanWaveAmplitude(int waveIndex, OceanState state) {
    return (waveIndex < OCEAN_WAVE_SWELL_COUNT ? state.swell : state.chop) * u_oceanWaves[waveIndex].w;
}

float sampleOceanDisplacement(vec2 pos, OceanState state) {
    float displacement = 0.0;

    for (int i = 0; i < OCEAN_WAVE_COUNT; i++) {
        vec4 wave = u_oceanWaves[i];
        displacement += oceanWaveAmplitude(i, state) * oceanWaveShape(dot(wave.xy, pos) + wave.z);
    }

    return displacement;
}

vec2 sampleOceanSlope(vec2 pos, OceanState state) {
    vec2 slope = vec2(0.0);

    for (int i = 0; i < OCEAN_WAVE_COUNT; i++) {
        vec4 wave = u_oceanWaves[i];
        slope += wave.xy * (oceanWaveAmplitude(i, state) * oceanWaveShapeSlope(dot(wave.xy, pos) + wave.z));
    }

    return slope;
}

float resolveOceanWhitecap(OceanState state, float displacement) {
    float crest = displacement / max(state.chop + state.swell, u_oceanWhitecap.w);

    return smoothstep(u_oceanWhitecap.x, u_oceanWhitecap.y, state.seaState)
        * smoothstep(u_oceanWhitecap.z, 1.0, crest);
}

// ── Hulls ─────────────────────────────────────────────────────────────────

float readOceanHullHalfBeam(int base, int station) {
    return u_oceanHulls[base + OCEAN_HULL_HEADER_VECTORS + station / 4][station % 4];
}

// True where a position lies inside the dry hull of a vehicle, which keeps the sea out of it.
bool isInsideOceanHull(vec3 pos) {
    int hullCount = min(u_oceanHullCount, OCEAN_HULL_MAX_ENTRIES);

    for (int i = 0; i < hullCount; i++) {
        int  base   = i * OCEAN_HULL_VECTORS_PER_ENTRY;
        vec4 point  = vec4(pos, 1.0);
        vec3 mask   = vec3(dot(u_oceanHulls[base], point), dot(u_oceanHulls[base + 1], point),
            dot(u_oceanHulls[base + 2], point));
        vec4 extent = u_oceanHulls[base + 3];

        if (mask.x < 0.0 || mask.x > extent.x || mask.y < extent.y || mask.y > extent.z)
        continue;

        float station  = clamp(mask.x / extent.x * float(OCEAN_HULL_STATIONS) - 0.5,
            0.0, float(OCEAN_HULL_STATIONS - 1));
        int   first    = int(floor(station));
        int   second   = min(first + 1, OCEAN_HULL_STATIONS - 1);
        float halfBeam = mix(readOceanHullHalfBeam(base, first), readOceanHullHalfBeam(base, second),
            station - float(first));

        if (abs(mask.z) < halfBeam)
        return true;
    }

    return false;
}

// ── Ripples ───────────────────────────────────────────────────────────────

float sampleOceanRipple(vec2 pos) {
    vec2  lattice = resolveOceanLattice(pos);
    vec2  across  = normalize(vec2(-u_oceanWaves[0].y, u_oceanWaves[0].x));
    vec2  drift   = across * (u_oceanSurface.w * OCEAN_RIPPLE_DETAIL_SPEED);

    float broad   = periodicGradientNoise2D(
        lattice * OCEAN_RIPPLE_LATTICE_SCALE, u_oceanNoisePeriod * OCEAN_RIPPLE_LATTICE_SCALE, OCEAN_RIPPLE_SEED);
    float fine    = periodicGradientNoise2D(
        lattice * OCEAN_RIPPLE_DETAIL_SCALE + drift, u_oceanNoisePeriod * OCEAN_RIPPLE_DETAIL_SCALE,
        OCEAN_RIPPLE_DETAIL_SEED);

    return broad + fine * 0.5;
}

// Surface slope of the wind ripples, which only ever bend the normal. Even a sheltered pond keeps a faint
// ripple, and the ripples strengthen with the sea state.
vec2 sampleOceanRippleSlope(vec2 pos, float seaState) {
    float center   = sampleOceanRipple(pos);
    float alongX   = sampleOceanRipple(pos + vec2(OCEAN_RIPPLE_PROBE_BLOCKS, 0.0)) - center;
    float alongZ   = sampleOceanRipple(pos + vec2(0.0, OCEAN_RIPPLE_PROBE_BLOCKS)) - center;
    float strength = OCEAN_RIPPLE_CALM_STRENGTH
        + OCEAN_RIPPLE_STATE_STRENGTH * min(seaState, OCEAN_RIPPLE_STATE_MAX);

    return vec2(alongX, alongZ) * (strength / OCEAN_RIPPLE_PROBE_BLOCKS);
}

vec3 slopeToNormal(vec2 slope) {
    return normalize(vec3(-slope.x, 1.0, -slope.y));
}

#endif
