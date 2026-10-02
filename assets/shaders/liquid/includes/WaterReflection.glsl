#ifndef WATER_REFLECTION_GLSL
#define WATER_REFLECTION_GLSL

#include "includes/CameraData.glsl"
#include "includes/SunLightData.glsl"
#include "sky/util/SkyColor.glsl"
#include "liquid/includes/WaterSceneData.glsl"

/*
 * Cheap reflections without a second scene render. The sky along a reflected direction is the analytic sky,
 * replaced wherever that direction is on screen by the sky and clouds the window actually drew there, so
 * clouds, sun and sunset all show in the water. Scene geometry is found by a short screen-space march along
 * the reflected ray with a bisection on the first hit, faded out toward the screen edge and with distance;
 * where it misses, the sky shows.
 */

// The sky and cloud targets, read where a reflected direction lands on screen, and the cloud target's
// distances, which lay the clouds standing in front of the water over it (CloudComposite).
uniform sampler2D u_waterSkyColor;
uniform sampler2D u_waterCloudColor;
uniform sampler2D u_waterCloudDistance;

const float WATER_REFLECTION_HORIZON_LIFT = 0.03;
const float WATER_REFLECTION_EDGE_FADE    = 0.1;
const int   WATER_SSR_STEPS               = 14;
const int   WATER_SSR_REFINE_STEPS        = 4;
const float WATER_SSR_FIRST_STEP_BLOCKS   = 0.6;
const float WATER_SSR_STEP_GROWTH         = 1.38;
const float WATER_SSR_THICKNESS_BLOCKS    = 1.5;

float resolveScreenEdgeFade(vec2 uv) {
    vec2 edge = smoothstep(vec2(0.0), vec2(WATER_REFLECTION_EDGE_FADE), uv)
        * (1.0 - smoothstep(vec2(1.0 - WATER_REFLECTION_EDGE_FADE), vec2(1.0), uv));
    return edge.x * edge.y;
}

vec3 liftAboveHorizon(vec3 dir) {
    return normalize(vec3(dir.x, max(dir.y, WATER_REFLECTION_HORIZON_LIFT), dir.z));
}

vec3 sampleWaterSky(vec3 dir) {
    vec3 skyDir = liftAboveHorizon(dir);
    vec3 sky    = resolveSkyColor(skyDir, normalize(u_sunDirection), 0.0);
    vec4 clip   = u_projection * vec4(mat3(u_view) * skyDir, 0.0);

    if (clip.w <= 0.0)
    return sky;

    vec2  uv   = clip.xy / clip.w * 0.5 + 0.5;
    float fade = resolveScreenEdgeFade(uv);

    if (fade <= 0.0)
    return sky;

    vec3 drawnSky = textureLod(u_waterSkyColor, uv, 0.0).rgb;
    vec4 cloud    = textureLod(u_waterCloudColor, uv, 0.0);

    return mix(sky, drawnSky * (1.0 - cloud.a) + cloud.rgb, fade);
}

bool projectToScreen(vec3 viewPos, out vec2 uv) {
    vec4 clip = u_projection * vec4(viewPos, 1.0);

    if (clip.w <= 0.0)
    return false;

    uv = clip.xy / clip.w * 0.5 + 0.5;

    return all(greaterThanEqual(uv, vec2(0.0))) && all(lessThanEqual(uv, vec2(1.0)));
}

// How far a view-space point lies behind the opaque scene at its pixel, negative in front of it.
float resolveBehindScene(vec3 viewPos, vec2 uv) {
    float depth = sampleWaterSceneDepth(uv);

    if (depth >= 1.0)
    return -1.0;

    return reconstructWaterViewPos(uv, depth).z - viewPos.z;
}

// rgb = reflected scene color, a = how much of it to use.
vec4 traceWaterReflection(vec3 viewPos, vec3 viewDir) {
    vec3  previous = viewPos;
    vec3  ray      = viewPos;
    float stepSize = WATER_SSR_FIRST_STEP_BLOCKS;
    vec2  uv;

    for (int i = 0; i < WATER_SSR_STEPS; i++) {

        previous  = ray;
        ray      += viewDir * stepSize;
        stepSize *= WATER_SSR_STEP_GROWTH;

        if (!projectToScreen(ray, uv))
        return vec4(0.0);

        float behind = resolveBehindScene(ray, uv);

        if (behind <= 0.0 || behind > max(WATER_SSR_THICKNESS_BLOCKS, stepSize))
        continue;

        vec3 front = previous;
        vec3 back  = ray;

        for (int j = 0; j < WATER_SSR_REFINE_STEPS; j++) {
            vec3 middle = (front + back) * 0.5;
            vec2 middleUV;

            if (!projectToScreen(middle, middleUV))
            break;

            if (resolveBehindScene(middle, middleUV) > 0.0)
            back = middle;
            else
            front = middle;
        }

        if (!projectToScreen(back, uv))
        return vec4(0.0);

        float reach = 1.0 - float(i) / float(WATER_SSR_STEPS);

        return vec4(sampleWaterSceneColor(uv), resolveScreenEdgeFade(uv) * reach);
    }

    return vec4(0.0);
}

#endif
