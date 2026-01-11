#ifndef SKY_ROTATION_GLSL
#define SKY_ROTATION_GLSL

#include "includes/TimeData.glsl"

// Rotates a vector around Y axis (up) by angle radians
vec3 rotateY(vec3 v, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec3(
        v.x * c - v.z * s,
        v.y,
        v.x * s + v.z * c);
}

// Rotates a vector around X axis (right) by angle radians
vec3 rotateX(vec3 v, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec3(
        v.x,
        v.y * c - v.z * s,
        v.y * s + v.z * c);
}

// Rotates a vector around Z axis (forward) by angle radians
vec3 rotateZ(vec3 v, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec3(
        v.x * c - v.y * s,
        v.x * s + v.y * c,
        v.z);
}

// Returns a dynamically rotated direction based on time of day/year
vec3 getDynamicDir(vec3 dir) {
    // Convert 0-1 time to 0-2π radians
    float daily = u_timeOfDay * 6.28318530718;    // 2π
    float yearly = u_timeOfYear * 6.28318530718;  // 2π

    // Daily rotation around Y axis (sun movement)
    vec3 rotated = rotateY(dir, daily);

    // Yearly tilt: rotate around X axis to simulate seasonal sky tilt
    rotated = rotateX(rotated, yearly);

    return rotated;
}

#endif
