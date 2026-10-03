#version 400 core

in vec3  vWorldPos;
flat in vec3 vFaceNormal;
in float vTidal;
in vec4  vOceanState;

out vec4 FragColor;

#include "liquid/includes/WaterShading.glsl"

// The voxel sea: liquid faces of loaded chunks, tessellated and displaced by the tide and waves, shaded by
// WaterShading.glsl.

void main() {
    WaterSurface surface;

    if (!resolveWaterSurface(WaterFragment(vWorldPos, vFaceNormal, vTidal, vOceanState), gl_FragCoord.xyz, surface))
    discard;

    vec2 foamEdges = max(fwidth(surface.foamValues), vec2(WATER_FOAM_EDGE_MIN));

    FragColor = vec4(shadeWaterSurface(surface, foamEdges), 1.0);
}
