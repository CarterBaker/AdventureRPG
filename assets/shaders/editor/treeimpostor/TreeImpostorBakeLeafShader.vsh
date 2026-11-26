#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in float aTile;
layout (location = 2) in float aMeta;
layout (location = 3) in float aColor;
layout (location = 4) in vec4  aEdgeLow;
layout (location = 5) in vec4  aEdgeHigh;

#include "trees/includes/TreeLeaf.glsl"

// The view of one impostor frame: an orthographic camera framed on the shared tree's sphere.
uniform mat4 u_bakeViewProjection;

out vec3       vWorldPos;
out vec2       vCard;
flat out vec3  vCenter;
flat out vec2  vRadii;
flat out vec2  vCardRadii;
flat out float vUnderside;
flat out float vSeed;
flat out int   vTile;
flat out vec3  vColor;

// The shared tree's leaves, still, in blocks from the centre of its root, seen from one frame's direction.
void main() {
    vec2 radii = vec2(aEdgeLow.w, aEdgeHigh.x);
    int  card  = int(aMeta);

    vWorldPos  = aPos;
    vCard      = aEdgeHigh.zw;
    vCenter    = aPos + aEdgeLow.xyz;
    vRadii     = radii;
    vCardRadii = resolveTreeLeafCardRadii(card, radii);
    vUnderside = resolveTreeLeafCardUnderside(card);
    vSeed      = resolveTreeLeafCardSeed(aEdgeHigh.y, card);
    vTile      = int(aTile + 0.5);
    vColor     = unpackTreeColor(aColor);

    gl_Position = u_bakeViewProjection * vec4(aPos, 1.0);
}
