// StandardSurfaceShader.fsh
#version 400 core

in vec3       vLocalPos;
in vec3       vUVLocalPos;
in vec3       vNormal;
flat in vec2  vUVOrigin;
flat in float vOrient;
in float      vColor;

#include "includes/CameraData.glsl"
#include "includes/GridCoordinateData.glsl"
#include "includes/SettingsData.glsl"
#include "includes/SunLightData.glsl"
#include "surface/includes/SurfaceStandard.glsl"
#include "includes/BlockOrientationMapData.glsl"
#include "surface/includes/TiledSampling.glsl"
#include "surface/includes/Albedo.glsl"
#include "surface/includes/Normal.glsl"
#include "surface/includes/AO.glsl"
#include "surface/includes/Specular.glsl"
#include "surface/includes/SurfaceTessellationTier.glsl"
#include "surface/includes/SurfaceTierFull.glsl"
#include "surface/includes/SurfaceTierMid.glsl"
#include "surface/includes/SurfaceTierFlat.glsl"
#include "surface/includes/CloudShadow.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

/*
* Tier selection mirrors the TCS/TES exactly, via the shared thresholds in
 * SurfaceTessellationTier.glsl, so all three stages always agree on the
 * same ring boundaries. This selection governs geometric material-sampling
 * detail only — it has no bearing on fog. Atmospheric fog is computed
 * entirely in the deferred Lighting.fsh pass from the fragment's
 * reconstructed world position, so it stays perfectly continuous across
 * every ring boundary regardless of chunk size. See
 * surface/includes/AtmosphericFog.glsl.
 */

// Sun visibility under in-range cloud cover, written into gMaterial.r so the
// deferred lighting pass can attenuate direct sunlight without resampling
// WeatherMapData itself. Skipped entirely once the sun is below the
// horizon, since nothing is around to cast a shadow then.
const float SUN_SHADOW_MIN_ELEVATION = 0.05;

float resolveSunVisibility() {
    if (u_sunIntensity <= 0.0)
    return 1.0;

    vec2 sunHorizonOffset = -u_sunDirection.xz / max(u_sunDirection.y, SUN_SHADOW_MIN_ELEVATION);

    return 1.0 - sampleCloudShadow(vLocalPos, sunHorizonOffset);
}

void main() {
    vec2 tiledUV = tileUV(vUVLocalPos, vUVOrigin, vNormal, vOrient);

    float tier0MaxSqDist = getTier0MaxSqDist();
    float tier1MaxSqDist = getTier1MaxSqDist();

    vec3  albedo;
    vec3  normalView;
    float specular;
    float ao;
    bool  visible;

    if (u_distanceFromCenter <= tier0MaxSqDist) {
        visible = shadeSurfaceFull(tiledUV, vNormal, u_view, albedo, normalView, specular, ao);
    }
    else if (u_distanceFromCenter <= tier1MaxSqDist) {
        visible = shadeSurfaceMid(tiledUV, vNormal, u_view, albedo, normalView, specular, ao);
    }
    else {
        visible = shadeSurfaceFlat(tiledUV, vNormal, u_view, albedo, normalView, specular, ao);
    }

    if (!visible)
    discard;

    float sunVisibility = resolveSunVisibility();

    // This pass draws with blending ENABLED (GL_SRC_ALPHA,
    // GL_ONE_MINUS_SRC_ALPHA — see RenderSystem.drawToMappedTargets), so
    // every output's alpha controls whether the write happens at all. All
    // three targets must output alpha = 1.0; gMaterial packs
    // r = sun visibility (1 = full sun, 0 = fully cloud-shadowed),
    // g = specular, b = ao, a = 1.0 (forced, no data).
    gAlbedo   = vec4(albedo, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(sunVisibility, specular, ao, 1.0);
}