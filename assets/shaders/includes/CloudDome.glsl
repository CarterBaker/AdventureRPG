#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"

/*
* Shared "clouds curve down toward the horizon" dome-bend math. A weather
 * entry's authored altitude (WeatherMapData's cloudShape.y) is only ever
 * the TRUE altitude directly overhead the camera — anywhere else it bends
 * toward camera-eye height the farther out it sits, so the sky reads as a
 * dome arcing up to the player rather than a flat blanket. Every consumer
 * that needs to know where a cloud entry ACTUALLY sits above some point —
 * WeatherShader.fsh's visible puff raymarch and CloudShadow.glsl's terrain
 * shadow ray alike — resolves that altitude through here, so a shadow
 * always falls under the exact puff a viewer sees, never under where that
 * puff would have sat before bending. domeRadiusBlocks is derived from
 * u_weatherRangeBlocks — the CPU-side weather sampling range — so the bend
 * always spans the same distance the weather simulation itself spans.
 */

const float CLOUD_DOME_RADIUS_SCALE       = 0.85;
const float CLOUD_DOME_CURVE_POWER        = 0.65;
const float CLOUD_DOME_HORIZON_DIP_BLOCKS = 60.0;

float resolveCloudDomeRadius() {
    return max(u_weatherRangeBlocks * CLOUD_DOME_RADIUS_SCALE, 1.0);
}

float computeCloudDomeT(vec2 worldXZ, float domeRadiusBlocks) {
    vec2  fromCameraXZ = worldXZ - u_cameraPosition.xz;
    float domeRadiusSq = domeRadiusBlocks * domeRadiusBlocks;
    float domeBiasedT  = pow(clamp(dot(fromCameraXZ, fromCameraXZ) / domeRadiusSq, 0.0, 1.0), CLOUD_DOME_CURVE_POWER);
    return smoothstep(0.0, 1.0, domeBiasedT);
}

float resolveCloudDomeAltitude(vec2 worldXZ, float authoredAltitude, float domeRadiusBlocks) {
    float domeT             = computeCloudDomeT(worldXZ, domeRadiusBlocks);
    float domeFloorAltitude = u_cameraPosition.y - CLOUD_DOME_HORIZON_DIP_BLOCKS;
    return mix(authoredAltitude, domeFloorAltitude, domeT);
}

bool intersectCloudDomePlane(vec3 origin, vec3 dir, float planeY, out float t) {
    if (abs(dir.y) < 0.0001)
    return false;
    t = (planeY - origin.y) / dir.y;
    return t > 0.0;
}

#endif