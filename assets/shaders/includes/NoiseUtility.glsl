#ifndef NOISE_UTILITY_GLSL
#define NOISE_UTILITY_GLSL

/*
* Shared noise primitives for every sky, weather, and cloud shader. A
 * value-noise set (hash31/smoothNoise3D/fbmNoise3D/fbmNoise2D/cellNoise)
 * for SkyNoise.glsl's daily dithering, and a gradient (Perlin-style) +
 * Worley (cellular) set for volumetric clouds — soft billowing shape from
 * gradient noise, bumpy cauliflower edges from Worley.
 */

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

mat3 noiseBasis = mat3(0.00, 0.80, 0.60, -0.80, 0.36, -0.48, -0.60, -0.48, 0.64);

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

// Simple FBM - back to basics
float fbmNoise3D(vec3 p) {
    float n = 0.0;
    n += 0.5 * smoothNoise3D(p);
    n += 0.25 * smoothNoise3D(p * 2.0);
    n += 0.125 * smoothNoise3D(p * 4.0);
    return n;
}

// --- 2D FBM using existing 3D implementation ---
float fbmNoise2D(vec2 p) {
    return fbmNoise3D(vec3(p, 0.0));
}

// --- simple 2D cell (Worley) noise ---
float cellNoise(vec3 p) {
    vec2 pv = p.xy;
    vec2 i = floor(pv);
    vec2 f = fract(pv);

    float res = 1.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec3 cell = vec3(i + neighbor, p.z);

            // use your existing hash
            float h = hash31(cell);

            vec2 randOffset = vec2(fract(h * 113.1), fract(h * 17.3));

            vec2 diff = neighbor + randOffset - f;

            float d = dot(diff, diff);
            res = min(res, d);
        }
    }

    return 1.0 - sqrt(res);
}

// ── Gradient (Perlin-style) noise ───────────────────────────────────────
vec3 hash33(vec3 p) {
    p = vec3(
        dot(p, vec3(127.1, 311.7, 74.7)),
        dot(p, vec3(269.5, 183.3, 246.1)),
        dot(p, vec3(113.5, 271.9, 124.6)));
    return fract(sin(p) * 43758.5453123) * 2.0 - 1.0;
}

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

// 4-octave FBM over gradientNoise3D — remapped to roughly [0, 1]. This is
// the cloud macro-shape function: soft, continuous billows rather than
// the grainier look a value-noise fbm produces.
float fbmGradient3D(vec3 p) {
    float sum = 0.0;
    float amp = 0.5;
    vec3 pos = p;

    for (int i = 0; i < 4; i++) {
        sum += amp * gradientNoise3D(pos);
        pos *= 2.03; // non-power-of-2 lacunarity avoids repeating lattices
        amp *= 0.5;
    }

    return clamp(sum * 0.5 + 0.5, 0.0, 1.0);
}

// ── Worley (cellular) noise ─────────────────────────────────────────────
// Only the current cell plus whichever neighbor each axis leans toward
// (2x2x2 = 8 taps) rather than the full 3x3x3 = 27 neighborhood. The true
// nearest feature point can in principle sit in a cell this skips, but the
// jitter is bounded within each cell so any error stays sub-cell-sized —
// invisible once blended into the fbm layer above it, and ~3.4x cheaper
// per call in what is by far the hottest function in the cloud raymarch.
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

// Two-octave Worley — a second, higher-frequency layer blended under the
// first so cell walls read as thin wisps rather than solid cell blobs.
float worleyFbm3D(vec3 p) {
    float w1 = worleyNoise3D(p);
    float w2 = worleyNoise3D(p * 2.4);
    return clamp(w1 * 0.65 + w2 * 0.35, 0.0, 1.0);
}

#endif