#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL
#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"

/*
* Puff-sphere cloud system — toon shading with day/night rim inversion.
 *
 * Noise model
 * -----------
 * Classic Perlin FBM replaced with a cellular "puff field": 3-D space is
 * divided into a regular grid; each cell spawns one sphere at a jittered
 * centre and contributes a quintic radial falloff.  Overlapping spheres
 * accumulate additively, so adjacent puffs merge into the rounded lobes of
 * real cumulus clouds rather than blending into abstract noise streaks.
 * Four octaves run at scale ratios ~2.3× (irrational multiples break period
 * alignment) so the rounded circle character is visible at every size.
 *
 * Day / night rim inversion
 * -------------------------
 * Day   – core is bright (direct sunlight); rim is a subtle warm edge,
 *          kept dimmer than the body.  rimStrength → 0.28.
 * Night – core is dark (no direct source); the rim becomes the primary
 *          visible feature — scattered moonlight / sky-glow traces each
 *          puff as a cool blue-white halo.  rimStrength → 0.92.
 * Both coreCol and rimCol are fully derived from u_skyZenithColor and
 * u_skyHorizonColor so they respond correctly to sunrises, sunsets, and
 * any weather tint applied upstream.
 *
 * Horizon-stain fix (unchanged)
 * ------------------------------------
 * Fine/fast layers have their alpha scaled by mix(floor, 1, horizonBlend)
 * with a progressively lower floor, so near dir.y = 0 only the large slow
 * base layers contribute.  Prevents fine shadow-edges from coffee-staining
 * the merged haze near the horizon.
 */

const float CLOUD_WIND_SPEED    = 0.003;
const float CLOUD_SOFTNESS      = 0.13;
const float CLOUD_COVERAGE_FREQ = 0.00025;
const float CLOUD_COVERAGE_MIN  = 0.50;
const float CLOUD_COVERAGE_MAX  = 0.85;

// ── Puff-cell noise ──────────────────────────────────────────────────────────

// Scalar hash (kept for potential external use)
float _cHash(vec3 p) {
    p  = fract(p * vec3(127.1, 311.7, 74.7));
    p += dot(p, p.zyx + 19.19);
    return fract((p.x + p.y) * p.z);
}

// Vector hash — maps each grid cell to a unique jitter offset in [0,1)³.
// Three independent dot products ensure the three output channels are
// uncorrelated, so puff centres scatter in all directions uniformly.
vec3 _cHash3(vec3 p) {
    p  = fract(p * vec3(127.1, 311.7, 74.7));
    p += dot(p, p.yxz + 19.19);
    return fract(vec3((p.x + p.y) * p.z,
            (p.y + p.z) * p.x,
            (p.z + p.x) * p.y));
}

// One puff-field octave.
//
// The 3×3×3 neighbourhood ensures no puff is missed even when its jittered
// centre has crossed a cell boundary.  The quintic falloff
//   w = 6t⁵ − 15t⁴ + 10t³   (t = 1 − dist/r, clamped to [0,1])
// has zero first AND second derivative at the boundary, eliminating the
// subtle halos produced by cubic or linear falloffs.
//
// Additive accumulation: two overlapping puffs produce a single smoothly
// merged mass, mirroring how real cumulus cells grow together.
float _puffCell(vec3 p, float r) {
    vec3  ip = floor(p);
    float d  = 0.0;
    for (int x = -1; x <= 1; x++)
    for (int y = -1; y <= 1; y++)
    for (int z = -1; z <= 1; z++) {
        vec3 n   = ip + vec3(x, y, z);
        vec3 ctr = n + 0.5 + (_cHash3(n) - 0.5) * 0.65;   // 65% jitter
        float t  = clamp(1.0 - length(p - ctr) / r, 0.0, 1.0);
        d += t * t * t * (t * (t * 6.0 - 15.0) + 10.0);   // quintic
    }
    return clamp(d, 0.0, 1.0);
}

// FBM over the puff field.
//
// Amplitude schedule:  0.55 → 0.264 → 0.127 → 0.061  (sum ≈ 1.0)
// Scale ratios: ×2.30, ×2.22, ×2.22  — slightly irrational to break
// any residual periodicity while preserving the circular puff character
// at every octave.  Puff radius shrinks each octave so fine-scale puffs
// are more distinctly separated, adding visible bubble texture at edges.
float _puffFbm(vec3 p) {
    float v = 0.0, a = 0.55;
    v += a * _puffCell(p,        0.80); a *= 0.48;   // mass silhouette
    v += a * _puffCell(p * 2.30, 0.72); a *= 0.48;   // secondary lobes
    v += a * _puffCell(p * 5.10, 0.62); a *= 0.48;   // surface bubbling
    v += a * _puffCell(p * 11.3, 0.52);               // fine edge fuzz
    return v;
}

