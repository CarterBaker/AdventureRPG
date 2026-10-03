#version 400 core

layout (location = 0) in vec2 aPos;

#include "includes/CameraData.glsl"
#include "liquid/includes/OceanSurface.glsl"

out vec3  vWorldPos;
flat out vec3 vFaceNormal;
out float vTidal;
out vec4  vOceanState;

// The distant sea plane. Corners arrive in the grid frame, relative to the active chunk's origin, and are
// lifted to the live tide, so the plane rises and falls with the sea the voxel water rides and never opens a
// gap against it. Beyond the wave radius the voxel sea lies flat at that same height, so the plane carries no
// displacement of its own, only the sea state at each corner, which the fragment reads for its slope.

void main() {
    vec3       worldPos = vec3(aPos.x, u_oceanSurface.x, aPos.y);
    OceanState state    = resolveOceanState(worldPos.xz);

    vWorldPos   = worldPos;
    vFaceNormal = vec3(0.0, 1.0, 0.0);
    vTidal      = 1.0;
    vOceanState = vec4(state.seaState, state.chop, state.swell, 0.0);

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}
