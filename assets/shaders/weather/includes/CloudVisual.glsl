#ifndef CLOUD_VISUAL_GLSL
#define CLOUD_VISUAL_GLSL

#include "includes/NoiseUtility.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"

/*
 * Shared cloud look for the weather fullscreen pass: horizontal silhouette,
 * vertical density profile, step normal and lit shading for a single
 * weather-map entry, driven entirely by that entry's own UBO values. The
 * silhouette is a billow field — folded gradient noise, which produces
 * rounded lobes instead of fbm mush — thresholded into firm stylized edges
 * and faded by angular size so sub-pixel detail is never sampled. Every
 * sample is taken in the entry's own pattern-local space as a pure function
 * of position and seed, so a cloud is rigid in the world and identical for
 * every player. Lighting is wrapped rather than clamped and its ambient term
 * comes from the sky gradient, which is what gives the cotton read.
 */

const float CLOUD_VISUAL_EPSILON = 0.001;

const float CLOUD_VISUAL_OUTER_FADE_START        = 0.80;
const float CLOUD_VISUAL_OUTER_FADE_END          = 1.30;
const float CLOUD_VISUAL_BOUNDARY_WARP_STRENGTH  = 0.22;
const float CLOUD_VISUAL_BOUNDARY_WARP_FREQUENCY = 2.5;
const float CLOUD_VISUAL_ANGLE_WOBBLE            = 1.2;

const float CLOUD_VISUAL_CLUMP_FREQUENCY               = 1.8;
const float CLOUD_VISUAL_CLUMP_THRESHOLD_SPARSE        = 0.80;
const float CLOUD_VISUAL_CLUMP_THRESHOLD_DENSE         = 0.14;
const float CLOUD_VISUAL_CLUMP_COVERAGE_BIAS_INFLUENCE = 0.25;
const float CLOUD_VISUAL_CLUMP_SOFTNESS                = 0.18;

const float CLOUD_VISUAL_WARP_FREQUENCY       = 0.7;
const float CLOUD_VISUAL_DETAIL_FREQUENCY     = 2.4;
const float CLOUD_VISUAL_DETAIL_STRENGTH      = 0.22;
const float CLOUD_VISUAL_GRADIENT_PROBE_RATIO = 0.35;
const float CLOUD_VISUAL_GRADIENT_SCALE       = 1.0;
const float CLOUD_VISUAL_EDGE_FIRMNESS        = 0.55;

const float CLOUD_VISUAL_LOD_MIN_ANGULAR  = 0.0020;
const float CLOUD_VISUAL_LOD_FULL_ANGULAR = 0.0120;

const float CLOUD_VISUAL_CENTER_T_MIN   = 0.38;
const float CLOUD_VISUAL_CENTER_T_MAX   = 0.50;
const float CLOUD_VISUAL_BELOW_SPAN_MIN = 0.50;
const float CLOUD_VISUAL_BELOW_SPAN_MAX = 0.30;
const float CLOUD_VISUAL_ABOVE_SPAN_MIN = 0.52;
const float CLOUD_VISUAL_ABOVE_SPAN_MAX = 0.62;
const float CLOUD_VISUAL_BASE_EDGE_SOFT = 0.85;
const float CLOUD_VISUAL_BASE_EDGE_HARD = 0.98;
const float CLOUD_VISUAL_TOP_EDGE_SHEET = 0.55;
const float CLOUD_VISUAL_TOP_EDGE_PUFFY = 0.30;
const float CLOUD_VISUAL_SLAB_OVERSHOOT = 0.25;
const float CLOUD_VISUAL_WISP_STRENGTH  = 0.22;

const float CLOUD_VISUAL_LIGHT_WRAP          = 0.5;
const float CLOUD_VISUAL_AMBIENT_BASE        = 0.42;
const float CLOUD_VISUAL_AMBIENT_TOP         = 0.95;
const float CLOUD_VISUAL_SHADOW_FLOOR        = 0.35;
const float CLOUD_VISUAL_SKY_TINT_STRENGTH   = 0.35;
const float CLOUD_VISUAL_STORM_DARKEN_MIN    = 0.45;
const float CLOUD_VISUAL_RIM_POWER           = 6.0;
const float CLOUD_VISUAL_RIM_STRENGTH        = 0.55;
const float CLOUD_VISUAL_LATERAL_LEAN_SHEET  = 0.35;
const float CLOUD_VISUAL_NORMAL_UP_FLOOR     = 0.15;

