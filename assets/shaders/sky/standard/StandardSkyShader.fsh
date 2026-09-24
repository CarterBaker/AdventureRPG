#version 330 core

in  vec3 v_dir;
out vec4 fragColor;

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SunLightData.glsl"
#include "sky/util/SkyColor.glsl"
#include "sky/util/SkyNoise.glsl"

/*
 * Fullscreen sky dome. The dome, twilight glow, anti-solar belt, and sun
 * halo all come from resolveSkyColor(), the same function the clouds use
 * for their haze, so clouds melt into exactly this sky. Slow rotating noise
 * nudges the gradient for a little life, and a sub-bit dither keeps the
 * long soft gradients free of banding.
 */

const float SKY_NOISE_GRADIENT_SHIFT = 0.18;
const float SKY_NOISE_TINT           = 0.015;
const float SKY_DITHER_STRENGTH      = 1.0 / 255.0;

void main() {
    vec3 dir    = normalize(v_dir);
    vec3 sunDir = normalize(u_sunDirection);

    float baseNoise      = calculateSkyNoiseBase(dir);
    float dailyVariation = calculateDailyVariation();
    float fullNoise      = calculateSkyNoiseWithDailyVariation(baseNoise, dailyVariation);
    float noiseOffset    = fullNoise - 0.5;

    vec3 sky = resolveSkyColor(dir, sunDir, noiseOffset * SKY_NOISE_GRADIENT_SHIFT);
    sky += noiseOffset * SKY_NOISE_TINT;
    sky += (hash31(vec3(gl_FragCoord.xy, 0.0)) - 0.5) * SKY_DITHER_STRENGTH;

    fragColor = vec4(max(sky, vec3(0.0)), 1.0);
}
