#ifndef PARTICLE_SCENE_GLSL
#define PARTICLE_SCENE_GLSL

#include "includes/CameraData.glsl"
#include "includes/SkyColorData.glsl"

/*
 * What every particle drawn into the precipitation target shares: the scene's and the water's depth, against which
 * a particle hides behind whatever stands in front of it and fades as it nears a surface, as soft particles do, so
 * none cuts a hard line where it meets the ground or the water; the light of the sky, which tints it as it tints the
 * rain; and the size of one pixel at a distance, which keeps a thin particle from thinning to nothing. A particle
 * writes its color premultiplied, as the target composites.
 */

uniform sampler2D u_sceneDepth;
uniform sampler2D u_waterDepth;

const float PARTICLE_ZENITH_SHARE = 0.4;
const float PARTICLE_LIGHT_GAIN   = 1.6;
const float PARTICLE_LIGHT_MIN    = 0.2;

// Blocks one pixel spans at this distance from the camera
float resolveParticlePixel(float distance) {
    return distance * 2.0 / (u_projection[1][1] * u_viewport.y);
}

// The window position of a clip position, 0 to 1 across the target
vec2 resolveParticleScreen(vec4 clip) {
    return clip.xy / clip.w * 0.5 + 0.5;
}

// How much of a particle shows at this distance along its pixel: all of it more than its soft reach before the
// nearest opaque or water surface there, fading as it nears that surface, none behind it
float resolveParticleVisibility(vec2 screen, float distance, float soft) {

    float depth = min(texture(u_sceneDepth, screen).r, texture(u_waterDepth, screen).r);

    if (depth >= 1.0)
    return 1.0;

    vec4 view = u_inverseProjection * vec4(screen * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);

    return clamp((length(view.xyz / view.w) - distance) / soft, 0.0, 1.0);
}

// The sky's light on a particle, as the rain is lit
vec3 resolveParticleLight() {
    vec3 sky = mix(u_skyHorizonColor, u_skyZenithColor, PARTICLE_ZENITH_SHARE);
    return clamp(sky * PARTICLE_LIGHT_GAIN, vec3(PARTICLE_LIGHT_MIN), vec3(1.0));
}

#endif
