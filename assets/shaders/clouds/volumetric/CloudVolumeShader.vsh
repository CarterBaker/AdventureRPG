// CloudVolumeShader.vsh
#version 330 core

layout (location = 0) in vec3 aPos;

#include "includes/CameraData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"
#include "includes/WeatherMapData.glsl"

flat out int  vInstanceID;
out vec3      vLocalPos;
flat out vec3 vBoxCenter;
flat out vec3 vBoxHalfExtent;

float hash01(float n) {
    return fract(sin(n) * 43758.5453123);
}

void main() {
    vInstanceID = gl_InstanceID;

    vec4 bounds    = u_weatherBounds[gl_InstanceID];
    vec4 shape     = u_weatherCloudShape[gl_InstanceID];
    vec4 variance0 = u_weatherCloudVariance0[gl_InstanceID];
    vec4 variance1 = u_weatherCloudVariance1[gl_InstanceID];

    float chunkSizeBlocks = u_chunkSize;

    float relMinX = (bounds.x - float(u_playerChunkX)) * chunkSizeBlocks;
    float relMinZ = (bounds.y - float(u_playerChunkZ)) * chunkSizeBlocks;
    float relMaxX = (bounds.z - float(u_playerChunkX)) * chunkSizeBlocks;
    float relMaxZ = (bounds.w - float(u_playerChunkZ)) * chunkSizeBlocks;

    float centerX = (relMinX + relMaxX) * 0.5;
    float centerZ = (relMinZ + relMaxZ) * 0.5;
    float halfX   = max((relMaxX - relMinX) * 0.5, 0.01);
    float halfZ   = max((relMaxZ - relMinZ) * 0.5, 0.01);
    float centerY = shape.y;
    float halfY   = max(shape.x * 0.5, 0.01);

    // Per-instance footprint variance, stable across frames via patternSeed,
    // so identical bounds never read as a grid of identical rectangular slabs.
    float seed     = variance1.z;
    float sizeT     = hash01(seed * 12.9898);
    float elongateT = hash01(seed * 78.233 + 3.7);
    float axisPick  = hash01(seed * 37.719 + 9.1);

    float sizeScale  = mix(variance0.y, variance0.z, sizeT);
    float elongation = mix(variance0.w, variance1.x, elongateT);

    halfX *= sizeScale;
    halfZ *= sizeScale;

    if (axisPick > 0.5)
    halfX *= elongation;
    else
    halfZ *= elongation;

    vec3 localPos = aPos - vec3(0.5);

    vBoxCenter     = vec3(centerX, centerY, centerZ);
    vBoxHalfExtent = vec3(halfX, halfY, halfZ);
    vLocalPos      = localPos;

    vec3 worldPos = vBoxCenter + localPos * vBoxHalfExtent * 2.0;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}