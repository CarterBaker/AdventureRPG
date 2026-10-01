#version 330 core

in vec2 v_screenPos;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "liquid/includes/OceanSurface.glsl"
#include "liquid/includes/WaterLight.glsl"
#include "liquid/includes/WaterSceneData.glsl"

/*
 * Drawn only while the camera is under the water, before the water surface, into the water target. Every
 * pixel of the lit scene is pulled toward the deep-water color with the distance it is seen through, tinted
 * and dimmed with the camera's depth below the surface, and swayed by a gentle current. Ground below the
 * surface catches the same drifting caustics shallow water shows from above, and nothing is seen past the
 * murk where the scene is empty. The water surface then draws over this from below.
 */

const float UNDERWATER_SWAY_STRENGTH  = 0.0025;
const float UNDERWATER_SWAY_FREQUENCY = 22.0;
const float UNDERWATER_SWAY_SPEED     = 1.3;
const float UNDERWATER_OPEN_DISTANCE  = 4096.0;

void main() {
    vec2  uv     = v_screenPos * 0.5 + 0.5;
    float time   = u_oceanSurface.w * UNDERWATER_SWAY_SPEED;
    vec2  sway   = vec2(sin(uv.y * UNDERWATER_SWAY_FREQUENCY + time), cos(uv.x * UNDERWATER_SWAY_FREQUENCY + time))
        * UNDERWATER_SWAY_STRENGTH;
    vec2  swayUV = clamp(uv + sway, vec2(0.0), vec2(1.0));

    float cameraDepth = u_oceanCamera.z;
    float depth       = sampleWaterSceneDepth(swayUV);

    if (depth >= 1.0) {
        fragColor = vec4(applyUnderwaterFog(vec3(0.0), UNDERWATER_OPEN_DISTANCE, cameraDepth), 1.0);
        return;
    }

    vec3 viewPos  = reconstructWaterViewPos(swayUV, depth);
    vec3 worldPos = viewToWorld(viewPos);
    vec3 color    = sampleWaterSceneColor(swayUV);

    if (worldPos.y < u_oceanCamera.y)
    color *= 1.0 + sampleWaterCaustics(resolveOceanLattice(worldPos.xz), u_oceanCamera.y - worldPos.y);

    fragColor = vec4(applyUnderwaterFog(color, length(viewPos), cameraDepth), 1.0);
}
