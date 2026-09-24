#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"

/*
 * The sky's dome is the planet itself. Every cloud layer is the shell between
 * two spheres around the planet's centre — one planet radius below sea level,
 * straight beneath the camera — at the layer's own altitude above sea level,
 * so a cloud belongs to the world rather than to the viewer: it stays where
 * it is as the camera climbs, can be flown into, and can be looked down on
 * from above. Overhead the shell is nearly flat, so a cloud there is seen
 * from beneath; toward the horizon the planet's curvature carries the same
 * shell down to eye level, so the view runs through the clouds' sides
 * instead. The weather itself stays flat: a sample's place on the weather map
 * is its horizontal offset, so the map slides across the dome with the flow.
 */

const float CLOUD_DOME_EPSILON = 0.0001;
const float CLOUD_DOME_NO_HIT   = 1.0e30;

float resolveCloudDomeCameraAltitude() {
    return u_cameraPosition.y - u_weatherPlanet.y;
}

float resolveCloudDomeCameraRadius() {
    return u_weatherPlanet.x + resolveCloudDomeCameraAltitude();
}

// Both crossings of a unit ray from the camera with the sphere at the given
// altitude above sea level: x = near, y = far. Solved around the camera with
// the constant term factored, so the planet's large radius never cancels the
// small height differences away.
bool intersectCloudDomeSphere(float altitude, vec3 rayDir, out vec2 crossings) {
    float cameraRadius = resolveCloudDomeCameraRadius();
    float b            = rayDir.y * cameraRadius;
    float c            = (resolveCloudDomeCameraAltitude() - altitude)
    * (cameraRadius + u_weatherPlanet.x + altitude);
    float discriminant = b * b - c;

    crossings = vec2(CLOUD_DOME_NO_HIT);

    if (discriminant < 0.0)
    return false;

    float q = b >= 0.0 ? -(b + sqrt(discriminant)) : sqrt(discriminant) - b;

    if (abs(q) < CLOUD_DOME_EPSILON) {
        crossings = vec2(-b);
        return true;
    }

    float other = c / q;
    crossings = vec2(min(q, other), max(q, other));

    return true;
}

// Distance to the ground the planet presents: sea level, or the camera's own
// height when it stands below sea level, so no cloud is ever drawn through
// the planet beneath the horizon.
float resolveCloudDomeGroundDistance(vec3 rayDir) {
    vec2 crossings;

    if (!intersectCloudDomeSphere(min(resolveCloudDomeCameraAltitude(), 0.0), rayDir, crossings))
    return CLOUD_DOME_NO_HIT;

    return crossings.y > 0.0 ? max(crossings.x, 0.0) : CLOUD_DOME_NO_HIT;
}

// The first stretch of the ray, within maxDistance, that lies inside a layer
// between baseAltitude and topAltitude — from beneath, from inside, or from
// above.
bool resolveCloudDomeInterval(
    float baseAltitude, float topAltitude, vec3 rayDir, float maxDistance,
    out float tEnter, out float tExit) {
    vec2 outer;
    vec2 inner;

    tEnter = 0.0;
    tExit  = 0.0;

    if (!intersectCloudDomeSphere(topAltitude, rayDir, outer) || outer.y <= 0.0)
    return false;

    tEnter = max(outer.x, 0.0);
    tExit  = min(outer.y, maxDistance);

    if (intersectCloudDomeSphere(baseAltitude, rayDir, inner)) {
        if (tEnter >= inner.x && tEnter < inner.y) {
            tEnter = inner.y;
        } else if (inner.x > tEnter) {
            tExit = min(tExit, inner.x);
        }
    }

    return tExit > tEnter;
}

// Altitude above sea level of a point relativePosition away from the camera.
float resolveCloudDomeAltitude(vec3 relativePosition) {
    float radial       = resolveCloudDomeCameraRadius() + relativePosition.y;
    float horizontalSq = dot(relativePosition.xz, relativePosition.xz);

    return resolveCloudDomeCameraAltitude() + relativePosition.y
    + horizontalSq / (sqrt(horizontalSq + radial * radial) + radial);
}

// The planet's local up at a point, which the shading lights against so a
// distant stretch of the curved layer reads as tilted toward the horizon.
vec3 resolveCloudDomeNormal(vec3 relativePosition) {
    return normalize(vec3(
        relativePosition.x,
        resolveCloudDomeCameraRadius() + relativePosition.y,
        relativePosition.z));
}

#endif
