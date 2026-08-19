#ifndef CLOUD_VISUAL_GLSL
#define CLOUD_VISUAL_GLSL

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"

/*
* Shared cloud "look" for the weather fullscreen pass — horizontal
 * silhouette, vertical density profile, and lit shading for a single
 * weather-map entry, all driven entirely by that entry's own UBO values
 * (shape, noise, color/material, and per-slot variance) so every
 * archetype — wispy Cirrus, dense towering Cumulonimbus, flat Stratus,
 * puffy Cumulus — is drawn by the exact same code and differs only
 * because its authored numbers differ. The silhouette comes from one
 * domain-warped multi-octave fbm field: fbm gives the soft, rounded,
 * billowy mass real clouds have, and the domain warp keeps that mass
 * from reading as an axis-aligned noise grid. Everything here is
 * evaluated in cheap 2D — the raymarch in WeatherShader.fsh supplies the
 * actual vertical thickness by resampling the vertical profile at each
 * step through a slab this file has no knowledge of.
 */

const float CLOUD_VISUAL_EPSILON = 0.001;

// Silhouette
const float CLOUD_VISUAL_OUTER_FADE_START        = 0.85;
const float CLOUD_VISUAL_OUTER_FADE_END          = 1.35;
const float CLOUD_VISUAL_BOUNDARY_WARP_STRENGTH  = 0.22;
const float CLOUD_VISUAL_BOUNDARY_WARP_FREQUENCY = 2.5;
const float CLOUD_VISUAL_ANGLE_WOBBLE            = 1.2;

// Shape field
const float CLOUD_VISUAL_WARP_FREQUENCY       = 0.9;
const float CLOUD_VISUAL_WARP_TIME_SCALE      = 0.015;
const float CLOUD_VISUAL_DETAIL_FREQUENCY     = 2.6;
const float CLOUD_VISUAL_DETAIL_STRENGTH      = 0.3;
const float CLOUD_VISUAL_SCROLL_SCALE         = 0.4;
const float CLOUD_VISUAL_GRADIENT_PROBE_RATIO = 0.15;
const float CLOUD_VISUAL_GRADIENT_SCALE       = 2.0;

// Vertical wisping
const float CLOUD_VISUAL_WISP_FREQUENCY = 0.02;
const float CLOUD_VISUAL_WISP_STRENGTH  = 0.35;

// Shading
const float CLOUD_VISUAL_SILVER_LINING_POWER    = 10.0;
const float CLOUD_VISUAL_SILVER_LINING_STRENGTH = 0.9;
const float CLOUD_VISUAL_AMBIENT_SHADOW         = 0.15;
const float CLOUD_VISUAL_AMBIENT_LIT            = 0.6;
const float CLOUD_VISUAL_SKY_TINT_STRENGTH      = 0.45;
const float CLOUD_VISUAL_STORM_DARKEN_MIN       = 0.45;

/*
* Horizontal cloud silhouette for one weather-map entry, evaluated once
 * per pixel at the ray's representative crossing point through its slab.
 * Returns 0 outside the pattern's own wobbled footprint boundary or
 * wherever the shape field falls under its coverage threshold.
 * outGradient is a cheap directional "lean" re-used by the caller to fake
 * a lit normal. outShadingBias is a coarse, smoothly varying 0..1 field
 * the vertical profile re-uses so different clumps within the same
 * pattern settle at slightly different heights instead of one flat sheet.
 */