// ── Sky-driven colour palette ─────────────────────────────────────────────────

void _cloudColors(float dayFactor, float horizonWarmth,
    out vec3 shadowCol, out vec3 coreCol, out vec3 rimCol) {
    float hLum = dot(u_skyHorizonColor, vec3(0.2126, 0.7152, 0.0722));
    float zLum = dot(u_skyZenithColor,  vec3(0.2126, 0.7152, 0.0722));

    // ── Core ─────────────────────────────────────────────────────────────────
    // Day  : near-white, nudged warm by the horizon sky tint at golden hour.
    // Night: very dark, derived from zenith so a deep-blue night sky gives
    //        dark-blue clouds rather than neutral black.
    vec3 dayCoreCol   = mix(vec3(0.95, 0.96, 1.00),
        u_skyHorizonColor * 1.15, horizonWarmth * 0.50);
    vec3 nightCoreCol = u_skyZenithColor * 0.18 + vec3(0.02, 0.02, 0.06);
    coreCol           = mix(nightCoreCol, dayCoreCol, dayFactor);

    // ── Shadow ───────────────────────────────────────────────────────────────
    // Day  : blue-grey self-shadow, blended with the zenith tint.
    // Night: near-black, just a faint zenith tint so clouds don't disappear
    //        entirely against a dark sky.
    vec3 dayShadow   = mix(u_skyZenithColor * 0.45, vec3(0.38, 0.42, 0.50), 0.45);
    vec3 nightShadow = u_skyZenithColor * 0.08 + vec3(0.01, 0.01, 0.03);
    shadowCol        = mix(nightShadow, dayShadow, dayFactor);

    // ── Rim ──────────────────────────────────────────────────────────────────
    // Day  : warm edge, intentionally dimmer than the core so it doesn't
    //        pop on an already-bright cloud body.
    vec3 dayRimCol = mix(vec3(1.00, 0.98, 0.95),
        u_skyHorizonColor * 1.25, horizonWarmth * 0.50);
    dayRimCol *= 0.65 + hLum * 0.35;

    // Night: the primary visible feature — scattered moonlight / atmospheric
    //        glow traces each puff as a cool blue-white halo.
    //        Sky zenith luminance drives the overall brightness so a bright
    //        full-moon sky produces noticeably more luminous cloud edges.
    //        The horizonWarmth term blends in a warm city-glow / twilight
    //        tint when the horizon is warmer than the zenith.
    //        Values may exceed 1.0 to allow bloom in HDR pipelines.
    vec3 nightRimCol = mix(vec3(0.62, 0.68, 0.88),
        u_skyHorizonColor * 1.70, horizonWarmth * 0.35);
    nightRimCol *= 1.10 + zLum * 1.50;
    nightRimCol  = clamp(nightRimCol, 0.0, 1.8);   // mild HDR headroom

    rimCol = mix(nightRimCol, dayRimCol, dayFactor);
}

// ── Cloud layer helpers ───────────────────────────────────────────────────────

float cloudCoverageBreath(float seed) {
    return mix(CLOUD_COVERAGE_MIN, CLOUD_COVERAGE_MAX,
        fbmNoise2D(vec2(u_time * CLOUD_COVERAGE_FREQ + seed, 11.7 + seed)));
}

float cloudHeightMask(float y, float peak, float fadeEnd) {
    if (y < 0.0) return 0.0;
    return min(smoothstep(0.0, peak, y), 1.0 - smoothstep(peak, fadeEnd, y));
}

vec4 _cloudOver(vec4 dst, vec4 src) {
    float a  = src.a + dst.a * (1.0 - src.a);
    vec3 rgb = (src.rgb * src.a + dst.rgb * dst.a * (1.0 - src.a))
    / max(a, 1.0e-5);
    return vec4(rgb, a);
}

