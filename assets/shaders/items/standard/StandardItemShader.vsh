#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;
layout(location = 3) in vec4  aInstance0; // chunkX(intBits), chunkZ(intBits), localX, localZ
layout(location = 4) in vec2  aInstance1; // localY, orientation

#include "includes/CameraData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/ItemRotationData.glsl"

const vec3 NORMALS[6] = vec3[](
    vec3(0, 0, 1), vec3(1, 0, 0), vec3(0, 0,-1),
    vec3(-1, 0, 0), vec3(0, 1, 0), vec3(0,-1, 0));

out vec3 vNormal;
out vec2 vUV;

void main() {
    mat4 R      = u_rotations[int(aInstance1.y)];
    vec3 rotPos = (R * vec4(aPos - vec3(0.5), 1.0)).xyz + vec3(0.5);
    vNormal     = normalize(mat3(R) * NORMALS[int(aNorIndex)]);
    vUV         = aUV;

    int chunkX = floatBitsToInt(aInstance0.x);
    int chunkZ = floatBitsToInt(aInstance0.y);
    float relChunkX = float(chunkX - u_playerChunkX);
    float relChunkZ = float(chunkZ - u_playerChunkZ);

    vec3 worldPos = vec3(
        relChunkX * 16.0 + aInstance0.z + rotPos.x,
        aInstance1.x     + rotPos.y,
        relChunkZ * 16.0 + aInstance0.w + rotPos.z);

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}
