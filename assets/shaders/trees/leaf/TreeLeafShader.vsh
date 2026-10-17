#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in vec2  aUVOrigin;
layout (location = 2) in float aMeta;
layout (location = 3) in float aColor;
layout (location = 4) in vec4  aEdgeLow;
layout (location = 5) in vec4  aEdgeHigh;

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"
#include "trees/includes/TreeLeaf.glsl"

out vec3       vWorldPos;
out vec2       vCard;
flat out vec3  vCenter;
flat out vec2  vRadii;
flat out vec2  vCardRadii;
flat out float vUnderside;
flat out float vSeed;
flat out vec2  vUVOrigin;
flat out vec3  vColor;

// Leaves standing in a chunk: positions are the chunk's own, placed by the grid slot they are drawn in, and the
// cluster's centre is found from each vertex, so every card of it sways as one.

void main() {
    vec3 position = vec3(aPos.x + u_gridPosition.x, aPos.y, aPos.z + u_gridPosition.y);
    vec3 center   = position + aEdgeLow.xyz;
    vec3 sway     = resolveTreeLeafSway(center, aEdgeHigh.y);
    vec2 radii    = vec2(aEdgeLow.w, aEdgeHigh.x);
    int  card     = int(aMeta);

    vWorldPos  = position + sway;
    vCard      = aEdgeHigh.zw;
    vCenter    = center + sway;
    vRadii     = radii;
    vCardRadii = resolveTreeLeafCardRadii(card, radii);
    vUnderside = resolveTreeLeafCardUnderside(card);
    vSeed      = resolveTreeLeafCardSeed(aEdgeHigh.y, card);
    vUVOrigin  = aUVOrigin;
    vColor     = unpackTreeColor(aColor);

    gl_Position = u_viewProjection * vec4(vWorldPos, 1.0);
}
