#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in vec2  aUVOrigin;
layout (location = 2) in float aMeta;
layout (location = 3) in float aColor;
layout (location = 4) in vec4  aEdgeLow;
layout (location = 5) in vec4  aEdgeHigh;

#include "includes/CameraData.glsl"
#include "trees/includes/TreeLeaf.glsl"

// Carries the falling piece, in blocks from the centre of its tree's root, into the world.
uniform mat4 u_model;

out vec3       vWorldPos;
out vec2       vCard;
flat out vec3  vCenter;
flat out vec2  vRadii;
flat out vec2  vCardRadii;
flat out float vUnderside;
flat out float vSeed;
flat out vec2  vUVOrigin;
flat out vec3  vColor;

// A falling cluster's cards turn with the piece about the cluster's own centre, so the sheaf falls whole.

void main() {
    vec2 radii = vec2(aEdgeLow.w, aEdgeHigh.x);
    int  card  = int(aMeta);

    vWorldPos  = (u_model * vec4(aPos, 1.0)).xyz;
    vCard      = aEdgeHigh.zw;
    vCenter    = (u_model * vec4(aPos + aEdgeLow.xyz, 1.0)).xyz;
    vRadii     = radii;
    vCardRadii = resolveTreeLeafCardRadii(card, radii);
    vUnderside = resolveTreeLeafCardUnderside(card);
    vSeed      = resolveTreeLeafCardSeed(aEdgeHigh.y, card);
    vUVOrigin  = aUVOrigin;
    vColor     = unpackTreeColor(aColor);

    gl_Position = u_viewProjection * vec4(vWorldPos, 1.0);
}
