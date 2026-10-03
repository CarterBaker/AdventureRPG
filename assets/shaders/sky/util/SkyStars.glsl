#ifndef SKY_STARS_GLSL
#define SKY_STARS_GLSL

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/CelestialData.glsl"
#include "sky/util/SkyBodies.glsl"

/*
 * The fixed stars. Every star keeps one place on the star sphere and the
 * sphere turns with the calendar, so constellations hold their shapes and
 * the sky can be steered by: the bright pole star stands still due north at
 * the observer's latitude, a dimmer one marks the south pole, and the rest
 * wheel around them through the night and drift with the seasons. Stars are
 * dealt from a cube of cells laid over the sphere — a dense field of faint
 * points and a sparse field of bright ones drawn as cartoon stars — each
 * with its own brightness, color and twinkle. Twinkling grows toward the
 * horizon where the air is thickest, faint stars drown first in moonlight,
 * and all of them fade as the sky brightens or clouds over.
 */

uniform sampler2D u_starTexture;

const float SKY_STAR_FOUR_OVER_PI       = 1.2732395;
const float SKY_STAR_QUARTER_PI         = 0.7853982;
const float SKY_STAR_TAU                = 6.2831853;
const float SKY_STAR_CELL_MARGIN        = 0.25;

const float SKY_STAR_FIELD_CELLS        = 80.0;
const float SKY_STAR_FIELD_DENSITY      = 0.34;
const float SKY_STAR_FIELD_RADIUS       = 0.0008;
const float SKY_STAR_FIELD_PIXEL_SHARE  = 0.6;
const float SKY_STAR_FIELD_FALLOFF      = 4.0;
const float SKY_STAR_FIELD_GAIN         = 1.4;
const float SKY_STAR_FIELD_SALT         = 0.0;

const float SKY_STAR_BRIGHT_CELLS       = 9.0;
const float SKY_STAR_BRIGHT_DENSITY     = 0.40;
const float SKY_STAR_BRIGHT_RADIUS_MIN  = 0.0075;
const float SKY_STAR_BRIGHT_RADIUS_MAX  = 0.0130;
const float SKY_STAR_BRIGHT_SALT        = 977.0;
const float SKY_STAR_BRIGHT_TILT        = 0.35;
const float SKY_STAR_BRIGHT_PULSE       = 0.12;
const float SKY_STAR_BRIGHT_GLOW        = 0.12;
const float SKY_STAR_BRIGHT_GLOW_SPREAD = 1.4;
const float SKY_STAR_BRIGHT_TINT        = 0.35;
const float SKY_STAR_BRIGHT_REACH       = 1.5;

const vec3  SKY_STAR_POLE_NORTH         = vec3(0.0, 0.0, 1.0);
const vec3  SKY_STAR_POLE_SOUTH         = vec3(0.0, 0.0, -1.0);
const float SKY_STAR_POLE_NORTH_RADIUS  = 0.0170;
const float SKY_STAR_POLE_SOUTH_RADIUS  = 0.0120;
const float SKY_STAR_POLE_NORTH_SEED    = 0.53;
const float SKY_STAR_POLE_SOUTH_SEED    = 0.21;

const float SKY_STAR_TWINKLE_SPEED_MIN  = 1.5;
const float SKY_STAR_TWINKLE_SPEED_MAX  = 4.5;
const float SKY_STAR_TWINKLE_ZENITH     = 0.18;
const float SKY_STAR_TWINKLE_HORIZON    = 0.65;
const float SKY_STAR_TWINKLE_HEIGHT     = 0.6;
const float SKY_STAR_TWINKLE_OVERTONE   = 2.37;

const float SKY_STAR_DAYLIGHT_START     = 0.02;
const float SKY_STAR_DAYLIGHT_END       = 0.28;
const float SKY_STAR_EXTINCTION_HEIGHT  = 0.22;
const float SKY_STAR_EXTINCTION_FLOOR   = 0.25;
const float SKY_STAR_MOON_WASHOUT       = 3.0;
const float SKY_STAR_OVERCAST_FADE      = 1.0;

const vec3  SKY_STAR_COLOR_HOT          = vec3(0.72, 0.82, 1.00);
const vec3  SKY_STAR_COLOR_MILD         = vec3(1.00, 0.98, 0.92);
const vec3  SKY_STAR_COLOR_COOL         = vec3(1.00, 0.78, 0.55);

// ── Visibility ─────────────────────────────────────────────────────────────

