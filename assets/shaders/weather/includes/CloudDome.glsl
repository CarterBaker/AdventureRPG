#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

/*
* Shared "clouds curve down to the horizon" dome-bend math. A weather
 * entry's authored altitude (WeatherMapData's cloudShape.y) is the real
 * altitude directly overhead the camera — resolveCloudDomeAltitude blends
 * toward the camera's own eye level (plus a small offset that sits it
 * just below the visible horizon) as the view ray tilts down toward the
 * horizon, so a cloud layer reads as rising up from beyond the horizon
 * and arcing overhead to its true elevation instead of sitting as a flat
 * blanket at a fixed height. The blend is driven purely by the ray's own
 * elevation angle, never by a horizontal world-space distance, so it is a
 * single continuous function of view direction with no plane-intersection
 * precondition and therefore no seam anywhere — including looking
 * straight up, where elevation is exactly 1 and the authored altitude
 * comes through completely unbent, exactly the cloud's real elevation.
 */

const float CLOUD_DOME_CURVE_POWER           = 2.2;
const float CLOUD_DOME_HORIZON_OFFSET_BLOCKS = -30.0;

float resolveCloudDomeAltitude(float authoredAltitude, float cameraY, float rayDirY) {
    float elevation01     = clamp(rayDirY, 0.0, 1.0);
    float bend            = pow(1.0 - elevation01, CLOUD_DOME_CURVE_POWER);
    float horizonAltitude = cameraY + CLOUD_DOME_HORIZON_OFFSET_BLOCKS;
    return mix(authoredAltitude, horizonAltitude, bend);
}

#endif