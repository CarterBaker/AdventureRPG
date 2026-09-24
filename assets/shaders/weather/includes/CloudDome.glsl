#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"

/*
 * Bends every cloud layer into a dome over the camera. A layer is the shell
 * between two concentric spheres centred straight below the camera, sized so
 * the layer's base sits at its true altitude overhead and comes down to eye
 * level exactly at the dome range, where the weather window ends. Overhead the
 * shell is nearly flat, so a cloud there is seen from beneath; toward the
 * horizon the same shell is met at a grazing angle, so the view runs through
 * the cloud's side and crown instead. The weather itself stays flat: a
 * sample's place on the weather map is simply its horizontal offset, so the
 * map slides across the dome as the flow carries it.
 */

const float CLOUD_DOME_MIN_HEIGHT_BLOCKS    = 32.0;
const float CLOUD_DOME_MIN_THICKNESS_BLOCKS = 4.0;
const float CLOUD_DOME_EPSILON              = 0.0001;

// x = dome radius (depth of the shared centre below the camera),
// y = layer base height above the camera, z = layer top height above it.
vec3 resolveCloudDome(int layer) {
    vec4  shape      = u_weatherLayerShape[layer];
    float range      = u_weatherMapOrigin.w;
    float baseHeight = max(shape.x - u_cameraPosition.y, CLOUD_DOME_MIN_HEIGHT_BLOCKS);
    float topHeight  = baseHeight + max(shape.y, CLOUD_DOME_MIN_THICKNESS_BLOCKS);
    float radius     = (range * range - baseHeight * baseHeight) / (2.0 * baseHeight);

    return vec3(max(radius, baseHeight), baseHeight, topHeight);
}

// Distance along a unit ray from the camera to a shell sphere shellHeight
// above the camera. The camera is always inside the sphere, so there is
// exactly one forward crossing; the rationalised form keeps upward rays
// precise despite the dome's large radius.
float intersectCloudDomeShell(vec3 dome, float shellHeight, float rayY) {
    float b    = rayY * dome.x;
    float k    = shellHeight * (2.0 * dome.x + shellHeight);
    float root = sqrt(b * b + k);

    return b > 0.0 ? k / (b + root) : root - b;
}

bool resolveCloudDomeInterval(vec3 dome, vec3 rayDir, float maxHorizontalDistance, out float tEnter, out float tExit) {
    tEnter = intersectCloudDomeShell(dome, dome.y, rayDir.y);
    tExit  = intersectCloudDomeShell(dome, dome.z, rayDir.y);

    float horizontalLength = length(rayDir.xz);

    if (horizontalLength > CLOUD_DOME_EPSILON)
    tExit = min(tExit, maxHorizontalDistance / horizontalLength);

    return tExit > tEnter;
}

// 0 at the layer's base shell, 1 at its top shell.
float resolveCloudDomeHeightFraction(vec3 dome, vec3 relativePosition) {
    float height = length(relativePosition + vec3(0.0, dome.x, 0.0)) - dome.x;
    return (height - dome.y) / max(dome.z - dome.y, CLOUD_DOME_EPSILON);
}

// The shell's local up at a point, which the shading lights against so a
// distant, strongly tilted stretch of the dome reads as edge-on.
vec3 resolveCloudDomeNormal(vec3 dome, vec3 relativePosition) {
    return normalize(relativePosition + vec3(0.0, dome.x, 0.0));
}

#endif
