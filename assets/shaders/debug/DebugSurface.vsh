#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;

#include "includes/GridCoordinateData.glsl"
#include "includes/PlayerPositionData.glsl"

out vec3 vWorldNormal;

void main()
{
    // Calculate normal (unchanged)
    vec3 normalMatrix = normalize(aNormal);
    vWorldNormal = normalMatrix;
    
    // Start with vertex position (chunk-local, centered at 0,0,0)
    vec3 worldPos = aPos;
    
    // Apply grid offset (grid coordinates as X and Z offset)
    worldPos.x += u_gridPosition.x * 16.0; // Assuming CHUNK_SIZE = 16
    worldPos.z += u_gridPosition.y * 16.0;
    
    // Apply player position offset
    worldPos -= u_playerPosition;
    
    // Transform to clip space
    gl_Position = vec4(worldPos, 1.0);
}