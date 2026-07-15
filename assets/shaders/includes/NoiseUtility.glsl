#ifndef NOISE_UTILITY_GLSL
#define NOISE_UTILITY_GLSL

// Shared noise primitives for every sky, weather, and cloud shader.
// Value noise stays in place for the existing sky background variation;
// everything below it is the cloud-facing toolkit — gradient noise, fbm,
// ridged fbm, Worley/cellular noise, a Perlin-Worley hybrid for billowy
// cumulus shapes, and curl-based domain warp.

// ── Hashing ──────────────────────────────────────────────────────────────

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 hash33(vec3 p) {
    p = vec3(
        dot(p, vec3(127.1, 311.7, 74.7)),
        dot(p, vec3(269.5, 183.3, 246.1)),
        dot(p, vec3(113.5, 271.9, 124.6)));
    return fract(sin(p) * 43758.5453123) * 2.0 - 1.0;
}

// ── Shared math ─────────────────────────────────────────────────────────

float remapClamped(float value, float oldLow, float oldHigh, float newLow, float newHigh) {
    float t = (value - oldLow) / max(oldHigh - oldLow, 0.0001);
    return clamp(newLow + t * (newHigh - newLow), newLow, newHigh);
}

// ── Value noise (legacy path — sky background variation only) ───────────

const mat3 noiseBasis = mat3(0.00, 0.80, 0.60, -0.80, 0.36, -0.48, -0.60, -0.48, 0.64);

float smoothNoise3D(vec3 p) {
    p = noiseBasis * p;
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);

    float n000 = hash31(i + vec3(0, 0, 0));
    float n100 = hash31(i + vec3(1, 0, 0));
    float n010 = hash31(i + vec3(0, 1, 0));
    float n110 = hash31(i + vec3(1, 1, 0));
    float n001 = hash31(i + vec3(0, 0, 1));
    float n101 = hash31(i + vec3(1, 0, 1));
    float n011 = hash31(i + vec3(0, 1, 1));
    float n111 = hash31(i + vec3(1, 1, 1));

    float nx00 = mix(n000, n100, u.x);
    float nx10 = mix(n010, n110, u.x);
    float nx01 = mix(n001, n101, u.x);
    float nx11 = mix(n011, n111, u.x);

    float nxy0 = mix(nx00, nx10, u.y);
    float nxy1 = mix(nx01, nx11, u.y);

    return mix(nxy0, nxy1, u.z);
}

float fbmNoise3D(vec3 p) {
    float n = 0.0;
    n += 0.5   * smoothNoise3D(p);
    n += 0.25  * smoothNoise3D(p * 2.0);
    n += 0.125 * smoothNoise3D(p * 4.0);
    return n;
}

float fbmNoise2D(vec2 p) {
    return fbmNoise3D(vec3(p, 0.0));
}

// ── Gradient (Perlin-style) noise ────────────────────────────────────────

float gradientNoise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);

    float n000 = dot(hash33(i + vec3(0.0, 0.0, 0.0)), f - vec3(0.0, 0.0, 0.0));
    float n100 = dot(hash33(i + vec3(1.0, 0.0, 0.0)), f - vec3(1.0, 0.0, 0.0));
    float n010 = dot(hash33(i + vec3(0.0, 1.0, 0.0)), f - vec3(0.0, 1.0, 0.0));
    float n110 = dot(hash33(i + vec3(1.0, 1.0, 0.0)), f - vec3(1.0, 1.0, 0.0));
    float n001 = dot(hash33(i + vec3(0.0, 0.0, 1.0)), f - vec3(0.0, 0.0, 1.0));
    float n101 = dot(hash33(i + vec3(1.0, 0.0, 1.0)), f - vec3(1.0, 0.0, 1.0));
    float n011 = dot(hash33(i + vec3(0.0, 1.0, 1.0)), f - vec3(0.0, 1.0, 1.0));
    float n111 = dot(hash33(i + vec3(1.0, 1.0, 1.0)), f - vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, u.x);
    float nx10 = mix(n010, n110, u.x);
    float nx01 = mix(n001, n101, u.x);
    float nx11 = mix(n011, n111, u.x);

    float nxy0 = mix(nx00, nx10, u.y);
    float nxy1 = mix(nx01, nx11, u.y);

    return mix(nxy0, nxy1, u.z);
}

// Normalized against total amplitude so octave count doesn't shift the
// output range, then rebiased into [0,1].
float fbmGradient3D(vec3 p, int octaves, float lacunarity, float gain) {
    float sum = 0.0;
    float norm = 0.0;
    float amp = 0.5;
    vec3 pos = p;

    for (int i = 0; i < octaves; i++) {
        sum += amp * gradientNoise3D(pos);
        norm += amp;
        pos *= lacunarity;
        amp *= gain;
    }

    return clamp((sum / max(norm, 0.0001)) * 0.5 + 0.5, 0.0, 1.0);
}

float fbmGradient3D(vec3 p) {
    return fbmGradient3D(p, 5, 2.02, 0.5);
}

// Ridged multifractal — sharp bright ridges, dark valleys. Reserved for
// wispy/torn-edge detail work in the shape passes.
float ridgedFbm3D(vec3 p, int octaves, float lacunarity, float gain) {
    float sum = 0.0;
    float norm = 0.0;
    float amp = 0.5;
    vec3 pos = p;

    for (int i = 0; i < octaves; i++) {
        float n = 1.0 - abs(gradientNoise3D(pos));
        n *= n;
        sum += amp * n;
        norm += amp;
        pos *= lacunarity;
        amp *= gain;
    }

    return clamp(sum / max(norm, 0.0001), 0.0, 1.0);
}

