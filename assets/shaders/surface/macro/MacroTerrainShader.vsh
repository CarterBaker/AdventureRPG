#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in float aColor;

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"

out vec3 vLocalPos;
out vec2 vTilePos;
out vec3 vColor;

// Places one distant macro tile. Positions are tile-local and u_gridPosition carries the tile's origin
// against the grid's active chunk, the same frame every chunk renders in. Vertex tint is an exact 24-bit
// RGB triple packed by MacroBuildBranch. The grid-relative position leaves for the fragment's facet normal,
// and the tile-local one for its chunk coverage test.

void main() {
    vec3 worldPos = aPos;
    worldPos.x   += u_gridPosition.x;
    worldPos.z   += u_gridPosition.y;

    int col = int(aColor);

    vLocalPos = worldPos;
    vTilePos  = aPos.xz;
    vColor    = vec3(float((col >> 16) & 255),
        float((col >>  8) & 255),
        float(col        & 255)) * (1.0 / 255.0);

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}
