#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in float aNorIndex;
layout (location = 2) in float aColor;
layout (location = 3) in vec2  aUVOrigin;
layout (location = 4) in float aOrient;
layout (location = 5) in float aQuadSize;
layout (location = 6) in float aBevelMaskA0;
layout (location = 7) in float aBevelMaskA1;
layout (location = 8) in float aBevelMaskB0;
layout (location = 9) in float aBevelMaskB1;
layout (location = 10) in float aBevelNegMaskA0;
layout (location = 11) in float aBevelNegMaskA1;
layout (location = 12) in float aBevelNegMaskB0;
layout (location = 13) in float aBevelNegMaskB1;

#include "includes/GridCoordinateData.glsl"
#include "includes/SettingsData.glsl"

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
out float tcBevelNegMaskA0;
out float tcBevelNegMaskA1;
out float tcBevelNegMaskB0;
out float tcBevelNegMaskB1;

// Places raw block-face geometry in world space and passes vertex-encoded quad/bevel data through untouched; per-vertex displacement happens after tessellation in StandardSurfaceShader.tes, since tessellation only ever sees a merged quad's four real corners.

void main() {
    vec3 worldPos  = aPos;
    worldPos.x    += u_gridPosition.x;
    worldPos.z    += u_gridPosition.y;

    vec3 normal = NORMALS[int(aNorIndex)];

    gl_Position       = vec4(worldPos, 1.0);
    tcWorldPos        = worldPos;
    tcNormal          = normal;
    tcUVOrigin        = aUVOrigin;
    tcOrient          = aOrient;
    tcColor           = aColor;
    tcQuadSize        = aQuadSize;
    tcBevelMaskA0     = aBevelMaskA0;
    tcBevelMaskA1     = aBevelMaskA1;
    tcBevelMaskB0     = aBevelMaskB0;
    tcBevelMaskB1     = aBevelMaskB1;
    tcBevelNegMaskA0  = aBevelNegMaskA0;
    tcBevelNegMaskA1  = aBevelNegMaskA1;
    tcBevelNegMaskB0  = aBevelNegMaskB0;
    tcBevelNegMaskB1  = aBevelNegMaskB1;
}