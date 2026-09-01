#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in float aNorIndex;
layout (location = 2) in float aColor;
layout (location = 3) in vec2  aUVOrigin;
layout (location = 4) in float aOrient;
layout (location = 5) in float aQuadSize;
layout (location = 6) in float aFluidLevel; // 0..LIQUID_LEVEL_MAX — same vertex slot FullGeometryBranch uses for bevel masks on solid blocks
layout (location = 7) in float aFluidTop;   // 1.0 on vertices sitting at this face's surface height, 0.0 otherwise
layout (location = 8) in float aBevelMaskB0; // unused for liquid
layout (location = 9) in float aBevelMaskB1; // unused for liquid

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/WorldCurvature.glsl"
#include "includes/WorldDistantRise.glsl"
#include "surface/includes/SurfaceTessellationTier.glsl"

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
const float LIQUID_LEVEL_MAX = 8.0;

out vec3 vWorldNormal;

void main() {
    vec3 worldPos = aPos;
    worldPos.x   += u_gridPosition.x;
    worldPos.z   += u_gridPosition.y;

    if (aFluidTop > 0.5)
    worldPos.y -= (1.0 - clamp(aFluidLevel / LIQUID_LEVEL_MAX, 0.0, 1.0));

    vWorldNormal = NORMALS[int(aNorIndex)];

    // Water previously skipped both world bends entirely, so a shoreline
    // visibly split away from the land it borders at any real distance.
    // Applying the exact same shared functions the terrain uses keeps a
    // water surface glued to its bank at every range. Gating is resolved
    // from this vertex's own world position via computeDistanceFromCenterSq()
    // rather than the shared-per-slot u_distanceFromCenter, so a shoreline
    // never shows a step between an individually rendered chunk and a mega
    // — see SurfaceTessellationTier.glsl.
    if (computeDistanceFromCenterSq(worldPos) > getTier1MaxSqDist())
    worldPos = applyDistantRise(worldPos);

    worldPos = applyWorldCurvature(worldPos);

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}