float resolveCloudCoverage(
    vec4 bounds, vec4 shape, vec4 noiseParams, vec4 colorScale,
    vec4 variance0, vec4 variance1,
    float intensity, vec3 worldPos,
    vec2 driftDirNorm, float driftAngle, float driftSpeed,
    out vec2 outGradient, out float outShadingBias) {
    outGradient    = vec2(0.0);
    outShadingBias = 0.5;

    vec2 boxCenter     = (bounds.xy + bounds.zw) * 0.5;
    vec2 boxHalfExtent = max((bounds.zw - bounds.xy) * 0.5, vec2(1.0));
    vec2 fromCenter    = worldPos.xz - boxCenter;

    float patternSeed    = variance1.z;
    float cloudSlotIndex = variance1.y;

    float orientationHash = hash31(vec3(
            patternSeed * 12.9898,
            cloudSlotIndex * 78.233 + patternSeed,
            patternSeed - cloudSlotIndex * 0.577));

    float puffAngle = driftAngle + (orientationHash - 0.5) * CLOUD_VISUAL_ANGLE_WOBBLE;
    float cosA = cos(puffAngle);
    float sinA = sin(puffAngle);
    vec2  rotated = vec2(
        fromCenter.x * cosA + fromCenter.y * sinA,
        fromCenter.y * cosA - fromCenter.x * sinA);

    float elongation  = clamp(mix(variance0.w, variance1.x, fract(orientationHash * 3.17)), 1.0, 6.0);
    float spreadRatio = clamp(variance0.x, 0.1, 2.0);
    vec2  anisotropicExtent = boxHalfExtent * spreadRatio * vec2(1.0, 1.0 / elongation);
    vec2  spreadNorm  = rotated / max(anisotropicExtent, vec2(1.0));
    float rawRadialDist = length(spreadNorm);

    if (rawRadialDist > CLOUD_VISUAL_OUTER_FADE_END * (1.0 + CLOUD_VISUAL_BOUNDARY_WARP_STRENGTH))
    return 0.0;

    float boundaryAngle  = atan(spreadNorm.y, spreadNorm.x);
    float boundaryWobble = 1.0 + CLOUD_VISUAL_BOUNDARY_WARP_STRENGTH * gradientNoise2D(
        vec2(cos(boundaryAngle), sin(boundaryAngle)) * CLOUD_VISUAL_BOUNDARY_WARP_FREQUENCY
        + vec2(patternSeed * 5.1, cloudSlotIndex * 13.7));
    float radialDist = rawRadialDist / max(boundaryWobble, 0.35);

    if (radialDist > CLOUD_VISUAL_OUTER_FADE_END)
    return 0.0;

    float outerFade = 1.0 - smoothstep(CLOUD_VISUAL_OUTER_FADE_START, CLOUD_VISUAL_OUTER_FADE_END, radialDist);

    if (outerFade <= CLOUD_VISUAL_EPSILON)
    return 0.0;

    float sizeVariance      = clamp(mix(variance0.y, variance0.z, fract(orientationHash * 5.63)), 0.3, 3.0);
    float featureSizeBlocks = max(colorScale.w, 4.0) * sizeVariance;

    vec2 driftScroll = driftDirNorm * driftSpeed * u_time * CLOUD_VISUAL_SCROLL_SCALE * shape.w;
    vec2 flowPos     = rotated + driftScroll;

    vec2 morphTime = vec2(u_time * CLOUD_VISUAL_WARP_TIME_SCALE);
    vec2 warpFreq  = vec2(CLOUD_VISUAL_WARP_FREQUENCY / featureSizeBlocks);
    vec2 warp = vec2(
        gradientNoise2D(flowPos * warpFreq + vec2(patternSeed * 7.7, cloudSlotIndex * 3.3) + morphTime),
        gradientNoise2D(flowPos * warpFreq + vec2(cloudSlotIndex * 9.1, patternSeed * 4.4) - morphTime));
    vec2 warpedPos = flowPos + warp * featureSizeBlocks * clamp(noiseParams.y, 0.0, 1.5);

    vec2 baseScale      = vec2(1.0 / featureSizeBlocks);
    vec2 shapeSamplePos = warpedPos * baseScale;

    float shapeNoise  = fbmGradient2D(shapeSamplePos, 4, 2.1, 0.5);
    float coarseNoise = fbmGradient2D(shapeSamplePos * 0.35 + vec2(19.3, -7.7), 2, 2.0, 0.5);
    outShadingBias = coarseNoise;

    float coverageBias = noiseParams.z;
    float threshold     = clamp(1.0 - (intensity * 0.6 + coverageBias * 0.4), 0.03, 0.95);
    float softness      = clamp(noiseParams.w, 0.04, 0.6);

    float coverage = smoothstep(threshold - softness, threshold + softness, shapeNoise);

    vec2  detailPos = warpedPos * (CLOUD_VISUAL_DETAIL_FREQUENCY / featureSizeBlocks) * max(noiseParams.x, 0.1);
    float detail    = fbmGradient2D(detailPos, 2, 2.2, 0.5) - 0.5;
    coverage = clamp(coverage + detail * CLOUD_VISUAL_DETAIL_STRENGTH * (1.0 - coverage), 0.0, 1.0);

    coverage *= outerFade;

    if (coverage <= CLOUD_VISUAL_EPSILON)
    return 0.0;

    float probeEps = featureSizeBlocks * CLOUD_VISUAL_GRADIENT_PROBE_RATIO;
    float gx = gradientNoise2D((warpedPos + vec2(probeEps, 0.0)) * baseScale)
    - gradientNoise2D((warpedPos - vec2(probeEps, 0.0)) * baseScale);
    float gz = gradientNoise2D((warpedPos + vec2(0.0, probeEps)) * baseScale)
    - gradientNoise2D((warpedPos - vec2(0.0, probeEps)) * baseScale);
    outGradient = vec2(gx, gz) * CLOUD_VISUAL_GRADIENT_SCALE;

    return coverage;
}

