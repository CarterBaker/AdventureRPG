#version 150
in vec3 v_dir;
out vec4 fragColor;
#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"

const float PI = 3.14159265359;

/* ---- SKY COLORS ---- */
const vec3 c_daySkyTop = vec3(0.3, 0.5, 0.9);
const vec3 c_daySkyHorizon = vec3(0.7, 0.85, 1.0);
const vec3 c_nightSkyTop = vec3(0.005, 0.01, 0.03);
const vec3 c_nightSkyHorizon = vec3(0.02, 0.03, 0.08);

void main() {
    vec3 dir = normalize(v_dir);

    /* ---- TIME FACTORS ---- */
    float nightFactor = abs(1.0 - 2.0 * u_timeOfDay);
    float dayFactor = 1.0 - nightFactor;

    /* ---- ALTITUDE ---- */
    float altitude = clamp(dir.y, 0.0, 1.0);
    altitude = smoothstep(0.0, 1.0, altitude);

    /* ---- BASE SKY GRADIENT ---- */
    vec3 daySky = mix(c_daySkyHorizon, c_daySkyTop, altitude);
    vec3 nightSky = mix(c_nightSkyHorizon, c_nightSkyTop, altitude);
    vec3 sky = mix(nightSky, daySky, dayFactor);

    /* ---- DAILY VARIATION ---- */
    float daySeed = fract(u_randomNoiseFromDay);
    float skyHueShift = (daySeed - 0.5) * 0.15;
    sky = mix(sky, sky * vec3(1.0 + skyHueShift, 1.0, 1.0 - skyHueShift * 0.5), dayFactor * 0.3);

    /* ---- TWILIGHT (SUNRISE/SUNSET) ---- */
    float twilightPeak = 0.15;  // Peak intensity at dawn/dusk
    float twilightFactor = smoothstep(0.0, twilightPeak, dayFactor) * smoothstep(1.0, 1.0 - twilightPeak, dayFactor);

    // Daily varying sunset colors
    vec3 sunsetA = vec3(1.0, 0.4, 0.2);
    vec3 sunsetB = vec3(1.0, 0.6, 0.3);
    vec3 sunsetC = vec3(0.95, 0.5, 0.4);
    vec3 twilightColor = mix(mix(sunsetA, sunsetB, daySeed), sunsetC, fract(daySeed * 7.13));

    // Apply sunset/sunrise glow near horizon
    float horizonGlow = smoothstep(0.6, 0.0, altitude) * smoothstep(-0.15, 0.0, dir.y);
    sky = mix(sky, twilightColor, horizonGlow * twilightFactor * 0.8);

    /* ---- CLOUDS (NO SEAMS) ---- */
    // Use 3D noise sampling to avoid seams
    vec3 cloudDir = normalize(dir);
    vec3 cloudPos = cloudDir * 100.0;                         // Large sphere
    cloudPos.xz += u_time * vec2(0.3, 0.2);                   // Slow drift
    cloudPos += vec3(0.0, 0.0, u_randomNoiseFromDay * 50.0);  // Daily variation

    float cloudNoise = fbmNoise3D(cloudPos * 0.05);
    cloudNoise = smoothstep(0.4, 0.7, cloudNoise);

    // Clouds only near horizon and below
    float cloudMask = smoothstep(0.4, -0.1, dir.y) * smoothstep(-0.2, -0.05, dir.y);
    cloudNoise *= cloudMask;

    // Cloud color
    vec3 cloudColor = mix(vec3(0.3, 0.3, 0.4), vec3(1.0, 1.0, 1.0), dayFactor);
    cloudColor = mix(cloudColor, twilightColor * 1.2, twilightFactor * horizonGlow * 0.6);

    sky = mix(sky, cloudColor, cloudNoise * 0.7 * mix(0.4, 1.0, dayFactor));

    /* ---- STARS (CONSISTENT, NON-ROTATING) ---- */
    // Use world-space direction directly (no rotation)
    vec3 starDir = normalize(dir);
    vec3 starCell = floor(starDir * 200.0);  // Higher density for smaller stars
    float starSeed = hash31(starCell);

    float starThreshold = 0.992;  // Fewer, smaller stars
    float starOn = step(starThreshold, starSeed);

    // Smaller, subtler stars
    float starBright = pow(fract(starSeed * 931.7), 25.0) * 0.8;

    // Gentle twinkle
    float twinkle = 0.7 + 0.3 * sin(u_time * 1.2 + starSeed * 6.28);

    // Only show stars at night, fade based on altitude
    float starFade = smoothstep(0.0, 0.3, altitude) * nightFactor;

    sky += vec3(starOn * starBright * twinkle * starFade);

    fragColor = vec4(sky, 1.0);
}