#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

#include "includes/WeatherMapData.glsl"

/*
* Bends an authored cloud altitude down toward a tunable fade altitude as
 * the fragment's own true horizontal distance grows, so a pattern's clouds
 * sit at their real elevation near the camera and sink toward the fade
 * altitude at the horizon continuously across their own footprint instead
 * of as one flat bent slab. Distance is normalized against
 * u_weatherRangeBlocks — the same terrain-independent range the CPU streams
 * patterns across — so the dome always spans exactly as far as weather is
 * simulated. u_cloudDomeFadeAltitude and u_cloudDomeBendCurve are pushed
 * once at awake() by WeatherSystem (see EngineSetting.CLOUD_DOME_FADE_
 * ALTITUDE_BLOCKS / CLOUD_DOME_BEND_CURVE): the fade altitude defaults to
 * sea level, and the bend curve is a 0..1 knob mapped to a pow() exponent
 * via 1/knob — 1.0 is a linear bend, 0.5 (default) is a parabola, and
 * values near 0.0 hold the true elevation almost the whole way out and only
 * bend down right at the horizon edge.
 */

uniform float u_cloudDomeFadeAltitude;
uniform float u_cloudDomeBendCurve;

const float CLOUD_DOME_BEND_CURVE_MIN = 0.02;

float resolveCloudDomeAltitude(float authoredAltitude, float distanceBlocks) {
    float distanceT  = clamp(distanceBlocks / max(u_weatherRangeBlocks, 1.0), 0.0, 1.0);
    float curvePower = 1.0 / max(u_cloudDomeBendCurve, CLOUD_DOME_BEND_CURVE_MIN);
    float bend        = pow(distanceT, curvePower);
    return mix(authoredAltitude, u_cloudDomeFadeAltitude, bend);
}

#endif