// ── Worley (cellular) noise ───────────────────────────────────────────────
// F1 samples only the current cell plus whichever neighbor each axis
// leans toward (2x2x2 = 8 taps) rather than the full 3x3x3 = 27 — jitter
// is bounded within each cell so the shortcut's error stays sub-cell-sized.
// Returns billowy values: ~1 at cell centers, ~0 at cell borders.

float worleyNoise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 dir = sign(f - 0.5);

    float minDistSq = 1.0;

    for (int z = 0; z <= 1; z++) {
        for (int y = 0; y <= 1; y++) {
            for (int x = 0; x <= 1; x++) {
                vec3 neighbor = vec3(float(x), float(y), float(z)) * dir;
                vec3 point = hash33(i + neighbor) * 0.5 + 0.5;
                vec3 diff = neighbor + point - f;
                minDistSq = min(minDistSq, dot(diff, diff));
            }
        }
    }

    return 1.0 - clamp(sqrt(minDistSq), 0.0, 1.0);
}

// F1 and F2 together — F2-F1 traces thin fibrous walls along cell
// boundaries, which plain F1 billows can't produce on their own.
vec2 worleyCellular3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 dir = sign(f - 0.5);

    float bestSq = 1.0;
    float secondSq = 1.0;

    for (int z = 0; z <= 1; z++) {
        for (int y = 0; y <= 1; y++) {
            for (int x = 0; x <= 1; x++) {
                vec3 neighbor = vec3(float(x), float(y), float(z)) * dir;
                vec3 point = hash33(i + neighbor) * 0.5 + 0.5;
                vec3 diff = neighbor + point - f;
                float distSq = dot(diff, diff);

                if (distSq < bestSq) {
                    secondSq = bestSq;
                    bestSq = distSq;
                } else if (distSq < secondSq) {
                    secondSq = distSq;
                }
            }
        }
    }

    return vec2(
        1.0 - clamp(sqrt(bestSq), 0.0, 1.0),
        1.0 - clamp(sqrt(secondSq), 0.0, 1.0));
}

float worleyFbm3D(vec3 p, int octaves, float lacunarity, float gain) {
    float sum = 0.0;
    float total = 0.0;
    float amp = 0.5;
    vec3 pos = p;

    for (int i = 0; i < octaves; i++) {
        sum += amp * worleyNoise3D(pos);
        total += amp;
        pos *= lacunarity;
        amp *= gain;
    }

    return sum / max(total, 0.0001);
}

float worleyFbm3D(vec3 p) {
    return worleyFbm3D(p, 3, 2.4, 0.55);
}

// ── Perlin-Worley hybrid ──────────────────────────────────────────────────
// The standard volumetric-cloud base shape: a low-frequency gradient fbm
// gives the billowy macro silhouette, and a Worley layer is inverted and
// used to erode only ITS OWN low end via a subtractive remap. That means
// erosion concentrates at Worley cell borders — carving cauliflower lobes
// into the silhouette — rather than multiplying noise straight through the
// whole volume, which is what punches random holes through a cloud's core.
float perlinWorley3D(vec3 p, float worleyFrequencyMul) {
    float perlin = fbmGradient3D(p, 5, 2.02, 0.5);
    float worley = worleyFbm3D(p * worleyFrequencyMul, 3, 2.2, 0.5);
    float worleyErosion = (1.0 - worley) * 0.5;
    return remapClamped(perlin, worleyErosion, 1.0, 0.0, 1.0);
}

// ── Curl noise (divergence-free warp) ─────────────────────────────────────
// Finite-difference curl of a 3-component gradient-noise potential field.
// Meant for one evaluation per macro-shape sample, never per raymarch
// light tap — nine gradientNoise3D calls is too costly for a hot loop.

vec3 curlNoise3D(vec3 p) {
    const float e = 0.1;
    const vec3 seedY = vec3(31.416, 47.853, 12.793);
    const vec3 seedZ = vec3(74.365, 9.481, 53.192);

    float fx  = gradientNoise3D(p);
    float fxY = gradientNoise3D(p + vec3(0.0, e, 0.0));
    float fxZ = gradientNoise3D(p + vec3(0.0, 0.0, e));

    float fy  = gradientNoise3D(p + seedY);
    float fyX = gradientNoise3D(p + seedY + vec3(e, 0.0, 0.0));
    float fyZ = gradientNoise3D(p + seedY + vec3(0.0, 0.0, e));

    float fz  = gradientNoise3D(p + seedZ);
    float fzX = gradientNoise3D(p + seedZ + vec3(e, 0.0, 0.0));
    float fzY = gradientNoise3D(p + seedZ + vec3(0.0, e, 0.0));

    float dFz_dy = (fzY - fz) / e;
    float dFy_dz = (fyZ - fy) / e;
    float dFx_dz = (fxZ - fx) / e;
    float dFz_dx = (fzX - fz) / e;
    float dFy_dx = (fyX - fy) / e;
    float dFx_dy = (fxY - fx) / e;

    return vec3(dFz_dy - dFy_dz, dFx_dz - dFz_dx, dFy_dx - dFx_dy);
}

#endif