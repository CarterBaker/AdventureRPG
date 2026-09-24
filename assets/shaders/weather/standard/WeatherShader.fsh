#version 330 core

in vec3 v_dir;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudVisual.glsl"
#include "weather/includes/CloudMarch.glsl"

/*
 * Fullscreen cloud pass for the sky, rendered at the reduced resolution its
 * target declares and sampled back up when composited. Every live cloud
 * layer is integrated along the view ray out to the edge of the weather map
 * (CloudMarch) and written premultiplied, so the upscale blends cloud edges
 * against transparency rather than against black. The target declares a
 * resolving blit, which averages away the march's per-pixel jitter while
 * upscaling and restores straight alpha. Terrain composites
 * over this pass; clouds standing in front of terrain are fogged in by the
 * lighting pass.
 */

const float CLOUD_PASS_ALPHA_DISCARD = 0.003;

void main() {
    if (u_weatherLayerCount == 0)
    discard;

    vec3  color         = vec3(0.0);
    float transmittance = 1.0;

    integrateCloudSky(normalize(v_dir), interleavedGradientNoise(gl_FragCoord.xy), color, transmittance);

    float coverage = 1.0 - transmittance;

    if (coverage <= CLOUD_PASS_ALPHA_DISCARD)
    discard;

    fragColor = vec4(color, coverage);
}
