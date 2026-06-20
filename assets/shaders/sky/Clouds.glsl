#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL
#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"

/*
 * Seamless horizon clouds with toon shading and rim glow.
 *
 * Seam fix: the original used atan(dir.z, dir.x) to derive a 2-D azimuth UV,
 * which has a hard discontinuity where the angle wraps from +pi to -pi. Noise
 * sampled across that boundary jumps, producing a visible vertical stripe.
 * Fix: sample noise directly in 3-D on the view-direction vector. A unit-sphere
 * direction is naturally continuous at every angle — no wrap, no seam.
 */

const float CLOUD_WIND_SPEED    = 0.003;
const float CLOUD_SOFTNESS      = 0.13;
const float CLOUD_COVERAGE_FREQ = 0.00025;
const float CLOUD_COVERAGE_MIN  = 0.22;
const float CLOUD_COVERAGE_MAX  = 0.70;

float _cHash(vec3 p) {
    p  = fract(p * vec3(127.1, 311.7, 74.7));
    p += dot(p, p.zyx + 19.19);
    return fract((p.x + p.y) * p.z);
}

float _cNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(_cHash(i),               _cHash(i + vec3(1,0,0)), f.x),
            mix(_cHash(i + vec3(0,1,0)), _cHash(i + vec3(1,1,0)), f.x), f.y),
        mix(mix(_cHash(i + vec3(0,0,1)), _cHash(i + vec3(1,0,1)), f.x),
            mix(_cHash(i + vec3(0,1,1)), _cHash(i + vec3(1,1,1)), f.x), f.y),
        f.z);
}

float _cFbm(vec3 p) {
    float v = 0.0, a = 0.50, fr = 1.0;
    for (int i = 0; i < 5; i++) {
        v  += a * _cNoise(p * fr);
        a  *= 0.50;
        fr *= 2.10;
    }
    return v;
}

float cloudCoverageBreath(float seed) {
    return mix(CLOUD_COVERAGE_MIN, CLOUD_COVERAGE_MAX,
               fbmNoise2D(vec2(u_time * CLOUD_COVERAGE_FREQ + seed, 11.7 + seed)));
}

float cloudHeightMask(float y, float peak, float fadeEnd) {
    if (y < 0.0) return 0.0;
    return min(smoothstep(0.0, peak, y), 1.0 - smoothstep(peak, fadeEnd, y));
}

vec4 cloudLayer(vec3 dir, float scale, float speed, float peak, float fadeEnd, float seed) {

    float hMask = cloudHeightMask(dir.y, peak, fadeEnd);
    if (hMask <= 0.001) return vec4(0.0);

    float wind = u_time * speed;
    vec3 p = dir * scale
           + vec3(wind        + seed,
                  seed * 0.31 + wind * 0.13,
                  wind * 0.41 + seed * 0.73);

    float coverage = cloudCoverageBreath(seed);
    float n        = _cFbm(p);
    float density  = smoothstep(coverage - CLOUD_SOFTNESS,
                                coverage + CLOUD_SOFTNESS, n) * hMask;
    if (density <= 0.001) return vec4(0.0);

    float toon;
    if      (density < 0.35) toon = 0.28;
    else if (density < 0.65) toon = 0.60;
    else                      toon = 1.00;

    float rim = smoothstep(0.10, 0.42, density)
              * (1.0 - smoothstep(0.42, 0.82, density));
    rim = pow(rim, 0.55);

    vec3 shadowCol = mix(u_skyHorizonColor * 0.48,
                         vec3(0.46, 0.50, 0.57), 0.45);
    vec3 coreCol   = mix(vec3(0.94, 0.96, 1.00),
                         u_skyZenithColor * 0.08 + vec3(0.90), 0.18);
    vec3 rimCol    = mix(vec3(1.00, 1.00, 1.00),
                         u_skyHorizonColor, 0.42) * 1.55;

    vec3 color = mix(shadowCol, coreCol, toon);
    color      = mix(color, rimCol, rim * 0.68);

    float alpha = smoothstep(0.08, 0.48, density) * hMask * 0.87;

    return vec4(color, alpha);
}

vec4 calculateClouds(vec3 dir, float dailySeed) {
    vec4 result = vec4(0.0);

    vec4 a = cloudLayer(dir, 1.8, CLOUD_WIND_SPEED * 0.50, 0.04, 0.28, dailySeed * 3.1);
    result  = mix(result, a, a.a);

    vec4 b  = cloudLayer(dir, 2.9, CLOUD_WIND_SPEED,        0.08, 0.38, dailySeed * 7.7  + 19.0);
    result  = mix(result, b, b.a);

    vec4 c  = cloudLayer(dir, 4.3, CLOUD_WIND_SPEED * 1.55, 0.13, 0.47, dailySeed * 13.3 + 41.0);
    result  = mix(result, c, c.a * 0.62);

    return result;
}

#endif