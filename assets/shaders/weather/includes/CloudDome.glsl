#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"

/*
 * The sky's dome. Every cloud layer arcs over the camera: straight overhead it
 * stands at its own altitude above sea level, and it bends down with the
 * square of the horizontal distance, just steeply enough that its base meets
 * sea level at the dome's horizon distance, the outer ring of the world's
 * macro terrain. A layer keeps its thickness all the way down, so distant
 * clouds sit on the horizon with their crowns standing over it, the view
 * toward the horizon runs through the clouds' sides, and the clouds sink
 * behind the world's flat horizon exactly where the world itself ends. A
 * cloud still belongs to the world rather than to the viewer near the camera,
 * where the arc is slight: it can be flown into and looked down on from
 * above. The weather itself stays flat: a sample's place on the weather map
 * is its horizontal offset, so the map slides across the dome with the flow.
 */

const float CLOUD_DOME_EPSILON = 0.0001;
const float CLOUD_DOME_NO_HIT  = 1.0e30;

float resolveCloudDomeCameraAltitude() {
    return u_cameraPosition.y - u_weatherDome.y;
}

// How far a layer whose base stands baseAltitude above sea level bends down
// per square block of horizontal distance.
float resolveCloudDomeCurvature(float baseAltitude) {
    return max(baseAltitude, 0.0) / (u_weatherDome.x * u_weatherDome.x);
}

// Where a unit ray from the camera lies below the dome surface at the given
// altitude: from x to y along the ray. The surface bends down by curvature
// times the square horizontal distance, so the ray meets it where a quadratic
// in the distance along the ray crosses zero; a straight up or down ray, or a
// flat surface, leaves a single crossing with the stretch below it running
// out to one side.
bool intersectCloudDome(float altitude, float curvature, vec3 rayDir, out vec2 crossings) {
    float a = curvature * dot(rayDir.xz, rayDir.xz);
    float b = rayDir.y;
    float c = resolveCloudDomeCameraAltitude() - altitude;

    crossings = vec2(CLOUD_DOME_NO_HIT);

    if (a < CLOUD_DOME_EPSILON * CLOUD_DOME_EPSILON) {
        if (abs(b) < CLOUD_DOME_EPSILON) {
            if (c > 0.0)
            return false;

            crossings = vec2(-CLOUD_DOME_NO_HIT, CLOUD_DOME_NO_HIT);
            return true;
        }

        float t = -c / b;
        crossings = b > 0.0 ? vec2(-CLOUD_DOME_NO_HIT, t) : vec2(t, CLOUD_DOME_NO_HIT);

        return true;
    }

    float discriminant = b * b - 4.0 * a * c;

    if (discriminant < 0.0)
    return false;

    float q     = -0.5 * (b + (b >= 0.0 ? sqrt(discriminant) : -sqrt(discriminant)));
    float rootA = q / a;
    float rootB = abs(q) > CLOUD_DOME_EPSILON ? c / q : rootA;

    crossings = vec2(min(rootA, rootB), max(rootA, rootB));

    return true;
}

// Distance to the ground the world presents. The world itself is flat, so its
// ground is the sea level plane, or the camera's own height when it stands
// below sea level: cloud the dome carries down past the world's flat horizon
// sinks behind it, exactly as the world's own terrain would hide it, and no
// cloud is ever drawn under the sea the world draws.
float resolveCloudDomeGroundDistance(vec3 rayDir) {
    if (rayDir.y >= -CLOUD_DOME_EPSILON)
    return CLOUD_DOME_NO_HIT;

    return max(resolveCloudDomeCameraAltitude(), 0.0) / -rayDir.y;
}

// The first stretch of the ray, within maxDistance, that lies inside a layer
// between baseAltitude and topAltitude — from beneath, from inside, or from
// above.
bool resolveCloudDomeInterval(
    float baseAltitude, float topAltitude, float curvature, vec3 rayDir, float maxDistance,
    out float tEnter, out float tExit) {
    vec2 outer;
    vec2 inner;

    tEnter = 0.0;
    tExit  = 0.0;

    if (!intersectCloudDome(topAltitude, curvature, rayDir, outer) || outer.y <= 0.0)
    return false;

    tEnter = max(outer.x, 0.0);
    tExit  = min(outer.y, maxDistance);

    if (intersectCloudDome(baseAltitude, curvature, rayDir, inner)) {
        if (tEnter >= inner.x && tEnter < inner.y) {
            tEnter = inner.y;
        } else if (inner.x > tEnter) {
            tExit = min(tExit, inner.x);
        }
    }

    return tExit > tEnter;
}

// Altitude above sea level, as the dome bends it, of a point relativePosition
// away from the camera: the true altitude lifted by the drop of the arc there.
float resolveCloudDomeAltitude(vec3 relativePosition, float curvature) {
    return resolveCloudDomeCameraAltitude() + relativePosition.y
    + curvature * dot(relativePosition.xz, relativePosition.xz);
}

// The dome's local up at a point, which the shading lights against so a
// distant stretch of the arc reads as tilted toward the horizon.
vec3 resolveCloudDomeNormal(vec3 relativePosition, float curvature) {
    return normalize(vec3(
        2.0 * curvature * relativePosition.x,
        1.0,
        2.0 * curvature * relativePosition.z));
}

#endif
