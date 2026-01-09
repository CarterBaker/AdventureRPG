#ifndef NOISE_UTILITY_GLSL
#define NOISE_UTILITY_GLSL

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

#endif