/*
* One cloud slab's vertical density at an arbitrary world height, sampled
 * once per raymarch step so a ray crossing at any angle integrates real
 * thickness instead of a single flat number. The profile is asymmetric —
 * sharper below its own center, softer above — for the flat-base,
 * round-top read a real convective cloud has; fullness controls how
 * pronounced that asymmetry is. shadingBias (see resolveCloudCoverage)
 * nudges the center within a modest band so different clumps in the same
 * pattern don't all peak at the same height, and a cheap per-step 2D wisp
 * breaks the density up along the ray so a thick slab never reads as a
 * uniform grey wall.
 */
float resolveCloudVerticalDensity(
    vec3 stepWorldPos, float slabBottomY, float slabTopY,
    float fullness, float shadingBias, float patternSeed,
    out float verticalNorm, out float verticalSign) {
    float verticalT = clamp(
        (stepWorldPos.y - slabBottomY) / max(slabTopY - slabBottomY, 0.0001), 0.0, 1.0);

    float centerT   = mix(0.40, 0.60, shadingBias);
    float belowSpan = mix(0.40, 0.80, fullness);
    float aboveSpan = mix(1.30, 0.90, fullness);

    float offset = verticalT - centerT;
    verticalSign = offset < 0.0 ? -1.0 : 1.0;
    verticalNorm = offset < 0.0
    ? -offset / max(belowSpan, 0.001)
    : offset / max(aboveSpan, 0.001);

    float profile = 1.0 - smoothstep(0.55, 1.0, verticalNorm);

    if (profile <= CLOUD_VISUAL_EPSILON)
    return 0.0;

    float wispNoise = gradientNoise2D(
        stepWorldPos.xz * CLOUD_VISUAL_WISP_FREQUENCY + vec2(patternSeed * 13.1, stepWorldPos.y * 0.05));
    float wisp = mix(1.0 - CLOUD_VISUAL_WISP_STRENGTH, 1.0, wispNoise * 0.5 + 0.5);

    return profile * wisp;
}

/*
* Shades one raymarch step's resolved sample — a soft top-lit/bottom-dark
 * gradient from the fake normal, plus a silver lining that only brightens
 * the cloud's own thin edges when looking roughly toward the sun through
 * them (forward scattering). selfShadow darkens both ambient and direct
 * terms the deeper this step sits within its own entry's already-
 * traversed volume, approximating multiple-scattering falloff without an
 * actual light-ray march.
 */
vec3 shadeCloudStep(
    vec3 rayDir, vec3 fakeNormal, float puffHeight, float selfShadow, float thicknessNorm,
    vec4 colorScale, vec4 materialParams) {
    vec3 sunDir  = normalize(u_sunDirection);
    vec3 moonDir = normalize(u_moonDirection);

    float saturation = materialParams.x;

    float sunLight  = clamp(dot(fakeNormal, sunDir), 0.0, 1.0) * selfShadow;
    float moonLight = clamp(dot(fakeNormal, moonDir), 0.0, 1.0) * min(u_moonIntensity, 0.18) * selfShadow;

    vec3  moonTint    = vec3(0.58, 0.74, 1.00);
    vec3  directLight = u_sunColor * u_sunIntensity * sunLight + u_moonColor * moonTint * moonLight;

    float luminance    = dot(colorScale.rgb, vec3(0.299, 0.587, 0.114));
    vec3  tintedAlbedo = mix(vec3(luminance), colorScale.rgb, saturation);
    tintedAlbedo = mix(tintedAlbedo, tintedAlbedo * u_skyCloudColor, CLOUD_VISUAL_SKY_TINT_STRENGTH);

    float ambientFloor = mix(CLOUD_VISUAL_AMBIENT_SHADOW, CLOUD_VISUAL_AMBIENT_SHADOW * 0.5, thicknessNorm);
    float ambient       = mix(ambientFloor, CLOUD_VISUAL_AMBIENT_LIT, puffHeight) * selfShadow;

    vec3 shaded = tintedAlbedo * (ambient + directLight);
    shaded *= mix(CLOUD_VISUAL_STORM_DARKEN_MIN, 1.0, saturation);

    float sunAlignment  = clamp(dot(rayDir, sunDir), 0.0, 1.0);
    float edgeThinness  = 1.0 - puffHeight;
    float silverLining  = pow(sunAlignment, CLOUD_VISUAL_SILVER_LINING_POWER) * edgeThinness;
    shaded += u_sunColor * u_sunIntensity * silverLining * CLOUD_VISUAL_SILVER_LINING_STRENGTH * selfShadow;

    return shaded;
}

#endif