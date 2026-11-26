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
 * murk where the scene is empty. Bubbles rise through it in the cartoon's way, a bright rim and a highlight on
 * a faint middle: each stands in a column fixed in the world, one in most cells of a grid laid across the water,
 * and the view ray meets the bubble of each column near it at its closest approach, in three bands each twice as
 * far as the last with cells twice as wide, so bubbles keep their place as the camera turns and slide past with
 * true parallax as it moves. A bubble behind the scene or above the surface is never drawn. The water surface then
 * draws over this from below.
 */

const float UNDERWATER_SWAY_STRENGTH  = 0.0025;
const float UNDERWATER_SWAY_FREQUENCY = 22.0;
const float UNDERWATER_SWAY_SPEED     = 1.3;
const float UNDERWATER_OPEN_DISTANCE  = 4096.0;

const int   UNDERWATER_BUBBLE_BANDS       = 3;
const float UNDERWATER_BUBBLE_NEAR        = 1.5;
const float UNDERWATER_BUBBLE_CELL        = 1.2;
const float UNDERWATER_BUBBLE_SHARE       = 0.55;
const float UNDERWATER_BUBBLE_PERIOD      = 2.4;
const float UNDERWATER_BUBBLE_RISE        = 1.1;
const float UNDERWATER_BUBBLE_RADIUS      = 0.06;
const float UNDERWATER_BUBBLE_WOBBLE      = 0.08;
const float UNDERWATER_BUBBLE_WOBBLE_RATE = 2.2;
const float UNDERWATER_BUBBLE_MIDDLE      = 0.25;
const float UNDERWATER_BUBBLE_OPACITY     = 0.6;
const float UNDERWATER_BUBBLE_GLINT       = 0.8;
const float UNDERWATER_BUBBLE_EDGE_SLOPE  = 0.002;
const vec3  UNDERWATER_BUBBLE_COLOR       = vec3(0.85, 0.95, 1.0);

// The bubble of one column nearest the view ray, written as the pixel's share of its rim and middle into x and of
// its highlight into y; nothing where the column stands out of the ray's way, behind the scene or above the surface
vec2 resolveBubble(vec3 origin, vec3 dir, vec2 column, float sceneDistance, float time, float phase) {

    vec2  across = dir.xz;
    float spread = dot(across, across);

    if (spread < 0.0001)
    return vec2(0.0);

    float reach  = dot(column - origin.xz, across) / spread;
    float rise   = phase * UNDERWATER_BUBBLE_PERIOD + time * UNDERWATER_BUBBLE_RISE;
    float height = origin.y + dir.y * reach;
    float index  = floor((height - rise) / UNDERWATER_BUBBLE_PERIOD + 0.5);
    float wobble = sin(time * UNDERWATER_BUBBLE_WOBBLE_RATE + index + phase * 6.0) * UNDERWATER_BUBBLE_WOBBLE;
    vec3  bubble = vec3(column.x + wobble, rise + index * UNDERWATER_BUBBLE_PERIOD, column.y);
    float along  = dot(bubble - origin, dir);

    if (along <= 0.0 || along > sceneDistance || bubble.y > u_oceanCamera.y - UNDERWATER_BUBBLE_RADIUS)
    return vec2(0.0);

    float radius  = UNDERWATER_BUBBLE_RADIUS;
    float edge    = max(radius * 0.1, along * UNDERWATER_BUBBLE_EDGE_SLOPE);
    float miss    = length(origin + dir * along - bubble);
    float inside  = 1.0 - smoothstep(radius - edge, radius, miss);
    float middle  = 1.0 - smoothstep(radius * 0.7 - edge, radius * 0.7, miss);
    float glint   = 1.0 - smoothstep(radius * 0.22 - edge, radius * 0.22,
        length(origin + dir * along - bubble - vec3(-0.35, 0.4, 0.0) * radius));

    return vec2(inside - middle * (1.0 - UNDERWATER_BUBBLE_MIDDLE), glint * inside);
}

// The bubbles the view ray passes, nearest columns of each band, the strongest of each kind kept
vec2 resolveBubbles(vec3 origin, vec3 dir, float sceneDistance, float time) {

    vec2 bubbles = vec2(0.0);

    for (int band = 0; band < UNDERWATER_BUBBLE_BANDS; band++) {

        float scale = float(1 << band);
        float cell  = UNDERWATER_BUBBLE_CELL * scale;
        vec2  probe = (origin + dir * (UNDERWATER_BUBBLE_NEAR * scale)).xz / cell;
        vec2  base  = floor(probe);

        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {

                vec2  index = base + vec2(float(x), float(z));
                float roll  = hash31(vec3(index, scale));

                if (roll > UNDERWATER_BUBBLE_SHARE)
                continue;

                vec2 offset = hash22(index + scale) * 0.5 + 0.5;
                bubbles = max(bubbles,
                    resolveBubble(origin, dir, (index + offset) * cell, sceneDistance, time, roll));
            }
    }

    return bubbles;
}

void main() {
    vec2  uv     = v_screenPos * 0.5 + 0.5;
    float time   = u_oceanSurface.w * UNDERWATER_SWAY_SPEED;
    vec2  sway   = vec2(sin(uv.y * UNDERWATER_SWAY_FREQUENCY + time), cos(uv.x * UNDERWATER_SWAY_FREQUENCY + time))
        * UNDERWATER_SWAY_STRENGTH;
    vec2  swayUV = clamp(uv + sway, vec2(0.0), vec2(1.0));

    float cameraDepth = u_oceanCamera.z;
    float depth       = sampleWaterSceneDepth(swayUV);

    vec3  rayDir        = normalize(mat3(u_inverseView) * reconstructWaterViewPos(uv, 0.5));
    float bubbleTime    = u_oceanSurface.w;
    vec3  color;
    float sceneDistance = UNDERWATER_OPEN_DISTANCE;

    if (depth >= 1.0)
    color = applyUnderwaterFog(vec3(0.0), UNDERWATER_OPEN_DISTANCE, cameraDepth);
    else {
        vec3 viewPos  = reconstructWaterViewPos(swayUV, depth);
        vec3 worldPos = viewToWorld(viewPos);

        color = sampleWaterSceneColor(swayUV);

        if (worldPos.y < u_oceanCamera.y)
        color *= 1.0 + sampleWaterCaustics(resolveOceanLattice(worldPos.xz), u_oceanCamera.y - worldPos.y);

        sceneDistance = length(viewPos);
        color         = applyUnderwaterFog(color, sceneDistance, cameraDepth);
    }

    vec2 bubbles = resolveBubbles(u_cameraPosition, rayDir, sceneDistance, bubbleTime);

    color = mix(color, UNDERWATER_BUBBLE_COLOR, bubbles.x * UNDERWATER_BUBBLE_OPACITY)
        + UNDERWATER_BUBBLE_COLOR * (bubbles.y * UNDERWATER_BUBBLE_GLINT);

    fragColor = vec4(color, 1.0);
}
