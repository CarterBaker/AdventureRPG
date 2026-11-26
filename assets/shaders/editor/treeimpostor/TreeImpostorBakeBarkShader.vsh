#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in float aTile;
layout (location = 2) in float aMeta;
layout (location = 3) in float aColor;
layout (location = 4) in vec4  aEdgeLow;
layout (location = 5) in vec4  aEdgeHigh;

#include "includes/CameraData.glsl"
#include "trees/includes/TreeBark.glsl"

// The view of one impostor frame: an orthographic camera framed on the shared tree's sphere.
uniform mat4 u_bakeViewProjection;

out vec3      vWorldPos;
out vec3      vModelPos;
out vec2      vLocal;
flat out vec2 vSize;
flat out int  vTile;
flat out vec3 vColor;
flat out int  vFace;
flat out int  vEdges;

// The shared tree's wood, in blocks from the centre of its root, seen from one frame's direction.
void main() {
    vWorldPos = aPos;
    vModelPos = aPos;
    vLocal    = aEdgeLow.xy;
    vSize     = aEdgeLow.zw;
    vTile     = int(aTile + 0.5);
    vColor    = unpackTreeColor(aColor);
    vFace     = unpackTreeFace(aMeta);
    vEdges    = unpackTreeEdges(aMeta);

    gl_Position = u_bakeViewProjection * vec4(aPos, 1.0);
}
