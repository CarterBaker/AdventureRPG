#version 330 core

in vec3 v_dir;
in vec2 v_screenPos;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"
#include "includes/NoiseUtility.glsl"

uniform float u_cloudAltitudeMin;
uniform float u_cloudAltitudeMax;
uniform float u_cloudMaxDistance;

// Step count adapts to how far a ray actually travels through the layer —
// a steep ray crosses a thin altitude band and sits near the minimum; a
// grazing, near-horizontal ray can cross thousands of blocks and climbs
// toward the maximum. A fixed step count starved grazing rays of samples,
// which is what produced the low-resolution, angle-dependent "clipping".
const int   CLOUD_RAYMARCH_MIN_STEPS          = 40;
const int   CLOUD_RAYMARCH_MAX_STEPS          = 80;
const float CLOUD_RAYMARCH_TARGET_STEP_LENGTH = 20.0;

// Fraction of u_cloudMaxDistance — the true visibility limit — used to fade
// the far end out smoothly instead of hard-clipping. Anchored to
// u_cloudMaxDistance itself rather than each ray's own tFar: a steep ray's
// tFar is just wherever it exits the thin altitude slab, often far short of
// the real visibility limit, so fading relative to that made clouds fade
// out at close range the moment the camera pitched away from grazing —
// clouds appeared to clip out simply from tilting the view.
const float CLOUD_FAR_FADE_FRACTION = 0.2;

const float CLOUD_DENSITY_ABSORPTION   = 1.35;
const float CLOUD_TRANSMITTANCE_CUTOFF = 0.01;
const float CLOUD_DENSITY_EPSILON      = 0.001;

const float CLOUD_WIND_SCROLL_SCALE = 1.2;
const float CLOUD_DRIFT_TIME_SCALE  = 0.1;
const float CLOUD_PUFF_ANGLE_WOBBLE = 1.2;
const float CLOUD_EDGE_EROSION_HARD = 1.9;
const float CLOUD_EDGE_EROSION_SOFT = 0.7;
const float CLOUD_OUTER_FADE_START  = 0.85;
const float CLOUD_OUTER_FADE_END    = 1.35;

// heightNorm is written out for the caller's vertical falloff/ambient use.
// chunkOffsetBlocks reconstructs a world coordinate that stays continuous
// across a reference-chunk requantization for the noise sample only —
// worldPos itself resets by a full chunk width whenever that happens, and
// the bounds/edge test tolerates that (both sides shift together), but raw
// noise coordinates do not.
//
// The silhouette is a rotated, anisotropic "puff" region derived from
// bounds rather than the box itself, aligned to the local wind direction
// and randomized per pattern instance. Coverage erodes toward the edge of
// that region through the noise field, so the boundary reads as ragged and
// organic instead of a hard rectangle; a generous soft multiply past the
// nominal radius is only a final backstop.
float sampleEntryDensity(
    vec4 bounds,
    vec4 shape,
    vec4 noiseParams,
    vec4 colorScale,
    vec4 variance0,
    vec4 variance1,
    float intensity,
    vec3 worldPos,
    vec2 chunkOffsetBlocks,
    vec2 windDirNorm,
    float windSpeed,
    out float heightNorm) {
    float halfThickness = max(shape.x * 0.5, 0.01);
    float rawHeightT = (worldPos.y - shape.y) / halfThickness;
    heightNorm = clamp(rawHeightT * 0.5 + 0.5, 0.0, 1.0);

    if (abs(rawHeightT) > 1.0)
    return 0.0;

    vec2 boxCenter     = (bounds.xy + bounds.zw) * 0.5;
    vec2 boxHalfExtent = max((bounds.zw - bounds.xy) * 0.5, vec2(1.0));
    vec2 fromCenter    = worldPos.xz - boxCenter;

    vec2 cullExtent = boxHalfExtent * (CLOUD_OUTER_FADE_END * 2.0);
    if (abs(fromCenter.x) > cullExtent.x || abs(fromCenter.y) > cullExtent.y)
    return 0.0;

    float patternSeed    = variance1.z;
    float cloudTypeIndex = variance1.y;
    float instanceHash   = hash31(vec3(
            patternSeed * 12.9898,
            cloudTypeIndex * 78.233 + patternSeed,
            patternSeed - cloudTypeIndex * 0.577));

    float windAngle = atan(windDirNorm.y, windDirNorm.x);
    float puffAngle = windAngle + (instanceHash - 0.5) * CLOUD_PUFF_ANGLE_WOBBLE;
    float cosA = cos(puffAngle);
    float sinA = sin(puffAngle);

    vec2 rotated = vec2(
        fromCenter.x * cosA + fromCenter.y * sinA,
        fromCenter.y * cosA - fromCenter.x * sinA);

    float elongation  = clamp(mix(variance0.w, variance1.x, fract(instanceHash * 3.17)), 1.0, 6.0);
    float spreadRatio = clamp(variance0.x, 0.1, 2.0);

    vec2 anisotropicExtent = boxHalfExtent * spreadRatio * vec2(1.0, 1.0 / elongation);
    vec2 spreadNorm  = rotated / max(anisotropicExtent, vec2(1.0));
    float radialDist = length(spreadNorm);

    if (radialDist > CLOUD_OUTER_FADE_END)
    return 0.0;

    float outerFade = 1.0 - smoothstep(CLOUD_OUTER_FADE_START, CLOUD_OUTER_FADE_END, radialDist);

    float sizeVariance  = clamp(mix(variance0.y, variance0.z, fract(instanceHash * 5.63)), 0.3, 3.0);
    float instanceScale = max(colorScale.w, 1.0) * sizeVariance;

    vec2 windScroll = windDirNorm * windSpeed * u_time * CLOUD_WIND_SCROLL_SCALE * shape.w;
    vec3 stableWorldPos = vec3(worldPos.x + chunkOffsetBlocks.x, worldPos.y, worldPos.z + chunkOffsetBlocks.y);
    vec3 evolvePos = stableWorldPos + vec3(windScroll.x, u_time * CLOUD_DRIFT_TIME_SCALE, windScroll.y);

    vec3 instanceOffset = vec3(patternSeed, fract(instanceHash * 7.0), fract(instanceHash * 13.0)) * 128.0;
    vec3 noisePos = evolvePos / instanceScale * max(noiseParams.x, 0.01) + instanceOffset;

    vec3 warp = curlNoise3D(noisePos * 0.6) * noiseParams.y;
    float shapeNoise = perlinWorley3D(noisePos + warp, 3.0);

    float softness        = clamp(noiseParams.w, 0.0, 1.0);
    float erosionStrength = mix(CLOUD_EDGE_EROSION_HARD, CLOUD_EDGE_EROSION_SOFT, softness);
    float archetypeBias   = (noiseParams.z - 0.6) * 0.5;
    float baseBias        = clamp(intensity + archetypeBias, 0.02, 0.95);
    float effectiveBias   = clamp(baseBias - radialDist * erosionStrength, 0.0, 0.98);

    float coverage = remapClamped(shapeNoise, 1.0 - effectiveBias, 1.0, 0.0, 1.0);

    return coverage * outerFade;
}