// Folded gradient noise. Rounded lobes rather than the symmetric mid-range
// mush a plain fbm gives, which is what reads as puffy once thresholded.
float billowGradient2D(vec2 p, int octaves, float lacunarity, float gain) {
    float sum  = 0.0;
    float norm = 0.0;
    float amp  = 0.5;
    vec2  pos  = p;

    for (int i = 0; i < octaves; i++) {
        sum  += amp * (1.0 - abs(gradientNoise2D(pos)));
        norm += amp;
        pos   = pos * lacunarity + vec2(13.7, -21.3);
        amp  *= gain;
    }

    return clamp(sum / max(norm, 0.0001), 0.0, 1.0);
}

// Fraction of this entry's detail that is still larger than a pixel at the
// given sample distance. Everything finer is folded out rather than sampled.
float resolveCloudLodFade(float featureSizeBlocks, float sampleDistance) {
    float angular = featureSizeBlocks / max(sampleDistance, 1.0);
    return clamp(
        (angular - CLOUD_VISUAL_LOD_MIN_ANGULAR)
        / (CLOUD_VISUAL_LOD_FULL_ANGULAR - CLOUD_VISUAL_LOD_MIN_ANGULAR), 0.0, 1.0);
}

float resolveCloudCoverage(
    vec4 bounds, vec4 noiseParams, vec4 colorScale,
    vec4 variance0, vec4 variance1,
    float intensity, float fullness, vec3 worldPos, vec2 orientationDir,
    float sampleDistance,
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

    float puffAngle = atan(orientationDir.y, orientationDir.x)
    + (orientationHash - 0.5) * CLOUD_VISUAL_ANGLE_WOBBLE;
    float cosA = cos(puffAngle);
    float sinA = sin(puffAngle);
    vec2  rotated = vec2(
        fromCenter.x * cosA + fromCenter.y * sinA,
        fromCenter.y * cosA - fromCenter.x * sinA);

    float elongation  = clamp(mix(variance0.w, variance1.x, fract(orientationHash * 3.17)), 1.0, 6.0);
    float spreadRatio = clamp(variance0.x, 0.1, 2.0);
    vec2  spreadNorm  = rotated / max(boxHalfExtent * spreadRatio * vec2(1.0, 1.0 / elongation), vec2(1.0));
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

    float coverageBias = noiseParams.z;

    vec2  clumpSamplePos = spreadNorm * CLOUD_VISUAL_CLUMP_FREQUENCY
    + vec2(patternSeed * 41.3, cloudSlotIndex * 23.7 - patternSeed * 7.1);
    float clumpNoise = fbmGradient2D(clumpSamplePos, 3, 2.0, 0.55);

    float clumpThreshold = clamp(
        mix(CLOUD_VISUAL_CLUMP_THRESHOLD_SPARSE, CLOUD_VISUAL_CLUMP_THRESHOLD_DENSE, intensity)
        - coverageBias * CLOUD_VISUAL_CLUMP_COVERAGE_BIAS_INFLUENCE,
        0.05, 0.9);
    float clumpMask = smoothstep(
        clumpThreshold - CLOUD_VISUAL_CLUMP_SOFTNESS, clumpThreshold + CLOUD_VISUAL_CLUMP_SOFTNESS, clumpNoise);

    if (clumpMask <= CLOUD_VISUAL_EPSILON)
    return 0.0;

    float sizeVariance      = clamp(mix(variance0.y, variance0.z, fract(orientationHash * 5.63)), 0.3, 3.0);
    float featureSizeBlocks = max(colorScale.w, 4.0) * sizeVariance;
    float lodFade           = resolveCloudLodFade(featureSizeBlocks, sampleDistance);

    vec2 warpFreq = vec2(CLOUD_VISUAL_WARP_FREQUENCY / featureSizeBlocks);
    vec2 warp = vec2(
        gradientNoise2D(rotated * warpFreq + vec2(patternSeed * 7.7, cloudSlotIndex * 3.3)),
        gradientNoise2D(rotated * warpFreq + vec2(cloudSlotIndex * 9.1, patternSeed * 4.4)));
    vec2 warpedPos = rotated + warp * featureSizeBlocks * clamp(noiseParams.y, 0.0, 1.5);

    vec2 baseScale      = vec2(1.0 / featureSizeBlocks);
    vec2 shapeSamplePos = warpedPos * baseScale;

    int   octaves     = lodFade > 0.5 ? 4 : 2;
    float billowField = billowGradient2D(shapeSamplePos, octaves, 2.1, 0.5);
    float sheetField  = fbmGradient2D(shapeSamplePos, octaves, 2.1, 0.5);
    float shapeField  = mix(sheetField, billowField, fullness);

    outShadingBias = fbmGradient2D(shapeSamplePos * 0.35 + vec2(19.3, -7.7), 2, 2.0, 0.5);

    float threshold = clamp(1.0 - (intensity * 0.6 + coverageBias * 0.4), 0.03, 0.95);
    float softness  = clamp(noiseParams.w, 0.04, 0.6) * mix(2.2, 1.0, lodFade);

    float coverage = smoothstep(threshold - softness, threshold + softness, shapeField);
    coverage = mix(coverage, coverage * coverage * (3.0 - 2.0 * coverage), CLOUD_VISUAL_EDGE_FIRMNESS);

    if (lodFade > CLOUD_VISUAL_EPSILON) {
        vec2  detailPos = warpedPos * (CLOUD_VISUAL_DETAIL_FREQUENCY / featureSizeBlocks) * max(noiseParams.x, 0.1);
        float detail    = billowGradient2D(detailPos, 2, 2.2, 0.5) - 0.5;
        coverage = clamp(
            coverage + detail * CLOUD_VISUAL_DETAIL_STRENGTH * lodFade * (1.0 - coverage), 0.0, 1.0);
    }

    coverage *= outerFade * clumpMask;

    if (coverage <= CLOUD_VISUAL_EPSILON)
    return 0.0;

    float probeEps = featureSizeBlocks * CLOUD_VISUAL_GRADIENT_PROBE_RATIO;
    float gx = gradientNoise2D((warpedPos + vec2(probeEps, 0.0)) * baseScale)
    - gradientNoise2D((warpedPos - vec2(probeEps, 0.0)) * baseScale);
    float gz = gradientNoise2D((warpedPos + vec2(0.0, probeEps)) * baseScale)
    - gradientNoise2D((warpedPos - vec2(0.0, probeEps)) * baseScale);
    outGradient = vec2(gx, gz) * CLOUD_VISUAL_GRADIENT_SCALE * lodFade;

    return coverage;
}