// How much starlight survives along a direction: none by day or under full
// cloud, thinned near the horizon where the light crosses the most air.
float resolveStarVisibility(vec3 dir) {
    float dark  = 1.0 - smoothstep(SKY_STAR_DAYLIGHT_START, SKY_STAR_DAYLIGHT_END, u_skyBlend.z);
    float clear = clamp(1.0 - u_skyBlend.w * SKY_STAR_OVERCAST_FADE, 0.0, 1.0);
    float air   = mix(SKY_STAR_EXTINCTION_FLOOR, 1.0, smoothstep(0.0, SKY_STAR_EXTINCTION_HEIGHT, dir.y));

    return dark * clear * air * resolveSkyBodyHorizon(dir);
}

// How strongly stars twinkle along a direction: most near the horizon.
float resolveStarTwinkleAmount(vec3 dir) {
    return mix(
        SKY_STAR_TWINKLE_HORIZON,
        SKY_STAR_TWINKLE_ZENITH,
        smoothstep(0.0, SKY_STAR_TWINKLE_HEIGHT, dir.y));
}

// ── Character ──────────────────────────────────────────────────────────────

float resolveStarTwinkle(float seed, float amount) {
    float speed = mix(SKY_STAR_TWINKLE_SPEED_MIN, SKY_STAR_TWINKLE_SPEED_MAX, fract(seed * 7.13));
    float phase = seed * SKY_STAR_TAU * 13.0;
    float wave  = 0.6 * sin(u_time * speed + phase)
                + 0.4 * sin(u_time * speed * SKY_STAR_TWINKLE_OVERTONE + phase * 1.91);

    return max(1.0 + amount * wave, 0.0);
}

vec3 resolveStarColor(float seed) {
    float temperature = fract(seed * 3.71);
    vec3  color       = mix(SKY_STAR_COLOR_HOT, SKY_STAR_COLOR_MILD, smoothstep(0.0, 0.35, temperature));

    return mix(color, SKY_STAR_COLOR_COOL, smoothstep(0.65, 1.0, temperature));
}

// ── Star Cube ──────────────────────────────────────────────────────────────

// The face of the star cube a sphere direction falls on (z) and where on that
// face (xy), in equal-angle coordinates from -1 to 1.
vec3 resolveStarCubeFace(vec3 sphereDir) {
    vec3 a = abs(sphereDir);
    vec3 face;

    if (a.x >= a.y && a.x >= a.z)
        face = vec3(sphereDir.yz / a.x, sphereDir.x > 0.0 ? 0.0 : 1.0);
    else if (a.y >= a.z)
        face = vec3(sphereDir.xz / a.y, sphereDir.y > 0.0 ? 2.0 : 3.0);
    else
        face = vec3(sphereDir.xy / a.z, sphereDir.z > 0.0 ? 4.0 : 5.0);

    return vec3(atan(face.xy) * SKY_STAR_FOUR_OVER_PI, face.z);
}

// The sphere direction at a place on a face of the star cube.
vec3 resolveStarCubeDirection(vec2 place, float face) {
    vec2  q    = tan(place * SKY_STAR_QUARTER_PI);
    float side = mod(face, 2.0) < 0.5 ? 1.0 : -1.0;

    if (face < 1.5)
        return normalize(vec3(side, q.x, q.y));

    if (face < 3.5)
        return normalize(vec3(q.x, side, q.y));

    return normalize(vec3(q.x, q.y, side));
}

// The star dealt to the cell of a face that a place falls in: its sphere
// direction (xyz) and its brightness (w), or zero brightness for an empty
// cell. The seed for its color and twinkle comes back through seed.
vec4 dealStar(vec3 face, float cells, float density, float salt, out float seed) {
    vec2  cell = floor((face.xy * 0.5 + 0.5) * cells);
    vec3  key  = vec3(cell, face.z * cells + salt);
    float roll = hash31(key);

    seed = hash31(key + 41.9);

    if (roll > density)
        return vec4(0.0);

    vec2 jitter = vec2(hash31(key + 11.3), hash31(key + 23.7));
    vec2 place  = (cell + SKY_STAR_CELL_MARGIN + (1.0 - 2.0 * SKY_STAR_CELL_MARGIN) * jitter) / cells * 2.0 - 1.0;

    return vec4(resolveStarCubeDirection(place, face.z), roll / density);
}

// ── Faint Field ────────────────────────────────────────────────────────────

