#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in float aNorIndex;
layout (location = 2) in float aColor;
layout (location = 3) in vec2  aUVOrigin;
layout (location = 4) in float aOrient;
layout (location = 5) in float aQuadSize;
layout (location = 6) in float aBevelMaskA0;  // A=0 edge exposure,    bit j = cell j along B
layout (location = 7) in float aBevelMaskA1;  // A=sizeA edge exposure, bit j = cell j along B
layout (location = 8) in float aBevelMaskB0;  // B=0 edge exposure,    bit i = cell i along A
layout (location = 9) in float aBevelMaskB1;  // B=sizeB edge exposure, bit i = cell i along A

#include "includes/GridCoordinateData.glsl"

const vec3 NORMALS[6] = vec3[](
    vec3(0, 0, 1),
    vec3(1, 0, 0),
    vec3(0, 0,-1),
    vec3(-1, 0, 0),
    vec3(0, 1, 0),
    vec3(0,-1, 0));

out vec3  tcWorldPos;
out vec3  tcNormal;
out vec2  tcUVOrigin;
out float tcOrient;
out float tcColor;
out float tcQuadSize;
out float tcBevelMaskA0;
out float tcBevelMaskA1;
out float tcBevelMaskB0;
out float tcBevelMaskB1;

void main() {
    vec3 worldPos  = aPos;
    worldPos.x    += u_gridPosition.x;
    worldPos.z    += u_gridPosition.y;
    gl_Position    = vec4(worldPos, 1.0);
    tcWorldPos     = worldPos;
    tcNormal       = NORMALS[int(aNorIndex)];
    tcUVOrigin     = aUVOrigin;
    tcOrient       = aOrient;
    tcColor        = aColor;
    tcQuadSize     = aQuadSize;
    tcBevelMaskA0  = aBevelMaskA0;
    tcBevelMaskA1  = aBevelMaskA1;
    tcBevelMaskB0  = aBevelMaskB0;
    tcBevelMaskB1  = aBevelMaskB1;
}