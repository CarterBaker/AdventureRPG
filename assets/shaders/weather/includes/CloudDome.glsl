#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

#include "includes/WeatherMapData.glsl"

/*
* Bends an authored cloud altitude down toward u_cloudDomeFadeAltitude as a
 * weather entry's true horizontal distance from world center grows, so a
 * pattern sits at its real elevation near the camera and sinks to the fade
 * altitude at the horizon — giving the sky a spherical "dome" silhouette
 * instead of a flat blanket. Distance is normalized against
 * u_weatherRangeBlocks, the same terrain-independent range the CPU streams
 * patterns across, so the dome always spans exactly as far as weather is
 * simulated: distanceT = 0.0 is world center (authored altitude) and
 * distanceT = 1.0 is the edge of simulated range (fade altitude).
 * u_cloudDomeBendCurve reshapes that 0..1 falloff through a pow() exponent
 * of 1/curve — curve = 1.0 is a linear bend (a literal cone from apex to
 * base), curve = 0.5 (default) is a parabola, and curve approaching 0.0
 * holds the true elevation almost the entire way out and only snaps down
 * to the fade altitude right at the horizon edge. Both uniforms are pushed
 * once at awake() by WeatherSystem from EngineSetting.CLOUD_DOME_FADE_
 * ALTITUDE_BLOCKS / CLOUD_DOME_BEND_CURVE.
 */

uniform float u_cloudDomeFadeAltitude;
uniform float u_cloudDomeBendCurve;

const float CLOUD_DOME_BEND_CURVE_MIN = 0.02;
const float CLOUD_DOME_BEND_CURVE_MAX = 1.0;

float resolveCloudDomeAltitude(float authoredAltitude, float distanceBlocks) {
    float distanceT  = clamp(distanceBlocks / max(u_weatherRangeBlocks, 1.0), 0.0, 1.0);
    float curve      = clamp(u_cloudDomeBendCurve, CLOUD_DOME_BEND_CURVE_MIN, CLOUD_DOME_BEND_CURVE_MAX);
    float curvePower = 1.0 / curve;
    float bendT      = pow(distanceT, curvePower);
    return mix(authoredAltitude, u_cloudDomeFadeAltitude, bendT);
}

#endif