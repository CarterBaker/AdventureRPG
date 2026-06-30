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
#include "surface/includes/SurfaceStandard.glsl"
#include "includes/BlockOrientationMapData.glsl"
#include "surface/includes/TiledSampling.glsl"
#include "surface/includes/Albedo.glsl"
#include "surface/includes/Normal.glsl"
#include "surface/includes/AO.glsl"
#include "surface/includes/Specular.glsl"
#include "surface/includes/SurfaceTierFull.glsl"
#include "surface/includes/SurfaceTierMid.glsl"
#include "surface/includes/SurfaceTierFlat.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

// Pushed per draw call alongside u_gridPosition / u_distanceFromCenter.
// True only for the cheap hole-filling underlay pass drawn beneath the
// full Detail pass on the innermost ring — see StandardSurface.tes for
// what it skips on the geometry side.
uniform bool u_forceFlatUnderlay;

// Tier thresholds in the same linearDist (sqrt-scaled distance fraction)
// space used for fog below. Tune against actual chunk size / render
// distance in-game; prefer comparing against a discrete chunk-ring index
// from GridSlotUBO instead if one is exposed, since that lines up with
// "3x3 chunks" exactly instead of approximately.
const float TIER_MID_START = 0.20;
const float TIER_FAR_START = 0.55;

void main() {
    vec2 tiledUV = tileUV(vUVLocalPos, vUVOrigin, vNormal, vOrient);

    float halfD      = u_renderDistance * 0.5 - 0.5;
    float trueMax    = halfD * halfD * 2.0;
    float distFrac   = clamp(u_distanceFromCenter / trueMax, 0.0, 1.0);
    float linearDist = sqrt(distFrac);

    vec3  albedo;
    vec3  normalView;
    float specular;
    float ao;
    float fogT = 0.0; // only the far-tier branch below overrides this
    bool  visible;

    if (u_forceFlatUnderlay) {
        visible = shadeSurfaceFlat(tiledUV, vNormal, u_view, albedo, normalView, specular, ao);
    }
    else if (linearDist < TIER_MID_START) {
        visible = shadeSurfaceFull(tiledUV, vNormal, u_view, albedo, normalView, specular, ao);
    }
    else if (linearDist < TIER_FAR_START) {
        visible = shadeSurfaceMid(tiledUV, vNormal, u_view, albedo, normalView, specular, ao);
    }
    else {
        visible = shadeSurfaceFlat(tiledUV, vNormal, u_view, albedo, normalView, specular, ao);
        fogT = smoothstep(0.0, 0.5, linearDist) * 0.25
        + smoothstep(0.5, 1.0, linearDist) * 0.15;
    }

    if (!visible)
    discard;

    // CRITICAL: this pass runs with blending ENABLED (GL_SRC_ALPHA,
    // GL_ONE_MINUS_SRC_ALPHA) — see RenderSystem.drawToMappedTargets.
    // Every output's alpha channel controls whether the write happens at
    // all: result = dst*(1-srcA) + src*srcA. Any output with alpha < 1.0
    // gets silently dropped and the buffer keeps its previous-frame-clear
    // value of (0,0,0,0). All three render targets MUST output alpha = 1.0
    // here, full stop. This is also what makes the underlay trick work:
    // the Detail pass drawn second has alpha=1 everywhere it doesn't
    // discard, so it fully overwrites the underlay there — the underlay
    // only survives in pixels where Detail discarded or never covered.
    //
    // gMaterial: r = fogT, g = specular, b = ao, a = 1.0 (forced, no data).
    gAlbedo   = vec4(albedo, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(fogT, specular, ao, 1.0);
}