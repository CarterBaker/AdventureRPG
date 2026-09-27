#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in float aColor;

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"
#include "includes/WorldCurvature.glsl"

out vec3 vLocalPos;
out vec3 vColor;

// Places one distant macro tile. Positions are tile-local and u_gridPosition carries the tile's origin
// against the grid's active chunk, the same frame every chunk renders in. Vertex tint is an exact 24-bit
// RGB triple packed by MacroBuildBranch. The flat position leaves for the fragment's facet normal and
// grid test, and only the projected position is bent by the shared world curvature, so distant terrain
// meets the chunk grid's own horizon.

void main() {
    vec3 worldPos = aPos;
    worldPos.x   += u_gridPosition.x;
    worldPos.z   += u_gridPosition.y;

    int col = int(aColor);

    vLocalPos = worldPos;
    vColor    = vec3(float((col >> 16) & 255),
        float((col >>  8) & 255),
        float(col        & 255)) * (1.0 / 255.0);

    gl_Position = u_viewProjection * vec4(applyWorldCurvature(worldPos), 1.0);
}
