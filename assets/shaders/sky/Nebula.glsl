#ifndef NEBULA_GLSL
#define NEBULA_GLSL

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "sky/DayNightCycle.glsl"
#include "sky/SeasonCycle.glsl"

vec4 calculateNebula(vec3 dir, float dailyVariation, CycleFactors dayFactor,
    SeasonFactors seasonFactor) {

    float nightFade = smoothstep(0.3, 0.6, dayFactor.night);
    if (nightFade <= 0.0) return vec4(0.0);

    float seasonalBrightness = mix(0.08, 1.0, seasonFactor.winter);

    mat3 rot = mat3(0.36, 0.48, -0.80,
        -0.80, 0.60, 0.10,
        0.48, 0.64, 0.60);

    vec3 p = rot * dir * 2.0;

    // ------------------------------------------------------------------
    // FIRST: evaluate density proxy BEFORE motion to derive "opacity"
    // ------------------------------------------------------------------
    float n1_pre = fbmNoise3D(p * 0.6);
    float n2_pre = fbmNoise3D(p * 1.2 + vec3(2.3, 1.1, 0.5));
    float density_pre = smoothstep(0.5, 0.8, 0.5 * n1_pre + 0.5 * n2_pre);

    // core opacity proxy (before alpha shaping)
    float core_pre = smoothstep(0.05, 0.55, density_pre);
    core_pre *= core_pre;

    // ------------------------------------------------------------------
    // WAVE MOTION with inverse opacity coupling
    // ------------------------------------------------------------------

    // more transparent → more motion
    float motionFactor = 1.0 - clamp(core_pre, 0.0, 1.0);

    // slightly stronger base motion
    float waveAmplitude = 0.10;     // was 0.05
    float waveSpeed = 0.2;

    vec2 wave =
        waveAmplitude * motionFactor * vec2(
            sin(u_time * waveSpeed + p.x * 2.0),
            cos(u_time * waveSpeed + p.y * 2.5));

    // apply motion
    p.xy += wave;

    // ------------------------------------------------------------------
    // continue your original pipeline with displaced p
    // ------------------------------------------------------------------

    float n1 = fbmNoise3D(p * 0.6);
    float n2 = fbmNoise3D(p * 1.2 + vec3(2.3, 1.1, 0.5));
    float density = smoothstep(0.5, 0.8, 0.5 * n1 + 0.5 * n2);

    float detail = fbmNoise3D(p * 2.5 + vec3(0.5, 1.2, 0.8));
    density *= 0.8 + 0.2 * detail;

    float colorMix = fbmNoise3D(p * 0.4 + vec3(1.0,2.0,3.0));
    vec3 baseColor = mix(vec3(1.0,0.2,0.3), vec3(0.2,0.3,1.0), colorMix);
    baseColor = mix(baseColor, vec3(0.8,0.2,0.9), smoothstep(0.4,0.7,colorMix));

    float core = smoothstep(0.05, 0.55, density);
    core *= core;

    float colorStrength = abs(colorMix - 0.5) * 2.0;
    colorStrength = clamp(colorStrength, 0.0, 1.0);

    float structureBlend = mix(core, core * colorStrength, 0.6);

    float edgeFade = smoothstep(0.0, 1.0, density);

    float alpha = structureBlend * edgeFade * nightFade * seasonalBrightness;

    alpha = sqrt(alpha);

    return vec4(baseColor, alpha);
}

#endif
