#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in vec2  aUVOrigin;
layout (location = 2) in float aMeta;
layout (location = 3) in float aColor;
layout (location = 4) in vec4  aEdgeLow;
layout (location = 5) in vec4  aEdgeHigh;

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"
#include "trees/includes/TreeBark.glsl"

out vec3      vWorldPos;
out vec3      vModelPos;
out vec2      vLocal;
flat out vec2 vSize;
flat out vec2 vUVOrigin;
flat out vec3 vColor;
flat out int  vFace;
flat out int  vEdges;

// Wood standing in a chunk: positions are the chunk's own, placed by the grid slot it is drawn in.

void main() {
    vWorldPos = vec3(aPos.x + u_gridPosition.x, aPos.y, aPos.z + u_gridPosition.y);
    vModelPos = aPos;
    vLocal    = aEdgeLow.xy;
    vSize     = aEdgeLow.zw;
    vUVOrigin = aUVOrigin;
    vColor    = unpackTreeColor(aColor);
    vFace     = unpackTreeFace(aMeta);
    vEdges    = unpackTreeEdges(aMeta);

    gl_Position = u_viewProjection * vec4(vWorldPos, 1.0);
}