bool intersectAltitudeSlab(vec3 origin, vec3 dir, float minY, float maxY, out float tNear, out float tFar) {
    if (abs(dir.y) < 0.0001) {
        if (origin.y < minY || origin.y > maxY)
        return false;
        tNear = 0.0;
        tFar  = 1000000.0;
        return true;
    }

    float t0 = (minY - origin.y) / dir.y;
    float t1 = (maxY - origin.y) / dir.y;

    tNear = max(min(t0, t1), 0.0);
    tFar  = max(t0, t1);

    return tFar > tNear;
}

void main() {
    int entryCount = min(u_weatherEntryCount, WEATHER_MAP_MAX_ENTRIES);

    if (entryCount == 0)
    discard;

    float layerMinY = max(u_weatherCloudLayerMinY, u_cloudAltitudeMin);
    float layerMaxY = min(u_weatherCloudLayerMaxY, u_cloudAltitudeMax);

    if (layerMaxY <= layerMinY)
    discard;

    vec3 rayDir    = normalize(v_dir);
    vec3 rayOrigin = u_cameraPosition;

    vec2 chunkOffsetBlocks = vec2(float(u_playerChunkX), float(u_playerChunkZ)) * u_chunkSize;

    float tNear, tFar;
    if (!intersectAltitudeSlab(rayOrigin, rayDir, layerMinY, layerMaxY, tNear, tFar))
    discard;

    tFar = min(tFar, u_cloudMaxDistance);

    if (tFar <= tNear)
    discard;

    vec2 windDirNorm = vec2(1.0, 0.0);
    float windXZLen = length(u_windDirection.xz);
    if (windXZLen > 0.0001)
    windDirNorm = u_windDirection.xz / windXZLen;
    float windSpeed = u_windSpeed;

    float rayLength = tFar - tNear;
    int stepCount = clamp(
        int(rayLength / CLOUD_RAYMARCH_TARGET_STEP_LENGTH),
        CLOUD_RAYMARCH_MIN_STEPS,
        CLOUD_RAYMARCH_MAX_STEPS);

    // Per-pixel dither on the start offset breaks up the banding a fixed
    // sample grid produces on long, coarsely-stepped grazing-angle rays.
    float dither = hash31(vec3(gl_FragCoord.xy, 0.0)) - 0.5;

    vec3 entryWorld = rayOrigin + rayDir * tNear;
    vec3 exitWorld  = rayOrigin + rayDir * tFar;

    vec3  stepVec      = (exitWorld - entryWorld) / float(stepCount);
    float stepLength   = length(stepVec);
    float farFadeStart = u_cloudMaxDistance * (1.0 - CLOUD_FAR_FADE_FRACTION);

    float sunFacing  = clamp(dot(vec3(0.0, 1.0, 0.0), normalize(u_sunDirection)), 0.0, 1.0);
    float moonFacing = clamp(dot(vec3(0.0, 1.0, 0.0), normalize(u_moonDirection)), 0.0, 1.0);
    vec3  moonTint   = vec3(0.58, 0.74, 1.00);
    vec3  directLight = u_sunColor * u_sunIntensity * mix(0.6, 1.0, sunFacing)
    + u_moonColor * moonTint * min(u_moonIntensity, 0.18) * mix(0.6, 1.0, moonFacing);

    float transmittance    = 1.0;
    vec3  accumulatedColor = vec3(0.0);
    float traveled         = tNear + stepLength * (0.5 + dither);
    vec3  samplePos        = entryWorld + stepVec * (0.5 + dither);

    for (int s = 0; s < stepCount; s++) {
        if (transmittance < CLOUD_TRANSMITTANCE_CUTOFF)
        break;

        float farFade = 1.0 - smoothstep(farFadeStart, u_cloudMaxDistance, traveled);

        if (farFade > CLOUD_DENSITY_EPSILON) {
            for (int i = 0; i < entryCount; i++) {
                vec4 patternState = u_weatherPatternState[i];
                float intensity  = patternState.x;
                float fadeAlpha  = patternState.y;
                float rangeFade  = patternState.w;

                if (intensity <= CLOUD_DENSITY_EPSILON || fadeAlpha <= CLOUD_DENSITY_EPSILON
                    || rangeFade <= CLOUD_DENSITY_EPSILON)
                continue;

                vec4 shape = u_weatherCloudShape[i];

                if (shape.z <= CLOUD_DENSITY_EPSILON)
                continue;

                vec4 bounds      = u_weatherBounds[i];
                vec4 noiseParams = u_weatherCloudNoise[i];
                vec4 colorScale  = u_weatherCloudColorScale[i];
                vec4 materialParams = u_weatherCloudMaterial[i];
                vec4 variance0   = u_weatherCloudVariance0[i];
                vec4 variance1   = u_weatherCloudVariance1[i];

                float heightNorm;
                float density = sampleEntryDensity(
                    bounds, shape, noiseParams, colorScale, variance0, variance1,
                    intensity, samplePos, chunkOffsetBlocks, windDirNorm, windSpeed, heightNorm)
                * fadeAlpha * rangeFade * farFade;

                if (density <= CLOUD_DENSITY_EPSILON)
                continue;

                float fullness  = materialParams.y;
                float vertShape = 1.0 - abs(heightNorm - 0.5) * 2.0;
                vertShape = pow(clamp(vertShape, 0.0, 1.0), mix(2.4, 0.6, fullness));

                if (vertShape <= 0.0)
                continue;

                float stepAbsorption = clamp(
                    density * vertShape * shape.z * stepLength * CLOUD_DENSITY_ABSORPTION, 0.0, 1.0);

                float luminance    = dot(colorScale.rgb, vec3(0.299, 0.587, 0.114));
                vec3  tintedAlbedo = mix(vec3(luminance), colorScale.rgb, materialParams.x);
                tintedAlbedo = mix(tintedAlbedo, tintedAlbedo * u_skyCloudColor, 0.35);

                float ambient  = mix(0.10, 0.22, heightNorm);
                vec3  litColor = tintedAlbedo * (directLight * mix(0.4, 1.0, heightNorm) + ambient);

                accumulatedColor += litColor * stepAbsorption * transmittance;
                transmittance    *= (1.0 - stepAbsorption);
            }
        }

        samplePos += stepVec;
        traveled  += stepLength;
    }

    float coverage = 1.0 - transmittance;

    if (coverage <= 0.003)
    discard;

    // accumulatedColor is premultiplied (front-to-back integration already
    // weights each contribution by remaining transmittance). This target's
    // render-into blend is GL_ONE/GL_ONE_MINUS_SRC_ALPHA (see FboData's
    // premultipliedBlend flag), which stores that premultiplied pair into
    // WeatherScene unmodified. Un-premultiplying here means the FINAL blit
    // of WeatherScene onto the screen — which uses the normal straight-alpha
    // blend shared with every other pass — reconstructs the correct
    // premultiplied-over composite on its own: color*coverage + dst*(1-coverage).
    vec3 straightColor = accumulatedColor / max(coverage, 0.0001);

    fragColor = vec4(straightColor, coverage);
}