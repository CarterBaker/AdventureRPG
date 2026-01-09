#ifndef NEBULA_GLSL
#define NEBULA_GLSL

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "sky/DayNightCycle.glsl"

// dir: normalized world-space direction
// altitude: 0=horizon, 1=zenith
// factors: day/night cycle factors
vec3 calculateNebula(vec3 dir, float altitude, CycleFactors factors) {
    if (factors.night < 0.2) return vec3(0.0);  // fully transparent/black if day

    // --- remove east/west banding by de-aligning world axes ---
    mat3 rot = mat3(0.36, 0.48, -0.80, -0.80, 0.60, 0.10, 0.48, 0.64, 0.60);
    vec3 p = rot * dir * 3.0;

    float n1 = fbmNoise3D(p * 0.8);
    float n2 = fbmNoise3D(p * 1.5 + vec3(2.3, 1.1, 0.5));
    float density = n1 * 0.6 + n2 * 0.4;
    density = smoothstep(0.4, 0.7, density);

    float detail = fbmNoise3D(p * 3.0 + vec3(0.5, 1.2, 0.8));
    density *= 0.7 + detail * 0.3;

    float colorMix = fbmNoise3D(p * 0.5 + vec3(1.0, 2.0, 3.0));
    vec3 color1 = vec3(1.0, 0.3, 0.5);
    vec3 color2 = vec3(0.3, 0.4, 1.0);
    vec3 color3 = vec3(0.7, 0.2, 0.8);

    vec3 nebulaColor = mix(color1, color2, colorMix);
    nebulaColor = mix(nebulaColor, color3, smoothstep(0.5, 0.8, colorMix));

    float nightFade = smoothstep(0.2, 0.5, factors.night);
    float altFade = smoothstep(0.4, 0.6, altitude);

    float alpha = density * nightFade * altFade * 0.6;

    // output nebula color only, no base sky blending
    return nebulaColor * alpha;
}

#endif
