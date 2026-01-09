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
    vec3 p = rot * dir * 2.0;  // smaller multiplier -> larger cloud shapes

    // Base density
    float n1 = fbmNoise3D(p * 0.6);  // larger scale = bigger clouds
    float n2 = fbmNoise3D(p * 1.2 + vec3(2.3, 1.1, 0.5));
    float density = n1 * 0.5 + n2 * 0.5;
    density = smoothstep(0.3, 0.7, density);  // slightly softer threshold

    // Small detail modulation (keep subtle, no distortion)
    float detail = fbmNoise3D(p * 2.5 + vec3(0.5, 1.2, 0.8));
    density *= 0.8 + detail * 0.2;

    // Color variation
    float colorMix = fbmNoise3D(p * 0.4 + vec3(1.0, 2.0, 3.0));  // larger scale
    vec3 color1 = vec3(1.0, 0.2, 0.3);                           // brighter red
    vec3 color2 = vec3(0.2, 0.3, 1.0);                           // brighter blue
    vec3 color3 = vec3(0.8, 0.2, 0.9);                           // bright purple accent

    vec3 nebulaColor = mix(color1, color2, colorMix);
    nebulaColor = mix(nebulaColor, color3, smoothstep(0.4, 0.7, colorMix));

    // Fade factors
    float nightFade = smoothstep(0.2, 0.5, factors.night);
    float altFade = smoothstep(0.3, 0.7, altitude);  // slightly wider range

    float alpha = density * nightFade * altFade * 0.8;  // brighter overall

    return nebulaColor * alpha;
}

#endif
