#version 400 core
layout (location = 0) in vec3 aPos;
layout (location = 7) in vec4 aInstance0; // chunkX (int bits), chunkZ (int bits), localX, localZ
layout (location = 8) in vec4 aInstance1; // centre height, radius, turn, layer
layout (location = 9) in vec2 aInstance2; // fade start, fade direction

#include "includes/CameraData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"
#include "trees/includes/TreeImpostor.glsl"

out vec3       vWorldPos;
out vec2       vQuad;
flat out vec2  vFrameA;
flat out vec2  vFrameB;
flat out vec2  vFrameC;
flat out vec3  vWeights;
flat out float vLayer;
flat out float vTurn;
flat out vec2  vFade;

// One tree drawn far off: the quad stands at the centre of the sphere its impostor was framed on, as wide as that
// sphere, facing the camera, and blends the three frames captured nearest the direction the tree is seen from,
// turned into the tree's own frame by its turn.

void main() {
    float relChunkX = float(floatBitsToInt(aInstance0.x) - u_playerChunkX);
    float relChunkZ = float(floatBitsToInt(aInstance0.y) - u_playerChunkZ);
    vec3  center    = vec3(
        relChunkX * u_chunkSize + aInstance0.z,
        aInstance1.x,
        relChunkZ * u_chunkSize + aInstance0.w);
    vec3  facing    = normalize(u_cameraPosition - center);
    vec3  right     = resolveTreeImpostorRight(facing);
    vec3  up        = cross(facing, right);

    resolveTreeImpostorFrames(turnTreeImpostor(facing, -aInstance1.z), vFrameA, vFrameB, vFrameC, vWeights);

    vWorldPos = center + (right * aPos.x + up * aPos.y) * aInstance1.y;
    vQuad     = aPos.xy * 0.5 + 0.5;
    vLayer    = aInstance1.w;
    vTurn     = aInstance1.z;
    vFade     = aInstance2;

    gl_Position = u_viewProjection * vec4(vWorldPos, 1.0);
}
