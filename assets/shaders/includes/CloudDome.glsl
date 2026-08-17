#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"

/*
* Shared "clouds curve down toward the horizon" dome-bend math. A weather
 * entry's authored altitude (WeatherMapData's cloudShape.y) is the real
 * altitude everywhere directly overhead — resolveCloudDomeAltitude only
 * ever subtracts a sag from it, it never blends toward a shared floor.
 * That distinction matters: an earlier revision mixed every entry toward
 * `cameraPosition.y - constant`, which collapsed every cloud type onto
 * the same visible band at range and reshuffled the whole sky any time
 * the camera's own altitude changed — most obvious standing on a
 * mountain. Sagging by horizontal distance alone keeps each cloud type
 * at its own authored altitude directly overhead, keeps their relative
 * separation intact at every range, and is completely unaffected by
 * camera height, mirroring how WorldCurvature.glsl sags terrain by
 * horizontal distance only. domeRadiusBlocks is derived from
 * u_weatherRangeBlocks — the CPU-side weather sampling range — so the
 * bend always spans the same distance the weather simulation itself
 * spans.
 */

const float CLOUD_DOME_RADIUS_SCALE   = 0.85;
const float CLOUD_DOME_CURVE_POWER    = 1.6;
const float CLOUD_DOME_MAX_DIP_BLOCKS = 450.0;

float resolveCloudDomeRadius() {
    return max(u_weatherRangeBlocks * CLOUD_DOME_RADIUS_SCALE, 1.0);
}

// Pure function of horizontal distance from the camera — never camera
// height — so the sag never reshapes itself as the player climbs or
// descends; only the camera's own position relative to the (fixed)
// result changes.
float resolveCloudDomeAltitude(vec2 worldXZ, float authoredAltitude, float domeRadiusBlocks) {
    float distFromCamera = distance(worldXZ, u_cameraPosition.xz);
    float t   = clamp(distFromCamera / domeRadiusBlocks, 0.0, 1.0);
    float dip = pow(t, CLOUD_DOME_CURVE_POWER) * CLOUD_DOME_MAX_DIP_BLOCKS;
    return authoredAltitude - dip;
}

bool intersectCloudDomePlane(vec3 origin, vec3 dir, float planeY, out float t) {
    if (abs(dir.y) < 0.0001)
    return false;
    t = (planeY - origin.y) / dir.y;
    return t > 0.0;
}

#endif