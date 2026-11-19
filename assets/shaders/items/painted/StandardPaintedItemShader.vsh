#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in vec3  aNormal;
layout(location = 2) in vec2  aUV;
layout(location = 3) in float aTile;
layout(location = 4) in vec4  aInstance0; // chunkX(intBits), chunkZ(intBits), localX, localZ
layout(location = 5) in vec2  aInstance1; // localY, orientation

#include "includes/CameraData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/ItemRotationData.glsl"
#include "includes/SettingsData.glsl"

out vec3 vNormalView;
out vec3 vViewPosition;
out vec2 vUV;
flat out int vTile;

// A painted item placed in the world, turned about its block's middle and carried to its chunk exactly as a
// sub-voxel item is, its own normal turned with it.
void main() {
    mat4 R      = u_rotations[int(aInstance1.y)];
    vec3 rotPos = (R * vec4(aPos - vec3(0.5), 1.0)).xyz + vec3(0.5);
    vNormalView = normalize(mat3(u_view) * (mat3(R) * aNormal));
    vUV         = aUV;
    vTile       = int(aTile + 0.5);

    int chunkX = floatBitsToInt(aInstance0.x);
    int chunkZ = floatBitsToInt(aInstance0.y);
    float relChunkX = float(chunkX - u_playerChunkX);
    float relChunkZ = float(chunkZ - u_playerChunkZ);

    vec3 worldPos = vec3(
        relChunkX * u_chunkSize + aInstance0.z + rotPos.x,
        aInstance1.x            + rotPos.y,
        relChunkZ * u_chunkSize + aInstance0.w + rotPos.z);

    vViewPosition = (u_view * vec4(worldPos, 1.0)).xyz;
    gl_Position   = u_viewProjection * vec4(worldPos, 1.0);
}
