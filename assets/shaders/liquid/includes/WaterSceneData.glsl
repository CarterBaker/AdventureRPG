#ifndef WATER_SCENE_DATA_GLSL
#define WATER_SCENE_DATA_GLSL

#include "includes/CameraData.glsl"

// The lit opaque scene water draws over, and its depth. Water is drawn forward after deferred lighting, so it
// refracts this frame's lit scene and tests itself against the opaque depth by hand.
uniform sampler2D u_waterSceneColor;
uniform sampler2D u_waterSceneDepth;

vec2 resolveWaterScreenUV(vec2 fragCoord) {
    return fragCoord / vec2(textureSize(u_waterSceneDepth, 0));
}

float sampleWaterSceneDepth(vec2 uv) {
    return textureLod(u_waterSceneDepth, uv, 0.0).r;
}

vec3 sampleWaterSceneColor(vec2 uv) {
    return textureLod(u_waterSceneColor, uv, 0.0).rgb;
}

vec3 reconstructWaterViewPos(vec2 uv, float depth) {
    vec4 view = u_inverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return view.xyz / view.w;
}

vec3 viewToWorld(vec3 viewPos) {
    return (u_inverseView * vec4(viewPos, 1.0)).xyz;
}

#endif
