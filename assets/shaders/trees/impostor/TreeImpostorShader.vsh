#version 400 core
layout (location = 0) in vec3 aPos;
layout (location = 7) in vec4 aInstance0; // chunkX (int bits), chunkZ (int bits), localX, localZ
layout (location = 8) in vec4 aInstance1; // centre height, radius, turn, layer

#include "includes/CameraData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"
#include "trees/includes/TreeImpostor.glsl"

out vec3       vWorldPos;
out vec2       vAtlas;
flat out float vLayer;
flat out float vTurn;

// One tree drawn far off: the quad stands at the centre of the sphere its impostor was framed on, as wide as that
// sphere, facing the camera, and shows the frame captured nearest the direction the tree is seen from, turned into
// the tree's own frame by its turn.

void main() {
    float relChunkX = float(floatBitsToInt(aInstance0.x) - u_playerChunkX);
    float relChunkZ = float(floatBitsToInt(aInstance0.y) - u_playerChunkZ);
    vec3  center    = vec3(
        relChunkX * u_chunkSize + aInstance0.z,
        aInstance1.x,
        relChunkZ * u_chunkSize + aInstance0.w);
    float radius    = aInstance1.y;
    vec3  facing    = normalize(u_cameraPosition - center);
    vec3  right     = resolveTreeImpostorRight(facing);
    vec3  up        = cross(facing, right);
    vec2  frame     = resolveTreeImpostorFrame(turnTreeImpostor(facing, -aInstance1.z));

    vWorldPos = center + (right * aPos.x + up * aPos.y) * radius;
    vAtlas    = (frame + aPos.xy * 0.5 + 0.5) / TREE_IMPOSTOR_FRAMES;
    vLayer    = aInstance1.w;
    vTurn     = aInstance1.z;

    gl_Position = u_viewProjection * vec4(vWorldPos, 1.0);
}