float resolveCloudVerticalDensity(
    vec3 stepWorldPos, vec2 patternLocalXZ, float slabBottomY, float slabTopY,
    float fullness, float shadingBias, float patternSeed, float lodFade,
    out float verticalNorm, out float verticalT) {
    float span = max(slabTopY - slabBottomY, 0.0001);

    verticalT    = (stepWorldPos.y - slabBottomY) / span;
    verticalNorm = 1.0;

    if (verticalT < -CLOUD_VISUAL_SLAB_OVERSHOOT || verticalT > 1.0 + CLOUD_VISUAL_SLAB_OVERSHOOT)
    return 0.0;

    float centerT   = mix(CLOUD_VISUAL_CENTER_T_MIN, CLOUD_VISUAL_CENTER_T_MAX, clamp(shadingBias, 0.0, 1.0));
    float belowSpan = mix(CLOUD_VISUAL_BELOW_SPAN_MIN, CLOUD_VISUAL_BELOW_SPAN_MAX, fullness);
    float aboveSpan = mix(CLOUD_VISUAL_ABOVE_SPAN_MIN, CLOUD_VISUAL_ABOVE_SPAN_MAX, fullness);

    float offset = verticalT - centerT;
    bool  below  = offset < 0.0;

    verticalNorm = below
    ? -offset / max(belowSpan, 0.001)
    : offset / max(aboveSpan, 0.001);

    float edgeStart = below
    ? mix(CLOUD_VISUAL_BASE_EDGE_SOFT, CLOUD_VISUAL_BASE_EDGE_HARD, fullness)
    : mix(CLOUD_VISUAL_TOP_EDGE_SHEET, CLOUD_VISUAL_TOP_EDGE_PUFFY, fullness);

    float profile = 1.0 - smoothstep(edgeStart, 1.0, verticalNorm);

    if (profile <= CLOUD_VISUAL_EPSILON)
    return 0.0;

    float roundTaper   = below ? 1.0 : sqrt(max(1.0 - verticalNorm * verticalNorm, 0.0));
    float lateralTaper = clamp(mix(1.0, roundTaper, fullness), 0.0, 1.0);

    float wispNoise = gradientNoise2D(
        patternLocalXZ * u_weatherHeightVariation.w + vec2(patternSeed * 13.1, patternSeed * 7.7));
    float wisp = mix(1.0, mix(1.0 - CLOUD_VISUAL_WISP_STRENGTH, 1.0, wispNoise * 0.5 + 0.5), lodFade);

    return profile * wisp * lateralTaper;
}

