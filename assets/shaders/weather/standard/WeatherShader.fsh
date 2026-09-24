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
 * Fullscreen cloud pass for the sky. Every live cloud layer is integrated
 * along the view ray out to the edge of the weather map (CloudMarch) and
 * written as straight color with coverage alpha. Terrain composites over
 * this pass; clouds standing in front of terrain are fogged in by the
 * lighting pass through the same integration.
 */

const float CLOUD_PASS_UNBOUNDED_DISTANCE = 1.0e30;
const float CLOUD_PASS_ALPHA_DISCARD      = 0.003;

void main() {
    if (u_weatherLayerCount == 0)
    discard;

    vec3  color         = vec3(0.0);
    float transmittance = 1.0;

    integrateCloudLayers(normalize(v_dir), CLOUD_PASS_UNBOUNDED_DISTANCE, color, transmittance);

    float coverage = 1.0 - transmittance;

    if (coverage <= CLOUD_PASS_ALPHA_DISCARD)
    discard;

    fragColor = vec4(color / max(coverage, CLOUD_MARCH_EPSILON), coverage);
}
