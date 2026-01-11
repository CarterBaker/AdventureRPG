#ifndef SEASON_CYCLE_GLSL
#define SEASON_CYCLE_GLSL

#include "includes/TimeData.glsl"

// Returns factors for winter/summer/spring/fall
struct SeasonFactors {
    float winter;  // 1 at 0.0/1.0, 0 at 0.5
    float summer;  // 1 at 0.5, 0 at 0.0/1.0
    float spring;  // peaks around 0.25
    float fall;    // peaks around 0.75
};

SeasonFactors getSeasonFactors() {
    SeasonFactors factors;

    // Winter/summer exponential curve
    // winter peaks at 0.0 and 1.0, summer peaks at 0.5
    factors.winter = pow(abs(1.0 - 2.0 * u_timeOfYear), 2.0);
    factors.summer = 1.0 - factors.winter;

    // Spring exponential peak (around 0.25)
    float springPeak = 0.25;
    factors.spring = exp(-pow((u_timeOfYear - springPeak) / 0.05, 2.0));

    // Fall exponential peak (around 0.75)
    float fallPeak = 0.75;
    factors.fall = exp(-pow((u_timeOfYear - fallPeak) / 0.05, 2.0));

    return factors;
}

#endif