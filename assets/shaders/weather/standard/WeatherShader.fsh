#version 330 core

in vec3 v_dir;
layout(location = 0) out vec4 cloudDistance;
layout(location = 1) out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudVisual.glsl"
#include "weather/includes/CloudRain.glsl"
#include "weather/includes/CloudMarch.glsl"
#include "weather/includes/CloudComposite.glsl"

/*
 * Fullscreen cloud pass for the sky, rendered at the reduced resolution its
 * target declares and sampled back up when composited. The nearest solid
 * cloud surface along the view ray, out to the edge of the weather map, is
 * found and painted (CloudMarch), and written premultiplied, so the upscale
 * blends cloud edges against transparency rather than against black, along
 * with its distance, so the lighting and water passes lay exactly the clouds
 * standing in front of their surfaces over them (CloudComposite). The rain
 * shafts hanging beneath rain-bearing cloud in front of that surface, or of
 * the open sky, are written beside the distance for the precipitation pass to
 * lay over whatever lies behind them. A camera standing inside a cloud
 * sees only the cloud's fog: the fog's color fills the sky, and its density
 * is written for the other passes to fog their surfaces by distance. The
 * target declares a resolving blit, which averages away the march's
 * per-pixel jitter while upscaling, softening the cloud outlines into clean
 * painted edges, and restores straight alpha.
 */

void main() {
    if (u_weatherLayerCount == 0)
    discard;

    vec3  rayDir = normalize(v_dir);
    vec3  fogColor;
    float fogDensity = resolveCloudFog(fogColor);

    if (fogDensity > 0.0) {
        fragColor     = vec4(fogColor, 1.0);
        cloudDistance = vec4(CLOUD_COMPOSITE_NO_SURFACE, fogDensity, 0.0, 0.0);
        return;
    }

    int      layer;
    CloudHit hit;
    float    jitter = interleavedGradientNoise(gl_FragCoord.xy);
    bool     found  = findNearestCloudSurface(rayDir, jitter, layer, hit);
    vec2     shaft  = resolveRainShaft(rayDir, hit.t, jitter);

    if (!found && shaft.y <= 0.0)
    discard;

    fragColor     = found ? vec4(shadeCloudHit(rayDir, layer, hit), 1.0) : vec4(0.0);
    cloudDistance = vec4(found ? hit.t : 0.0, 0.0, shaft);
}
