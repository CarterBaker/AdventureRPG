// CloudVolumeShader.fsh
#version 330 core

in vec3       vLocalPos;
flat in int   vInstanceID;
flat in vec3  vBoxCenter;
flat in vec3  vBoxHalfExtent;

out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"

const int   CLOUD_RAYMARCH_STEPS       = 28;
const float CLOUD_DENSITY_ABSORPTION   = 1.35;
const float CLOUD_TRANSMITTANCE_CUTOFF = 0.01;

// Local box-space density: vertical profile from fullness, horizontal
// silhouette softness at the footprint edge, domain-warped Perlin-Worley
// shape noise scaled to the cloud's own authored width, biased by how
// purely this weather is currently expressed (intensity).
float sampleCloudDensity(
    vec3 worldPos,
    vec3 boxCenter,
    vec3 halfExtent,
    vec4 noiseParams,
    vec4 colorScale,
    vec4 materialParams,
    vec2 windDrift,
    float intensity) {
    vec3 rel = worldPos - boxCenter;
    vec2 planarNorm = rel.xz / max(halfExtent.xz, vec2(0.01));
    float heightNorm = clamp((rel.y / max(halfExtent.y, 0.01)) * 0.5 + 0.5, 0.0, 1.0);

    float fullness = materialParams.y;
    float vertShape = 1.0 - abs(heightNorm - 0.5) * 2.0;
    vertShape = pow(clamp(vertShape, 0.0, 1.0), mix(2.4, 0.6, fullness));

    float edgeDist = 1.0 - max(abs(planarNorm.x), abs(planarNorm.y));
    float softness = max(noiseParams.w, 0.02);
    float edgeShape = smoothstep(0.0, softness, edgeDist);

    float scale = max(colorScale.w, 1.0);
    vec3 evolvePos = worldPos + vec3(windDrift.x, u_time * 0.4, windDrift.y);
    vec3 noisePos  = evolvePos / scale * max(noiseParams.x, 0.01);

    vec3 warp = curlNoise3D(noisePos * 0.6) * noiseParams.y;
    float shapeNoise = perlinWorley3D(noisePos + warp, 3.0);

    float effectiveBias = clamp(noiseParams.z * mix(0.5, 1.0, intensity), 0.0, 0.98);
    float coverage = remapClamped(shapeNoise, 1.0 - effectiveBias, 1.0, 0.0, 1.0);

    return coverage * vertShape * edgeShape;
}

void main() {
    vec4 patternState   = u_weatherPatternState[vInstanceID];
    vec4 cloudShape      = u_weatherCloudShape[vInstanceID];
    vec4 cloudNoise      = u_weatherCloudNoise[vInstanceID];
    vec4 cloudColorScale = u_weatherCloudColorScale[vInstanceID];
    vec4 cloudMaterial   = u_weatherCloudMaterial[vInstanceID];

    float fadeAlpha = patternState.w;
    float intensity = patternState.y;

    if (fadeAlpha <= 0.001 || cloudShape.z <= 0.001)
    discard;

    // Ray-AABB test in normalized local box space handles the camera
    // being inside the box, regardless of which face got rasterized.
    vec3 localCamPos  = (u_cameraPosition - vBoxCenter) / vBoxHalfExtent;
    vec3 localFragPos = vLocalPos * 2.0;
    vec3 rayDirLocal  = normalize(localFragPos - localCamPos);

    vec3 invDir = 1.0 / rayDirLocal;
    vec3 t0 = (vec3(-1.0) - localCamPos) * invDir;
    vec3 t1 = (vec3(1.0) - localCamPos) * invDir;
    vec3 tMinV = min(t0, t1);
    vec3 tMaxV = max(t0, t1);
    float tNear = max(max(tMinV.x, tMinV.y), tMinV.z);
    float tFar  = min(min(tMaxV.x, tMaxV.y), tMaxV.z);

    if (tFar <= max(tNear, 0.0))
    discard;

    tNear = max(tNear, 0.0);

    vec3 entryWorld = vBoxCenter + (localCamPos + rayDirLocal * tNear) * vBoxHalfExtent;
    vec3 exitWorld  = vBoxCenter + (localCamPos + rayDirLocal * tFar)  * vBoxHalfExtent;

    vec3  stepVec    = (exitWorld - entryWorld) / float(CLOUD_RAYMARCH_STEPS);
    float stepLength = length(stepVec);

    vec2 windDrift = u_windDriftOffset * cloudShape.w;

    float transmittance    = 1.0;
    vec3  accumulatedColor = vec3(0.0);
    vec3  samplePos        = entryWorld + stepVec * 0.5;

    float sunFacing  = clamp(dot(vec3(0.0, 1.0, 0.0), normalize(u_sunDirection)), 0.0, 1.0);
    float moonFacing = clamp(dot(vec3(0.0, 1.0, 0.0), normalize(u_moonDirection)), 0.0, 1.0);
    vec3  moonTint   = vec3(0.58, 0.74, 1.00);
    vec3  directLight = u_sunColor * u_sunIntensity * mix(0.6, 1.0, sunFacing)
    + u_moonColor * moonTint * min(u_moonIntensity, 0.18) * mix(0.6, 1.0, moonFacing);

    vec3  rawAlbedo    = cloudColorScale.rgb;
    float luminance    = dot(rawAlbedo, vec3(0.299, 0.587, 0.114));
    vec3  tintedAlbedo = mix(vec3(luminance), rawAlbedo, cloudMaterial.x);
    tintedAlbedo = mix(tintedAlbedo, tintedAlbedo * u_skyCloudColor, 0.35);

    for (int i = 0; i < CLOUD_RAYMARCH_STEPS; i++) {
        if (transmittance < CLOUD_TRANSMITTANCE_CUTOFF)
        break;

        float density = sampleCloudDensity(
            samplePos, vBoxCenter, vBoxHalfExtent,
            cloudNoise, cloudColorScale, cloudMaterial,
            windDrift, intensity);

        if (density > 0.001) {
            float stepAbsorption = clamp(density * cloudShape.z * stepLength * CLOUD_DENSITY_ABSORPTION, 0.0, 1.0);

            float heightNorm = clamp(((samplePos.y - vBoxCenter.y) / max(vBoxHalfExtent.y, 0.01)) * 0.5 + 0.5, 0.0, 1.0);
            float ambient    = mix(0.10, 0.22, heightNorm);
            vec3  litColor   = tintedAlbedo * (directLight * mix(0.4, 1.0, heightNorm) + ambient);

            accumulatedColor += litColor * stepAbsorption * transmittance;
            transmittance    *= (1.0 - stepAbsorption);
        }

        samplePos += stepVec;
    }

    float coverage = (1.0 - transmittance) * fadeAlpha;

    if (coverage <= 0.003)
    discard;

    fragColor = vec4(accumulatedColor, coverage);
}