#ifndef WATER_LIGHT_GLSL
#define WATER_LIGHT_GLSL

#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/OceanData.glsl"
#include "includes/NoiseUtility.glsl"

/*
 * Light and color for water, shared by the surface and the underwater pass. Everything is lit by the same
 * sun, moon and sky the deferred pass uses, so water follows the time of day: turquoise shallows and deep
 * blue at noon, warm at sunset, dark slate under the moon. Direct light is cel banded into a few flat steps
 * with thin soft edges, the depth palette steps from shallow to mid to deep the same way, and light under the
 * surface dims with depth into a fog tinted by what little sky reaches it. Caustics are a drifting cellular
 * pattern on the same world-anchored lattice as the sea noise, kept to bright cartoon lines.
 */

const vec3  WATER_SHALLOW_COLOR        = vec3(0.22, 0.80, 0.76);
const vec3  WATER_MID_COLOR            = vec3(0.07, 0.50, 0.66);
const vec3  WATER_DEEP_COLOR           = vec3(0.02, 0.16, 0.36);
const vec3  WATER_FOAM_COLOR           = vec3(0.96, 0.99, 1.00);
const vec3  WATER_ABSORPTION           = vec3(0.42, 0.13, 0.08);
const vec3  WATER_MOON_TINT            = vec3(0.58, 0.74, 1.00);
const float WATER_MID_DEPTH_BLOCKS     = 2.5;
const float WATER_DEEP_DEPTH_BLOCKS    = 9.0;
const float WATER_DEPTH_BAND_SOFTNESS  = 0.6;
const float WATER_AMBIENT_ZENITH_SHARE = 0.55;
const float WATER_AMBIENT_SCALE        = 0.75;
const float WATER_AMBIENT_FLOOR        = 0.035;
const float WATER_MOON_MAX_INTENSITY   = 0.18;
const float WATER_CEL_SHADOW           = 0.35;
const float WATER_CEL_MID              = 0.72;
const float WATER_CEL_LOW_EDGE         = 0.08;
const float WATER_CEL_HIGH_EDGE        = 0.55;
const float WATER_CEL_SOFTNESS         = 0.04;

const float UNDERWATER_DENSITY          = 0.045;
const float UNDERWATER_DEPTH_DIMMING    = 0.035;
const float UNDERWATER_MIN_LIGHT        = 0.18;
const vec3  UNDERWATER_TINT             = vec3(0.55, 0.85, 0.95);

const float CAUSTIC_LATTICE_SCALE       = 96.0;
const float CAUSTIC_JITTER              = 0.42;
const float CAUSTIC_SPEED               = 0.6;
const float CAUSTIC_LINE_WIDTH          = 0.09;
const float CAUSTIC_STRENGTH            = 0.55;
const float CAUSTIC_DEPTH_BLOCKS        = 7.0;
const uint  CAUSTIC_SEED                = 4093u;
const float CAUSTIC_TWO_PI              = 6.2831853;

// ── Light ─────────────────────────────────────────────────────────────────

float resolveWaterCelBand(float lambert) {
    float low  = smoothstep(WATER_CEL_LOW_EDGE - WATER_CEL_SOFTNESS, WATER_CEL_LOW_EDGE + WATER_CEL_SOFTNESS,
        lambert);
    float high = smoothstep(WATER_CEL_HIGH_EDGE - WATER_CEL_SOFTNESS, WATER_CEL_HIGH_EDGE + WATER_CEL_SOFTNESS,
        lambert);

    return mix(WATER_CEL_SHADOW, mix(WATER_CEL_MID, 1.0, high), low);
}

vec3 resolveWaterAmbient() {
    vec3 sky = mix(u_skyHorizonColor, u_skyZenithColor, WATER_AMBIENT_ZENITH_SHARE);

    return sky * WATER_AMBIENT_SCALE + vec3(WATER_AMBIENT_FLOOR);
}

vec3 resolveWaterDirectLight(vec3 normal) {
    vec3  sunDir   = normalize(u_sunDirection);
    vec3  moonDir  = normalize(u_moonDirection);
    float sunBand  = resolveWaterCelBand(max(dot(normal, sunDir), 0.0));
    float moonBand = resolveWaterCelBand(max(dot(normal, moonDir), 0.0));
    float moonInt  = min(u_moonIntensity, WATER_MOON_MAX_INTENSITY);

    return u_sunColor * (u_sunIntensity * sunBand) + u_moonColor * WATER_MOON_TINT * (moonInt * moonBand);
}

