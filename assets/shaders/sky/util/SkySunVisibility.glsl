#ifndef SKY_SUN_VISIBILITY_GLSL
#define SKY_SUN_VISIBILITY_GLSL

#include "includes/CameraData.glsl"

/*
 * How much of the sun's disc on screen is left in view. Taps across the
 * disc read the scene's depth and the weather pass's clouds: a tap counts
 * nothing where the scene stands in front of the sky and less where clouds
 * cover it. Shared by every pass whose sun effects fade as the sun is
 * hidden, so they all agree on how much of it shows.
 */

uniform sampler2D u_sceneDepth;
uniform sampler2D u_cloudColor;

const float SKY_SUN_VISIBILITY_RADIUS  = 0.025;
const int   SKY_SUN_VISIBILITY_TAPS    = 8;
const float SKY_SUN_VISIBILITY_HIDDEN  = 0.2;
const float SKY_SUN_VISIBILITY_SHOWN   = 0.8;
const float SKY_SUN_VISIBILITY_TAU     = 6.28318530718;
const float SKY_SUN_VISIBILITY_EPSILON = 1e-5;

// Where the sun stands on screen, from 0 to 1 across each axis.
vec2 resolveSunScreen(vec3 sunDir) {
    vec4 clip = u_viewProjection * vec4(sunDir, 0.0);

    return clip.xy / max(clip.w, SKY_SUN_VISIBILITY_EPSILON) * 0.5 + 0.5;
}

// How much of the sun at one point of the screen reaches the eye.
float resolveSunTap(vec2 uv) {
    uv = clamp(uv, vec2(0.0), vec2(1.0));

    float sky = step(1.0, texture(u_sceneDepth, uv).r);

    return sky * (1.0 - clamp(texture(u_cloudColor, uv).a, 0.0, 1.0));
}

// How much of the sun's disc around sunScreen is left in view, eased so a
// mostly hidden sun counts as none and a mostly visible one as all of it.
float resolveSunVisibility(vec2 sunScreen) {
    float aspect  = float(textureSize(u_sceneDepth, 0).y) / float(textureSize(u_sceneDepth, 0).x);
    float visible = resolveSunTap(sunScreen);

    for (int i = 0; i < SKY_SUN_VISIBILITY_TAPS; i++) {
        float angle = SKY_SUN_VISIBILITY_TAU * float(i) / float(SKY_SUN_VISIBILITY_TAPS);
        vec2  tap   = vec2(cos(angle) * aspect, sin(angle)) * SKY_SUN_VISIBILITY_RADIUS;

        visible += resolveSunTap(sunScreen + tap);
    }

    return smoothstep(
        SKY_SUN_VISIBILITY_HIDDEN,
        SKY_SUN_VISIBILITY_SHOWN,
        visible / float(SKY_SUN_VISIBILITY_TAPS + 1));
}

#endif
