#ifndef SKY_COLOR_GLSL
#define SKY_COLOR_GLSL

#include "includes/TimeData.glsl"
#include "sky/DayNightCycle.glsl"
#include "sky/SeasonCycle.glsl"

// altitude: 0 = horizon, 1 = zenith
// noise: precomputed coherent FBM
// dailyVariation: 0->1 mask to blend daily random offset (avoids midnight pop)
// dayFactor: day/night/sunrise/sunset weights
// seasonFactor: winter/summer/spring/fall weights
vec3 getSkyColor(float altitude, vec3 worldDir, float fullNoise, float dailyVariation, CycleFactors dayFactor,
    SeasonFactors seasonFactor) {
    // --------------------------------------
    // NORMALIZE THE DAILY RANDOM INT
    // --------------------------------------
    const float INT_MAX = 2147483647.0;
    float dailyRandom = u_randomNoiseFromDay / INT_MAX;

    // --------------------------------------
    // BASE SKY PALETTE (day/night)
    // --------------------------------------
    vec3 nightTop = vec3(0.02, 0.02, 0.08);
    vec3 nightBottom = vec3(0.005, 0.005, 0.02);
    vec3 dayTop = vec3(0.60, 0.82, 1.00);
    vec3 dayBottom = vec3(0.32, 0.52, 0.80);

    vec3 topColor = nightTop * dayFactor.night + dayTop * dayFactor.day;
    vec3 bottomColor = nightBottom * dayFactor.night + dayBottom * dayFactor.day;

    // --------------------------------------
    // ALTITUDE GRADIENT WITH NOISE
    // --------------------------------------
    float altT = clamp(altitude, 0.0, 1.0);
    altT = smoothstep(0.0, 1.0, altT);
    altT = pow(altT, 1.2);

    float noisyT = altT + (fullNoise - 0.5) * 0.35;
    noisyT = clamp(noisyT, 0.0, 1.0);

    vec3 baseSky = mix(bottomColor, topColor, noisyT);
    baseSky += (fullNoise - 0.5) * 0.02;  // subtle texture

    // --------------------------------------
    // DAILY RANDOMNESS controls seasonal strength
    // Sometimes seasonal colors show, sometimes just basic sky
    // --------------------------------------
    float dailySeasonalStrength = dailyRandom * dailyVariation;  // mask midnight pop

    // --------------------------------------
    // SEASONAL COLOR TINTS (simplified)
    // Only applied when dailySeasonalStrength is high
    // --------------------------------------
    // Winter: cool blue tint
    vec3 winterTint = vec3(0.70, 0.75, 0.95);

    // Summer: warm golden tint
    vec3 summerTint = vec3(1.05, 0.95, 0.85);

    // Spring: fresh pastel tint
    vec3 springTint = vec3(1.00, 0.90, 0.80);

    // Fall: earthy amber tint
    vec3 fallTint = vec3(0.95, 0.80, 0.70);

    // Blend all seasonal tints
    vec3 seasonalTint = winterTint * seasonFactor.winter + summerTint * seasonFactor.summer +
    springTint * seasonFactor.spring + fallTint * seasonFactor.fall;

    // Apply seasonal tint with daily randomness controlling strength
    // Use additive offset instead of multiply to preserve gradient
    vec3 seasonalOffset = (seasonalTint - vec3(1.0)) * 0.15;  // tint becomes offset
    float seasonalStrength = dayFactor.day * dailySeasonalStrength * 0.5;
    baseSky += seasonalOffset * seasonalStrength;

    // --------------------------------------
    // DAILY COLOR VARIATION (subtle hue shift)
    // Generate 3 different randoms from the single int
    // --------------------------------------
    vec3 dailyColorOffset = vec3(fract(dailyRandom * 1.0) * 0.08 - 0.04, fract(dailyRandom * 7919.0) * 0.06 - 0.03,
        fract(dailyRandom * 5333.0) * 0.04 - 0.02);

    baseSky += dailyColorOffset * dailyVariation * dayFactor.day;

    // --------------------------------------
    // SUNRISE/SUNSET BLEND
    // --------------------------------------
    // Seasonal sunrise/sunset colors (one pair per season)
    vec3 winterSunriseSet = vec3(0.80, 0.50, 0.60);  // cool pink/purple
    vec3 summerSunriseSet = vec3(1.00, 0.50, 0.25);  // warm orange
    vec3 springSunriseSet = vec3(0.95, 0.65, 0.45);  // soft coral
    vec3 fallSunriseSet = vec3(0.85, 0.45, 0.25);    // burnt orange

    vec3 seasonalSunriseSet = winterSunriseSet * seasonFactor.winter + summerSunriseSet * seasonFactor.summer +
    springSunriseSet * seasonFactor.spring + fallSunriseSet * seasonFactor.fall;

    // Blend sunrise/sunset into sky
    float sunriseSetFactor = dayFactor.sunrise + dayFactor.sunset;
    baseSky = mix(baseSky, seasonalSunriseSet, sunriseSetFactor);

    // --------------------------------------
    // HORIZON DESATURATION
    // --------------------------------------
    float gray = dot(baseSky, vec3(0.333));
    baseSky = mix(baseSky, vec3(gray), 0.12 * (1.0 - altT));

    return baseSky;
}

#endif