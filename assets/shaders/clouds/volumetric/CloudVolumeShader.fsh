// CloudVolumeShader.fsh — clouds/volumetric/CloudVolumeShader.fsh
#version 330 core

in vec3  vWorldPos;
in float vRandomSeed;
in float vFadeAlpha;
in float vIntensity;
in float vDensityMultiplier;
flat in vec3 vBoxCenter;
flat in vec3 vHalfExtent;
flat in vec2 vRot;
flat in float vDetailFactor;

out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/CloudSettingsData.glsl"
#include "clouds/util/VolumetricCloudUtility.glsl"

/*
* Forward-lit, alpha-blended raymarch of this instance's oriented box.
 * Composited as its own screen layer between the sky and the lit world
 * (see WeatherRenderSystem) — never touches the deferred G-buffer, so
 * every bit of a cloud's lighting is computed once, right here. Instances
 * are submitted farthest-first (see CloudRenderSystem), drawn with depth
 * write disabled, so ordinary alpha blending composites them correctly
 * with no dithered alpha-test approximation.
 */

uniform vec3  u_cloudColor;
uniform float u_cloudDensity;
uniform float u_cloudDensityNoiseScale;
uniform float u_cloudNoiseWarpStrength;
uniform float u_cloudCoverageBias;
uniform float u_cloudSilhouetteSoftness;

const float CLOUD_STEP_SIZE_NEAR = 3.0;
const float CLOUD_STEP_SIZE_FAR  = 9.0;
const float CLOUD_TIER_NEAR      = 60.0;
const float CLOUD_TIER_FAR       = 220.0;
const int   CLOUD_MIN_STEPS      = 20;
const int   CLOUD_MAX_STEPS      = 64;
const float CLOUD_EXTINCTION     = 0.18;
const float CLOUD_LIGHT_STEP_SIZE = 6.0;
const int   CLOUD_LIGHT_TAPS      = 4;
const float CLOUD_POWDER_STRENGTH = 1.4;
const float CLOUD_PHASE_FORWARD_G = 0.72;
const float CLOUD_PHASE_BACK_G    = -0.25;
const float CLOUD_PHASE_BLEND     = 0.65;
const float CLOUD_AMBIENT_BASE    = 0.35;
const float CLOUD_FOG_MIN_DISTANCE_RATIO = 0.40;
const float CLOUD_FOG_MAX_DISTANCE_RATIO = 0.95;
const float CLOUD_FOG_MAX_BLEND          = 0.55;

float interleavedGradientNoise(vec2 screenPos) {
    return fract(52.9829189 * fract(dot(screenPos, vec2(0.06711056, 0.00583715))));
}

void main() {
    if (vFadeAlpha <= 0.001)
    discard;

    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;
    vec3 rayDir = normalize(vWorldPos - cameraRenderPos);

    vec2 boxHit = intersectCloudBox(cameraRenderPos, rayDir, vBoxCenter, vRot, vHalfExtent);
    float marchStart = max(boxHit.x, 0.0);
    float marchLen = max(boxHit.y - marchStart, 0.0);

    if (marchLen <= 0.001)
    discard;

    float camDist = length(vWorldPos - cameraRenderPos);
    float thicknessStep = clamp(vHalfExtent.y * 2.0 / 6.0, 1.2, 7.0);
    float distanceStep = mix(CLOUD_STEP_SIZE_NEAR, CLOUD_STEP_SIZE_FAR,
        smoothstep(CLOUD_TIER_NEAR, CLOUD_TIER_FAR, camDist));
    float targetStepSize = max(thicknessStep, distanceStep);

    int steps = clamp(int(marchLen / targetStepSize), CLOUD_MIN_STEPS, CLOUD_MAX_STEPS);
    float stepSize = marchLen / float(steps);
    float dither = interleavedGradientNoise(gl_FragCoord.xy);

    float boxHeight = max(vHalfExtent.y * 2.0, 0.0001);
    float baseY = vBoxCenter.y - vHalfExtent.y;

    float sunWeight = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));
    vec3  lightColor = mix(u_moonColor, u_sunColor, sunWeight);
    float lightPower = mix(u_moonIntensity, u_sunIntensity, sunWeight);
    float cosAngle = dot(rayDir, lightDir);
    float phase = cloudPhase(cosAngle, CLOUD_PHASE_FORWARD_G, CLOUD_PHASE_BACK_G, CLOUD_PHASE_BLEND);
    float backlit = clamp(cosAngle * 0.5 + 0.5, 0.0, 1.0);

    vec4 accum = vec4(0.0);

    for (int i = 0; i < CLOUD_MAX_STEPS; i++) {
        if (i >= steps || accum.a > 0.985)
        break;

        float t = marchStart + (float(i) + dither) * stepSize;
        vec3  p = cameraRenderPos + rayDir * t;

        float heightT = clamp((p.y - baseY) / boxHeight, 0.0, 1.0);

        float rawDensity = sampleCloudDensity(
            p, vBoxCenter, vRot, vHalfExtent, heightT,
            u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
            u_cloudCoverageBias, u_cloudSilhouetteSoftness,
            vDetailFactor, vRandomSeed, u_time);
        float density = rawDensity * u_cloudDensity * vDensityMultiplier * vIntensity;

        if (density > 0.01) {
            float lightTransmittance = sampleCloudLightTransmittance(
                p, lightDir, vBoxCenter, vRot, vHalfExtent,
                u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
                u_cloudCoverageBias, u_cloudSilhouetteSoftness,
                vDetailFactor, vRandomSeed, u_time,
                CLOUD_LIGHT_STEP_SIZE, CLOUD_LIGHT_TAPS, CLOUD_EXTINCTION);

            float powder = 1.0 - exp(-density * CLOUD_POWDER_STRENGTH);
            float inScatter = mix(1.0, powder, backlit);

            vec3 direct = lightColor * lightPower * lightTransmittance * phase * inScatter;

            vec3 skyTint = mix(u_skyHorizonColor, u_skyZenithColor, heightT);
            vec3 ambient = skyTint * mix(CLOUD_AMBIENT_BASE, 1.0, heightT);

            vec3 shaded = u_cloudColor * (ambient + direct);

            float stepTransmittance = exp(-density * CLOUD_EXTINCTION * stepSize);
            float stepAlpha = 1.0 - stepTransmittance;
            float contribution = (1.0 - accum.a) * stepAlpha;

            accum.rgb += contribution * shaded;
            accum.a   += contribution;
        }
    }

    float finalAlpha = clamp(accum.a * vFadeAlpha, 0.0, 1.0);

    if (finalAlpha <= 0.02)
    discard;

    vec3 straightColor = accum.rgb / max(accum.a, 0.0001);

    // Aerial-perspective fade toward the horizon color as a cloud
    // approaches the edge of the simulated weather radius.
    float fogT = smoothstep(
        u_cloudHorizonDistance * CLOUD_FOG_MIN_DISTANCE_RATIO,
        u_cloudHorizonDistance * CLOUD_FOG_MAX_DISTANCE_RATIO,
        camDist) * CLOUD_FOG_MAX_BLEND;
    straightColor = mix(straightColor, u_skyHorizonColor + 0.04, fogT);

    fragColor = vec4(straightColor, finalAlpha);
}