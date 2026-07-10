#version 330 core

in vec3  vWorldPos;
in vec3  vNormal;
in float vRandomSeed;
in float vFadeAlpha;
in float vIntensity;
flat in vec3 vBoxMin;
flat in vec3 vBoxMax;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

#include "includes/CameraData.glsl"
#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "sky/util/CloudShapeUtility.glsl"

/*
* ============================================================================
 * VOLUMETRIC REWORK — this replaces the old flat box-face "puff mask"
 * (measuring distance from the fragment to the box's own center and
 * discarding outside a core/outer radius — see prior revisions of this
 * file) with a genuine raymarch through this instance's own AABB
 * (vBoxMin/vBoxMax, computed once per-instance in the vertex shader),
 * sampling the exact same sampleCloudDensity() the sky dome uses (see
 * sky/util/CloudShapeUtility.glsl and sky/util/Clouds.glsl) — so a near
 * cloud object and the distant sky representation of the same weather are
 * built from one shared density function, never two independently-tuned
 * looks.
 *
 * Every front-facing fragment GL rasterizes for this box IS a ray-entry
 * point into the volume — the box is convex and we only ever see its
 * outer surface — so the "entry" side of the raymarch is free. This
 * shader marches from the fragment's own position toward wherever that
 * same view ray exits the box (a simple AABB slab test against
 * vBoxMin/vBoxMax), accumulating density front-to-back exactly like the
 * sky dome's own raymarch.
 *
 * DEFERRED-LIGHTING FIX — this used to also shade each sample with real
 * sun/moon color and intensity (shadeCloudLit()), bake THAT into the
 * accumulated color, and write it straight into gAlbedo. But gAlbedo
 * feeds the exact same deferred Lighting.fsh pass every opaque terrain
 * fragment goes through — see LightingShader.fsh, which treats gAlbedo as
 * raw UNLIT surface color and multiplies it by sky ambient plus sun/moon
 * diffuse and specular all over again, using gNormal for the directional
 * terms. Every cloud pixel was getting lit TWICE: once here, once in the
 * deferred pass.
 *
 * This shader now only ever produces UNLIT shape data — a toon-banded
 * base color via shadeCloudUnlit() (the deferred-safe sibling of the sky
 * dome's own shadeCloudLit(), with a real light-direction self-shadow
 * tap folded in as shape/AO information rather than surface radiance)
 * plus a real accumulated ambient-occlusion value for gMaterial.b.
 * Lighting.fsh is completely unmodified — it already knows how to light
 * an albedo+normal+ao G-buffer fragment; it now only ever does that once
 * for a cloud pixel, exactly like it already does for every terrain
 * fragment.
 *
 * STAGE 1 — height-shaped density. sampleCloudDensity() now takes a
 * heightT parameter (this sample's normalized position within the box,
 * 0 = base, 1 = top — already computed below as `heightT`, just moved
 * earlier in the loop so both density calls can use it) and uses it to
 * both stretch the sampling domain (thin/streaky near the top of the
 * box, compact/puffy near the base) and apply a vertical density
 * gradient (flat-ish base, softer eroding top) — see
 * CloudShapeUtility.glsl's own doc comment. This is what gives an
 * individual Cumulus instance its flat underside and rounded top instead
 * of a uniform blob regardless of where in the box a sample falls.
 *
 * Still writes a COMPLETE G-buffer output (albedo, normal, material) and
 * real depth, exactly like StandardSurfaceShader.fsh — this pass runs
 * with blending ENABLED (see RenderSystem.drawToMappedTargets), so every
 * output here still forces alpha = 1.0 and instead pre-blends the
 * raymarched (still unlit) cloud color against an approximated
 * (also unlit) sky-behind color itself, same as before.
 * ============================================================================
 */

