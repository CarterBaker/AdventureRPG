#version 400 core

in vec3  vWorldPos;
flat in vec3 vFaceNormal;
in float vTidal;
in vec4  vOceanState;

out vec4 FragColor;

#include "includes/MacroWaterData.glsl"
#include "liquid/includes/WaterShading.glsl"

// The distant sea plane, cut per pixel before any shading: it shows only where the open water mask says the
// sea stands over the terrain, so a dry basin below sea level stays dry, and never over a chunk the grid draws
// itself, whose own water takes over there. What remains is shaded exactly as the voxel sea is.

void main() {
    if (!isMacroWaterOpen(vWorldPos.xz) || isDrawnByChunkGridAround(vWorldPos.xz))
    discard;

    WaterSurface surface;

    if (!resolveWaterSurface(WaterFragment(vWorldPos, vFaceNormal, vTidal, vOceanState), gl_FragCoord.xyz, surface))
    discard;

    vec2 foamEdges = max(fwidth(surface.foamValues), vec2(WATER_FOAM_EDGE_MIN));

    FragColor = vec4(shadeWaterSurface(surface, foamEdges), 1.0);
}