// Light from the faint field along a sphere direction. A point narrower than
// a pixel is widened to one with its light kept, so it never flickers out
// between pixels.
vec3 resolveStarFieldLight(vec3 sphereDir, float pixelAngle, float twinkleAmount) {
    float seed;
    vec4  star = dealStar(
        resolveStarCubeFace(sphereDir), SKY_STAR_FIELD_CELLS, SKY_STAR_FIELD_DENSITY, SKY_STAR_FIELD_SALT, seed);

    if (star.w <= 0.0)
        return vec3(0.0);

    float magnitude = pow(star.w, SKY_STAR_FIELD_FALLOFF);
    float spread    = max(SKY_STAR_FIELD_RADIUS, pixelAngle * SKY_STAR_FIELD_PIXEL_SHARE);
    float energy    = SKY_STAR_FIELD_RADIUS / spread;
    float offset    = length(sphereDir - star.xyz) / spread;
    float washout   = clamp(1.0 - u_moonIntensity * SKY_STAR_MOON_WASHOUT * (1.0 - magnitude), 0.0, 1.0);

    return resolveStarColor(seed) * magnitude * energy * energy * exp(-offset * offset)
         * resolveStarTwinkle(seed, twinkleAmount) * washout * SKY_STAR_FIELD_GAIN;
}

// ── Bright Stars ───────────────────────────────────────────────────────────

// One cartoon star centred on a sphere direction, turned a little on its own
// seed, swelling and glowing with its twinkle.
vec3 drawStarPicture(
        vec3 color, vec3 dir, vec3 starSphereDir, float radius, float seed,
        float pixelAngle, float visibility, float twinkleAmount) {
    vec3  starDir = transpose(mat3(u_starRotation)) * starSphereDir;
    float twinkle = resolveStarTwinkle(seed, twinkleAmount);
    float size    = radius * (1.0 + SKY_STAR_BRIGHT_PULSE * (twinkle - 1.0));
    vec2  plane   = resolveSkyBodyPlane(dir, starDir);
    float reach   = length(plane) / size;

    if (reach > SKY_STAR_BRIGHT_GLOW_SPREAD * 2.0)
        return color;

    vec3 starColor = mix(vec3(1.0), resolveStarColor(seed), SKY_STAR_BRIGHT_TINT);

    color += starColor * SKY_STAR_BRIGHT_GLOW * twinkle * visibility
           * exp(-reach * reach / (SKY_STAR_BRIGHT_GLOW_SPREAD * SKY_STAR_BRIGHT_GLOW_SPREAD));

    if (reach > SKY_STAR_BRIGHT_REACH)
        return color;

    vec2 local   = rotateSkyBodyPlane(plane, (seed * 2.0 - 1.0) * SKY_STAR_BRIGHT_TILT) / size;
    vec4 picture = sampleSkyBodyPicture(u_starTexture, 0.5 + 0.5 * local, pixelAngle / (2.0 * size));

    return layerSkyBodyPicture(color, picture, starColor, visibility);
}

// The bright star of the cell a sphere direction falls in, then both poles.
vec3 drawBrightStars(
        vec3 color, vec3 dir, vec3 sphereDir, float pixelAngle, float visibility, float twinkleAmount) {
    float seed;
    vec4  star = dealStar(
        resolveStarCubeFace(sphereDir), SKY_STAR_BRIGHT_CELLS, SKY_STAR_BRIGHT_DENSITY, SKY_STAR_BRIGHT_SALT, seed);

    if (star.w > 0.0)
        color = drawStarPicture(
            color, dir, star.xyz, mix(SKY_STAR_BRIGHT_RADIUS_MIN, SKY_STAR_BRIGHT_RADIUS_MAX, star.w), seed,
            pixelAngle, visibility, twinkleAmount);

    color = drawStarPicture(
        color, dir, SKY_STAR_POLE_NORTH, SKY_STAR_POLE_NORTH_RADIUS, SKY_STAR_POLE_NORTH_SEED,
        pixelAngle, visibility, twinkleAmount);

    return drawStarPicture(
        color, dir, SKY_STAR_POLE_SOUTH, SKY_STAR_POLE_SOUTH_RADIUS, SKY_STAR_POLE_SOUTH_SEED,
        pixelAngle, visibility, twinkleAmount);
}

// ── Stars ──────────────────────────────────────────────────────────────────

vec3 drawStars(vec3 color, vec3 dir, float pixelAngle) {
    float visibility = resolveStarVisibility(dir);

    if (visibility <= 0.0)
        return color;

    vec3  sphereDir     = normalize(mat3(u_starRotation) * dir);
    float twinkleAmount = resolveStarTwinkleAmount(dir);

    color += resolveStarFieldLight(sphereDir, pixelAngle, twinkleAmount) * visibility;

    return drawBrightStars(color, dir, sphereDir, pixelAngle, visibility, twinkleAmount);
}

#endif