vec4 cloudLayer(vec3 dir, float scale, float speed,
    float peak, float fadeEnd, float seed) {
    float hMask = cloudHeightMask(dir.y, peak, fadeEnd);
    if (hMask <= 0.001) return vec4(0.0);

    float wind = u_time * speed;
    vec3  p    = dir * scale
    + vec3(wind        + seed,
        seed * 0.31 + wind * 0.13,
        wind * 0.41 + seed * 0.73);

    float coverage  = cloudCoverageBreath(seed);
    float n         = _puffFbm(p);
    float threshold = 1.0 - coverage;
    float density   = smoothstep(threshold - CLOUD_SOFTNESS,
        threshold + CLOUD_SOFTNESS, n) * hMask;

    if (density <= 0.001) return vec4(0.0);

    float zenithLum     = dot(u_skyZenithColor, vec3(0.2126, 0.7152, 0.0722));
    float dayFactor     = clamp(zenithLum * 2.5, 0.0, 1.0);
    float horizonWarmth = clamp(
        (u_skyHorizonColor.r - u_skyHorizonColor.b) * 2.0, 0.0, 1.0) * dayFactor;

    // Toon bands.
    // Night floors are kept non-zero so the dark bands retain visible
    // depth rather than crushing to black, but they stay well below the
    // day values to preserve contrast in the illuminated puffs.
    float toonDay, toonNight;
    if      (density < 0.18) {
        toonDay = 0.68; toonNight = 0.60;
    }
    else if (density < 0.36) {
        toonDay = 0.76; toonNight = 0.42;
    }
    else if (density < 0.55) {
        toonDay = 0.84; toonNight = 0.26;
    }
    else if (density < 0.75) {
        toonDay = 0.92; toonNight = 0.13;
    }
    else {
        toonDay = 1.00; toonNight = 0.05;
    }
    float toon = mix(toonNight, toonDay, dayFactor);

    // Rim band: peaks in the mid-density transition zone.
    float rim = smoothstep(0.12, 0.46, density)
    * (1.0 - smoothstep(0.46, 0.86, density));
    rim = pow(rim, 0.60);

    // ── Day/night rim strength inversion ─────────────────────────────────────
    // Night (dayFactor = 0): rimStrength = 0.92 — rim dominates over dark core,
    //                        tracing each puff with a luminous halo.
    // Day   (dayFactor = 1): rimStrength = 0.28 — rim barely registers against
    //                        the already-bright cloud body.
    float rimStrength = mix(0.92, 0.28, dayFactor);

    vec3 shadowCol, coreCol, rimCol;
    _cloudColors(dayFactor, horizonWarmth, shadowCol, coreCol, rimCol);

    vec3 color = mix(shadowCol, coreCol, toon);
    color      = mix(color, rimCol, rim * rimStrength);

    float alpha = smoothstep(0.14, 0.52, density) * hMask * 0.90;
    return vec4(color, alpha);
}

// ── Public API ────────────────────────────────────────────────────────────────

vec4 calculateClouds(vec3 dir, float dailySeed) {
    float horizonBlend = smoothstep(0.0, 0.12, dir.y);

    vec4 result = vec4(0.0);

    // Large slow base — nearly unaffected at all heights
    vec4 a = cloudLayer(dir, 1.0, CLOUD_WIND_SPEED * 0.30, 0.02, 0.22, dailySeed * 1.7);
    result  = _cloudOver(result, a);

    // Primary mid-scale — slightly pulled back near the horizon
    vec4 b = cloudLayer(dir, 1.9, CLOUD_WIND_SPEED * 0.55, 0.05, 0.32, dailySeed * 3.1);
    b.a    *= mix(0.60, 1.00, horizonBlend);
    result  = _cloudOver(result, b);

    // Medium detail — significantly suppressed at bottom, full above
    vec4 c = cloudLayer(dir, 3.2, CLOUD_WIND_SPEED * 1.00, 0.09, 0.42, dailySeed * 7.7  + 19.0);
    c.a    *= mix(0.25, 1.00, horizonBlend);
    result  = _cloudOver(result, c);

    // Fine detail — barely present near horizon, phases in above
    vec4 d = cloudLayer(dir, 5.1, CLOUD_WIND_SPEED * 1.50, 0.14, 0.50, dailySeed * 13.3 + 41.0);
    d.a    *= mix(0.08, 0.80, horizonBlend);
    result  = _cloudOver(result, d);

    // Very fine wisps — invisible at horizon, subtle above
    vec4 e = cloudLayer(dir, 8.0, CLOUD_WIND_SPEED * 2.10, 0.18, 0.56, dailySeed * 23.7 + 77.0);
    e.a    *= mix(0.00, 0.55, horizonBlend);
    result  = _cloudOver(result, e);

    return result;
}
#endif