// Baked once per material clone in CloudRenderSystem.bakeArchetypeUniforms()
// — this archetype's own color/shape numbers, shared by every instance
// drawn through this material. u_cloudEdgeSoftness (the legacy card-shader
// field) is no longer read here — see CloudData's own doc comment; the
// raymarch below uses u_cloudSilhouetteSoftness for its density-threshold
// falloff instead. u_cloudPuffJitter is repurposed as the raymarch's fine
// detail-jitter input (sampleCloudDensity's detailJitter parameter) rather
// than removed outright, so no Java/JSON changes are needed for this pass.
uniform vec3  u_cloudColor;
uniform float u_cloudDensity;
uniform float u_cloudEdgeSoftness;
uniform float u_cloudPuffJitter;

uniform vec3  u_cloudTopColor;
uniform int   u_cloudToonBands;
uniform float u_cloudDensityNoiseScale;
uniform float u_cloudNoiseWarpStrength;
uniform float u_cloudCoverageBias;
uniform float u_cloudSilhouetteSoftness;
uniform vec3  u_cloudShadowColor;
uniform float u_cloudShadeStrength;
uniform float u_cloudRimLightStrength;
uniform float u_cloudAmbientOcclusionStrength;
uniform float u_cloudBrightnessMultiplier;

// Raymarch tuning — mirrors EngineSetting.CLOUD_VOLUME_RAYMARCH_STEPS_NEAR/
// FAR/TIER_NEAR_DISTANCE (reserved for exactly this purpose). Kept as
// shader consts for this stage; a later pass can push these through
// CloudSettingsData if they ever need to be runtime-tunable rather than
// per-build constants.
const int   CLOUD_RAYMARCH_STEPS_NEAR       = 48;
const int   CLOUD_RAYMARCH_STEPS_FAR        = 16;
const float CLOUD_RAYMARCH_TIER_DISTANCE    = 128.0;
const float CLOUD_RAYMARCH_STEP_ALPHA_SCALE = 0.09;
const float CLOUD_LIGHT_TAP_DISTANCE        = 2.5;

// Floor applied to vIntensity's density contribution — a forming/
// weakening cloud reads as thin and wispy, never fully invisible purely
// from intensity. True existence pop-in/pop-out is vFadeAlpha's job
// alone (see OverheadCellStruct.fadeAlpha / CloudVolumeShader.vsh's
// horizonFade) — intensity only ever thins density on top of whatever
// vFadeAlpha already decided about existence.
const float INTENSITY_DENSITY_FLOOR = 0.4;

