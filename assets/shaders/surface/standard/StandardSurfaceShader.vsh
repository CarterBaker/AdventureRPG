#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in vec2  aUVOrigin;
layout (location = 2) in float aMeta;
layout (location = 3) in float aColor;
layout (location = 4) in float aEdgeA0;
layout (location = 5) in float aEdgeA1;
layout (location = 6) in float aEdgeB0;
layout (location = 7) in float aEdgeB1;

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
out vec3  tcColor;
out float tcSizeA;
out float tcSizeB;
out float tcNatural;
out vec4  tcEdge;

// Places raw block-face geometry in world space and unpacks the two integer-packed vertex attributes.
// Face index, encoded face orientation, both merged quad extents and the natural-block flag share one
// 18-bit word, vertex tint is an exact 24-bit RGB triple, and each of the four per-edge state words packs
// two bits per unit cell across a quad capped at ten cells plus one padding cell at either end, which is
// twenty-four bits and therefore exact in a float32 mantissa. Per-vertex displacement still happens after
// tessellation, since tessellation only ever sees a merged quad's four real corners.

void main() {
    vec3 worldPos  = aPos;
    worldPos.x    += u_gridPosition.x;
    worldPos.z    += u_gridPosition.y;

    int meta = int(aMeta);
    int col  = int(aColor);

    gl_Position = vec4(worldPos, 1.0);
    tcWorldPos  = worldPos;
    tcNormal    = NORMALS[meta & 7];
    tcUVOrigin  = aUVOrigin;
    tcOrient    = float((meta >> 3) & 63);
    tcSizeA     = float(((meta >>  9) & 15) + 1);
    tcSizeB     = float(((meta >> 13) & 15) + 1);
    tcNatural   = float((meta >> 17) & 1);
    tcColor     = vec3(float((col >> 16) & 255),
        float((col >>  8) & 255),
        float(col        & 255)) * (1.0 / 255.0);
    tcEdge      = vec4(aEdgeA0, aEdgeA1, aEdgeB0, aEdgeB1);
}