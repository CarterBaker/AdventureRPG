#version 400 core

in vec3       vLocalPos;
in vec3       vUVLocalPos;
in vec3       vNormal;
flat in vec2  vUVOrigin;
flat in float vOrient;
in vec3 vColor;
flat in float vCoverage;

#include "includes/CameraData.glsl"
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
#include "surface/includes/SurfaceTierFlat.glsl"
#include "surface/includes/CloudShadow.glsl"
#include "surface/includes/Coverage.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

/*
 * Material detail follows the same detail strength the TES displaces with,
 * via getDetailStrength() (SurfaceTessellationTier.glsl) evaluated against
 * this fragment's own vLocalPos, so normal, specular and ambient occlusion
 * maps and the full draw of a covering fade out across exactly the band the
 * bevel and relief fade across. Every fragment first runs the flat tier for
 * its albedo; beyond the radius that is the whole material, with any
 * covering laid over it by its baked approximation, and within it the detail
 * tier and the covering's full draw take over, the base maps read only where
 * no covering texel shows. Inside the fade band both are drawn and blended,
 * so nothing pops. This selection governs material detail only — it has no
 * bearing on fog, which the deferred Lighting.fsh pass computes from the
 * fragment's reconstructed world position.
 */

// Sun visibility under the grid's cloud layers, written into gMaterial.r so
// the deferred lighting pass can attenuate direct sunlight without resampling
// WeatherMapData itself. The occluding cloud lies toward the sun, so the
// offset follows u_sunDirection. Skipped entirely once the sun is below the
// horizon, since nothing is around to cast a shadow then.
const float SUN_SHADOW_MIN_ELEVATION = 0.05;

float resolveSunVisibility() {
    if (u_sunIntensity <= 0.0)
    return 1.0;

    vec2 sunHorizonOffset = u_sunDirection.xz / max(u_sunDirection.y, SUN_SHADOW_MIN_ELEVATION);

    return 1.0 - sampleCloudShadow(vLocalPos, sunHorizonOffset);
}

void main() {
    vec2  tiledUV = tileUV(vUVLocalPos, vUVOrigin, vNormal, vOrient);
    float detail  = getDetailStrength(vLocalPos);

    vec3  albedo;
    vec3  normalView;
    float specular;
    float ao;

    if (!shadeSurfaceFlat(tiledUV, vNormal, u_view, albedo, normalView, specular, ao))
    discard;

    vec3 farAlbedo = detail < 1.0 ? approximateCoverage(albedo, vCoverage, vColor) : albedo;

    if (detail > 0.0) {

        vec3  nearAlbedo;
        vec3  nearNormalView;
        float nearSpecular;
        float nearAO;

        bool covered = sampleCoverageDetail(
            vUVLocalPos, tiledUV, vUVOrigin, vCoverage, vColor, vNormal, u_view,
            nearAlbedo, nearNormalView, nearSpecular, nearAO);

        if (!covered) {
            nearAlbedo = albedo;
            sampleSurfaceDetail(tiledUV, vNormal, u_view, nearNormalView, nearSpecular, nearAO);
        }

        albedo     = mix(farAlbedo, nearAlbedo, detail);
        normalView = normalize(mix(normalView, nearNormalView, detail));
        specular   = mix(specular, nearSpecular, detail);
        ao         = mix(ao, nearAO, detail);
    }
    else {
        albedo = farAlbedo;
    }

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