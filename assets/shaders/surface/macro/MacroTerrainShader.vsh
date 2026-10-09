#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in float aTopColor;
layout (location = 2) in float aSideColor;
layout (location = 3) in float aCoverChunk;

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"

out vec3 vLocalPos;
out vec2 vTilePos;
out vec3 vTopColor;
out vec3 vSideColor;
flat out int vCoverChunk;

// Places one distant macro tile. Positions are tile-local and u_gridPosition carries the tile's origin
// against the grid's active chunk, the same frame every chunk renders in. The top and slope colors are exact
// 24-bit RGB triples packed by MacroBuildBranch, the average albedo of the blocks a chunk dresses that ground
// with. The grid-relative position leaves for the fragment's facet normal and cloud shadow, and the
// tile-local one for its chunk coverage test. A tree stand-in names the chunk of the tile its tree roots in, so
// the whole tree yields with that chunk; ground and canopy name none and yield wherever they lie.

vec3 unpackColor(float packedColor) {
    int col = int(packedColor);

    return vec3(float((col >> 16) & 255),
        float((col >>  8) & 255),
        float(col        & 255)) * (1.0 / 255.0);
}

void main() {
    vec3 worldPos = aPos;
    worldPos.x   += u_gridPosition.x;
    worldPos.z   += u_gridPosition.y;

    vLocalPos  = worldPos;
    vTilePos   = aPos.xz;
    vTopColor  = unpackColor(aTopColor);
    vSideColor = unpackColor(aSideColor);
    vCoverChunk = int(aCoverChunk);

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}