vec3 resolveCloudStepNormal(vec3 domeNormal, vec2 shapeGradient, float verticalT, float fullness) {
    float centered   = clamp(verticalT * 2.0 - 1.0, -1.0, 1.0);
    float sideWeight = (1.0 - abs(centered)) * mix(CLOUD_VISUAL_LATERAL_LEAN_SHEET, 1.0, fullness);

    vec3  lateralLean = vec3(shapeGradient.x, 0.0, shapeGradient.y);
    float lateralLen  = length(lateralLean);
    vec3  lateralDir  = lateralLen > CLOUD_VISUAL_EPSILON ? lateralLean / lateralLen : vec3(0.0);

    vec3  assembled    = domeNormal * (centered + CLOUD_VISUAL_NORMAL_UP_FLOOR) + lateralDir * sideWeight;
    float assembledLen = length(assembled);

    return assembledLen > CLOUD_VISUAL_EPSILON ? assembled / assembledLen : domeNormal;
}

// Wrapped diffuse with a sky-gradient ambient. The wrap removes the hard
// terminator that reads as plastic, and taking ambient from the horizon/zenith
// gradient rather than a flat constant is what puts the day's own colour into
// the cloud body — cream and warm near the horizon, cooler overhead.
vec3 shadeCloudStep(
    vec3 rayDir, vec3 stepNormal, float bodyDepth, float selfShadow, float thicknessNorm,
    vec4 colorScale, vec4 materialParams) {
    vec3 sunDir  = normalize(u_sunDirection);
    vec3 moonDir = normalize(u_moonDirection);

    float saturation = materialParams.x;

    float sunWrap  = clamp(dot(stepNormal, sunDir) * CLOUD_VISUAL_LIGHT_WRAP
            + (1.0 - CLOUD_VISUAL_LIGHT_WRAP), 0.0, 1.0);
    float moonWrap = clamp(dot(stepNormal, moonDir) * CLOUD_VISUAL_LIGHT_WRAP
            + (1.0 - CLOUD_VISUAL_LIGHT_WRAP), 0.0, 1.0);

    float shadow = mix(CLOUD_VISUAL_SHADOW_FLOOR, 1.0, selfShadow);

    vec3 direct = u_sunColor * u_sunIntensity * sunWrap
    + u_moonColor * vec3(0.58, 0.74, 1.00) * min(u_moonIntensity, 0.18) * moonWrap;

    float skyBlend = clamp(stepNormal.y * 0.5 + 0.5, 0.0, 1.0);
    vec3  ambient  = mix(u_skyHorizonColor, u_skyZenithColor, skyBlend)
    * mix(CLOUD_VISUAL_AMBIENT_BASE, CLOUD_VISUAL_AMBIENT_TOP, skyBlend)
    * mix(1.0, 0.65, thicknessNorm);

    float luminance = dot(colorScale.rgb, vec3(0.299, 0.587, 0.114));
    vec3  albedo    = mix(vec3(luminance), colorScale.rgb, saturation);
    albedo = mix(albedo, albedo * u_skyCloudColor, CLOUD_VISUAL_SKY_TINT_STRENGTH);

    vec3 shaded = albedo * (ambient + direct * shadow);
    shaded *= mix(CLOUD_VISUAL_STORM_DARKEN_MIN, 1.0, saturation);

    float sunAlignment = clamp(dot(rayDir, sunDir), 0.0, 1.0);
    float rim          = pow(sunAlignment, CLOUD_VISUAL_RIM_POWER) * (1.0 - bodyDepth);
    shaded += u_sunColor * u_sunIntensity * rim * CLOUD_VISUAL_RIM_STRENGTH * shadow;

    return shaded;
}

#endif