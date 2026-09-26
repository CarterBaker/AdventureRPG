#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in vec2  aUVOrigin;   // unused for liquid — same ChunkVAO layout StandardSurfaceShader reads
layout (location = 2) in float aMeta;       // face index in bits 0-2, packed exactly like solid geometry
layout (location = 3) in float aColor;      // unused for liquid
layout (location = 4) in vec4  aLiquid;     // x: fluid level 0..LIQUID_LEVEL_MAX, y: 1.0 on surface-height vertices,
                                            // z: 1.0 on tidal ocean surface vertices, w: unused
layout (location = 5) in vec4  aEdgeHigh;   // unused for liquid — solid geometry's high edge words

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/WorldCurvature.glsl"
#include "liquid/includes/OceanSurface.glsl"

const vec3 NORMALS[6] = vec3[](
    vec3(0, 0, 1),
    vec3(1, 0, 0),
    vec3(0, 0,-1),
    vec3(-1, 0, 0),
    vec3(0, 1, 0),
    vec3(0,-1, 0));

// Must match EngineSetting.LIQUID_LEVEL_MAX — GLSL has no visibility into
// the Java constant, so this is a manually-kept mirror, same convention
// StandardSurface.tes already uses for its own constant re-derivations.
const float LIQUID_LEVEL_MAX = 64.0;

out vec3  vWorldNormal;
out vec3  vOceanPos;
out float vOceanSurface;
out float vTurbulence;

void main() {
    vec3 worldPos = aPos;
    worldPos.x   += u_gridPosition.x;
    worldPos.z   += u_gridPosition.y;

    vOceanSurface = 0.0;
    vTurbulence   = 0.0;

    // Tidal ocean surface vertices ignore the level their cell was last
    // written at and ride the live tide plus the swell the turbulence field
    // raises here, so the sea rises, falls, and rolls continuously at every
    // range — including chunks the tide pass has not yet re-levelled.
    if (aLiquid.z > 0.5) {
        vTurbulence   = sampleOceanTurbulence(worldPos.xz);
        worldPos.y    = u_oceanSurface.x + oceanWaveAmplitude(vTurbulence) * sampleOceanSwell(worldPos.xz);
        vOceanSurface = 1.0;
    }
    else if (aLiquid.y > 0.5)
    worldPos.y -= (1.0 - clamp(aLiquid.x / LIQUID_LEVEL_MAX, 0.0, 1.0));

    vWorldNormal = NORMALS[int(aMeta) & 7];
    vOceanPos    = worldPos;

    // The exact same world bend the terrain uses, measured from the
    // player's true position, keeps a water surface glued to its bank at
    // every range.
    worldPos = applyWorldCurvature(worldPos);

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}