void main() {
    // Cells that haven't faded in yet (or are nearly retired) contribute
    // nothing visible — bail before spending a single raymarch step.
    if (vFadeAlpha <= 0.001)
    discard;

    vec3 rayOrigin = vWorldPos;
    vec3 rayDir    = normalize(vWorldPos - u_cameraPosition);

    vec2 boxHit = intersectAABB(rayOrigin, rayDir, vBoxMin, vBoxMax);
    float marchStart = max(boxHit.x, 0.0);
    float marchLen   = max(boxHit.y - marchStart, 0.0);

    if (marchLen <= 0.001)
    discard;

    float camDist = length(vWorldPos - u_cameraPosition);
    int steps = camDist < CLOUD_RAYMARCH_TIER_DISTANCE ? CLOUD_RAYMARCH_STEPS_NEAR : CLOUD_RAYMARCH_STEPS_FAR;
    float stepSize = marchLen / float(steps);

    // Real light DIRECTION only — sun by day, blending toward moon as the
    // sun weakens, exactly like before. Never converted into a color/
    // intensity that could tint albedo — see the header comment.
    float sunWeight = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir  = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));

    float intensityFactor = mix(INTENSITY_DENSITY_FLOOR, 1.0, clamp(vIntensity, 0.0, 1.0));
    float boxHeight        = max(vBoxMax.y - vBoxMin.y, 0.001);

    vec4  accum   = vec4(0.0);
    float accumAO = 0.0;

    for (int i = 0; i < CLOUD_RAYMARCH_STEPS_NEAR; i++) {
        if (i >= steps)
        break;

        if (accum.a > 0.97)
        break;

        vec3 p = rayOrigin + rayDir * (marchStart + (float(i) + 0.5) * stepSize);

        // Moved above the density sample (Stage 1) — sampleCloudDensity()
        // now needs this to shape the cloud's own flat-base/rounded-top
        // silhouette and to stretch its sampling domain near the box top.
        float heightT = clamp((p.y - vBoxMin.y) / boxHeight, 0.0, 1.0);

        float rawDensity = sampleCloudDensity(
            p, heightT, u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength, u_cloudPuffJitter,
            u_cloudCoverageBias, u_cloudSilhouetteSoftness, vRandomSeed, u_time);
        float density = rawDensity * u_cloudDensity * intensityFactor;

        if (density > 0.01) {
            // Self-shadow tap — a second density sample one step toward the
            // light source. Shape/AO information only (how buried this
            // point is in cloud material relative to the light), never a
            // radiance value — see the header comment.
            float rawLit = sampleCloudDensity(
                p + lightDir * CLOUD_LIGHT_TAP_DISTANCE, heightT,
                u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength, u_cloudPuffJitter,
                u_cloudCoverageBias, u_cloudSilhouetteSoftness, vRandomSeed, u_time);
            float litDensity = rawLit * u_cloudDensity * intensityFactor;

            float lightLift = clamp((density - litDensity) * 2.0 + 0.5, 0.0, 1.0);

            float stepAO;
            vec3 shaded = shadeCloudUnlit(
                u_cloudColor, u_cloudTopColor, u_cloudShadowColor,
                heightT, lightLift, density,
                u_cloudToonBands, u_cloudShadeStrength,
                u_cloudAmbientOcclusionStrength, u_cloudBrightnessMultiplier,
                stepAO);

            float stepAlpha = clamp(density * CLOUD_RAYMARCH_STEP_ALPHA_SCALE * stepSize, 0.0, 1.0);
            float contribution = (1.0 - accum.a) * stepAlpha;

            accum.rgb += contribution * shaded;
            accumAO   += contribution * stepAO;
            accum.a   += contribution;
        }
    }

    float finalAlpha = clamp(accum.a * vFadeAlpha, 0.0, 1.0);

    if (finalAlpha <= 0.02)
    discard;

    // Manual "blend" against an approximate sky background, using the
    // actual view ray's altitude the same way the real sky pass blends
    // horizon -> zenith — see the header comment above for why this
    // replaces GL_SRC_ALPHA blending for a G-buffer write. This is still
    // UNLIT sky color (u_skyHorizonColor/u_skyZenithColor already bake in
    // day/night/season tinting from SkyColorBranch) mixed with UNLIT cloud
    // color — Lighting.fsh applies real sun/moon/ambient to the combined
    // result exactly once, same as every other gAlbedo fragment.
    float skyAltitude = clamp(rayDir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3  approxSky    = mix(u_skyHorizonColor, u_skyZenithColor, skyAltitude);
    vec3  blended      = mix(approxSky, accum.rgb, finalAlpha);

    // Average AO across accumulated samples — accum.a is the premultiplied
    // weight accumAO was built against, so dividing back out gives a
    // genuine [0,1] AO rather than an alpha-scaled one. Falls back to 1.0
    // (no occlusion) for the sky-only sliver of a fragment's coverage.
    float ao = mix(1.0, accumAO / max(accum.a, 0.0001), finalAlpha);

    vec3 normalView = normalize(mat3(u_view) * normalize(vNormal));

    // Cloud material packing mirrors StandardSurfaceShader's gMaterial
    // convention (r = fogT, g = specular, b = ao). Clouds don't apply the
    // terrain's distance-fog curve to themselves (r = 0) and are non-shiny
    // (g = 0) — b now carries the raymarch's own accumulated self-shadow
    // AO instead of a hardcoded 1.0, so Lighting.fsh's ambient term
    // actually responds to cloud shape instead of treating every cloud
    // pixel as fully exposed.
    gAlbedo   = vec4(blended, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(0.0, 0.0, ao, 1.0);
}