vec3 resolveWaterLight(vec3 normal) {
    return resolveWaterAmbient() + resolveWaterDirectLight(normal);
}

// How directly sun and moon reach a surface, 0 to 1, for the atmospheric fog weighting.
float resolveWaterLitAmount(vec3 normal) {
    float sunDiff  = max(dot(normal, normalize(u_sunDirection)), 0.0);
    float moonDiff = max(dot(normal, normalize(u_moonDirection)), 0.0);

    return clamp(sunDiff + moonDiff * 0.5, 0.0, 1.0);
}

// ── Palette ───────────────────────────────────────────────────────────────

vec3 resolveWaterBodyColor(float depthBlocks) {
    float mid  = smoothstep(WATER_MID_DEPTH_BLOCKS - WATER_DEPTH_BAND_SOFTNESS,
        WATER_MID_DEPTH_BLOCKS + WATER_DEPTH_BAND_SOFTNESS, depthBlocks);
    float deep = smoothstep(WATER_DEEP_DEPTH_BLOCKS - WATER_DEPTH_BAND_SOFTNESS,
        WATER_DEEP_DEPTH_BLOCKS + WATER_DEPTH_BAND_SOFTNESS, depthBlocks);

    return mix(mix(WATER_SHALLOW_COLOR, WATER_MID_COLOR, mid), WATER_DEEP_COLOR, deep);
}

vec3 resolveWaterTransmittance(float thicknessBlocks) {
    return exp(-WATER_ABSORPTION * max(thicknessBlocks, 0.0));
}

// ── Underwater ────────────────────────────────────────────────────────────

float resolveUnderwaterLight(float depthBlocks) {
    return max(exp(-depthBlocks * UNDERWATER_DEPTH_DIMMING), UNDERWATER_MIN_LIGHT);
}

vec3 resolveUnderwaterFogColor(float depthBlocks) {
    vec3 light = resolveWaterAmbient() + u_sunColor * u_sunIntensity * WATER_CEL_MID
        + u_moonColor * WATER_MOON_TINT * min(u_moonIntensity, WATER_MOON_MAX_INTENSITY);

    return mix(WATER_MID_COLOR, WATER_DEEP_COLOR, 1.0 - resolveUnderwaterLight(depthBlocks))
        * light * resolveUnderwaterLight(depthBlocks);
}

vec3 applyUnderwaterFog(vec3 color, float distanceBlocks, float depthBlocks) {
    vec3  tinted     = color * UNDERWATER_TINT * resolveUnderwaterLight(depthBlocks);
    float visibility = exp(-distanceBlocks * UNDERWATER_DENSITY);

    return mix(resolveUnderwaterFogColor(depthBlocks), tinted, visibility);
}

// ── Caustics ──────────────────────────────────────────────────────────────

// Distance between the nearest two jittered points of a periodic cellular lattice; each point circles its
// cell over time, so the pattern swims without ever repeating short of the lattice period.
float sampleCausticEdge(vec2 lattice, vec2 period, float time) {
    vec2  cell     = floor(lattice);
    vec2  local    = lattice - cell;
    float nearest  = 8.0;
    float second   = 8.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2  offset = vec2(float(x), float(y));
            vec2  hash   = periodicGradient2D(mod(cell + offset, period), CAUSTIC_SEED);
            vec2  phase  = time * CAUSTIC_SPEED + hash * CAUSTIC_TWO_PI;
            vec2  point  = offset + 0.5 + vec2(sin(phase.x), cos(phase.y)) * CAUSTIC_JITTER - local;
            float dist   = dot(point, point);

            if (dist < nearest) {
                second  = nearest;
                nearest = dist;
            } else if (dist < second) {
                second = dist;
            }
        }
    }

    return sqrt(second) - sqrt(nearest);
}

// Bright caustic lines on a floor position, fading out with the water depth above it.
float sampleWaterCaustics(vec2 lattice, float depthBlocks) {
    float edge  = sampleCausticEdge(lattice * CAUSTIC_LATTICE_SCALE, u_oceanNoisePeriod * CAUSTIC_LATTICE_SCALE,
        u_oceanSurface.w);
    float lines = 1.0 - smoothstep(CAUSTIC_LINE_WIDTH * 0.5, CAUSTIC_LINE_WIDTH, edge);
    float reach = 1.0 - smoothstep(0.0, CAUSTIC_DEPTH_BLOCKS, depthBlocks);

    return lines * reach * CAUSTIC_STRENGTH * u_sunIntensity;
}

#endif
