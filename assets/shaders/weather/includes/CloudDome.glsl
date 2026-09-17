#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

#include "includes/WeatherMapData.glsl"

/*
* Bends an authored cloud altitude toward u_cloudDomeFadeAltitude as the
 * sampled point's horizontal distance from world center grows, so a pattern
 * sits at its real block elevation overhead and reaches exactly the fade
 * altitude at the edge of simulated weather range — a spherical dome rather
 * than a flat blanket. Distance is normalized against u_weatherRangeBlocks,
 * the same range the CPU streams patterns across: distanceT 0.0 is world
 * center (authored altitude) and distanceT 1.0 is range edge (fade altitude,
 * exactly, with no tolerance applied anywhere). Past the range edge the
 * surface is a true constant-altitude plane at the fade altitude, which the
 * clamp below produces for free so callers need no special case.
 * u_cloudDomeBendCurve reshapes the 0..1 falloff through a pow() exponent of
 * 1/curve — 1.0 is linear (a cone), 0.5 is a parabola, approaching 0.0 holds
 * true elevation almost the whole way out and drops to the fade altitude only
 * at the very edge. Both uniforms are pushed once at awake() by WeatherSystem
 * from EngineSetting.CLOUD_DOME_FADE_ALTITUDE_BLOCKS / CLOUD_DOME_BEND_CURVE.
 * The slope and normal below are the analytic derivative of that same curve,
 * used by the weather pass to know which way the bent layer is actually
 * facing at any point.
 */

uniform float u_cloudDomeFadeAltitude;
uniform float u_cloudDomeBendCurve;

const float CLOUD_DOME_BEND_CURVE_MIN = 0.02;
const float CLOUD_DOME_BEND_CURVE_MAX = 1.0;

float resolveCloudDomeBendExponent() {
    return 1.0 / clamp(u_cloudDomeBendCurve, CLOUD_DOME_BEND_CURVE_MIN, CLOUD_DOME_BEND_CURVE_MAX);
}

float resolveCloudDomeAltitude(float authoredAltitude, float distanceBlocks) {
    float distanceT = clamp(distanceBlocks / max(u_weatherRangeBlocks, 1.0), 0.0, 1.0);
    float bendT     = pow(distanceT, resolveCloudDomeBendExponent());
    return mix(authoredAltitude, u_cloudDomeFadeAltitude, bendT);
}

// Rate of altitude change per block of horizontal distance. Zero at world
// center and flat past the range edge, where the dome is a constant plane.
float resolveCloudDomeSlope(float authoredAltitude, float distanceBlocks) {
    float range     = max(u_weatherRangeBlocks, 1.0);
    float distanceT = distanceBlocks / range;

    if (distanceT <= 0.0 || distanceT >= 1.0)
    return 0.0;

    float exponent = resolveCloudDomeBendExponent();

    return (u_cloudDomeFadeAltitude - authoredAltitude)
    * exponent * pow(distanceT, exponent - 1.0) / range;
}

// Outward surface normal of the bent layer at a world position. This is the
// layer's local "up" — the weather pass shades and angle-tests against it
// rather than against world up, which is what lets a strongly bent, distant
// portion of the dome read as edge-on instead of as the same flat stamp
// every other angle produces.
vec3 resolveCloudDomeNormal(float authoredAltitude, vec3 worldPos) {
    vec2  radial         = worldPos.xz;
    float distanceBlocks = length(radial);

    if (distanceBlocks < 1.0)
    return vec3(0.0, 1.0, 0.0);

    float slope   = resolveCloudDomeSlope(authoredAltitude, distanceBlocks);
    vec2  radialN = radial / distanceBlocks;

    return normalize(vec3(-slope * radialN.x, 1.0, -slope * radialN.y));
}

#endif