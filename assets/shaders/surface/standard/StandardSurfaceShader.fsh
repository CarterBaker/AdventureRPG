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
#include "surface/includes/AtmosphericFog.glsl"
#include "surface/includes/SurfaceTierFull.glsl"
#include "surface/includes/SurfaceTierMid.glsl"
#include "surface/includes/SurfaceTierFlat.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

// ── Tier selection ──────────────────────────────────────────────────────
// u_distanceFromCenter is the SQUARED chunk-grid distance of this chunk's
// grid slot from the center grid slot — proof: the TES already computes
// maxDist = halfD*halfD*2.0 (max squared diagonal distance across a square
// grid of side u_renderDistance), and normalizes u_distanceFromCenter
// against it. That only holds if u_distanceFromCenter is itself raw
// squared chunk distance, not a pre-normalized fraction. Tier selection
// compares directly against it — exact integer-grid math.
//
// For an integer chunk offset (dx, dy) from center, squared distance is
// dx*dx + dy*dy:
//   3x3 ring   (max(|dx|,|dy|) <= 1): values {0, 1, 2}  -> center + 8 neighbors
//   next ring  (max(|dx|,|dy|) == 2): values {4, 5, 8}  -> starts at 4
// This stays exact through ring 2 (5x5). It is NOT exact for larger rings —
// squared-distance circles and chebyshev squares diverge past r=2 — so if
// you widen TIER1_MAX_SQ_DIST to cover a 7x7+ area, switch to real
// chebyshev math using grid-slot integer coordinates instead.
const float TIER0_MAX_SQ_DIST = 2.5;  // 3x3 ring (center + 8 neighbors)
const float TIER1_MAX_SQ_DIST = 8.5;  // adds the next ring out (5x5)
// Beyond TIER1_MAX_SQ_DIST = Far tier.

void main() {
    vec2 tiledUV = tileUV(vUVLocalPos, vUVOrigin, vNormal, vOrient);

    vec3  albedo;
    vec3  normalView;
    float specular;
    float ao;
    float fogT = 0.0; // only the Far tier branch below overrides this
    bool  visible;

    if (u_distanceFromCenter <= TIER0_MAX_SQ_DIST) {
        visible = shadeSurfaceFull(tiledUV, vNormal, u_view, albedo, normalView, specular, ao);
    }
    else if (u_distanceFromCenter <= TIER1_MAX_SQ_DIST) {
        visible = shadeSurfaceMid(tiledUV, vNormal, u_view, albedo, normalView, specular, ao);
    }
    else {
        visible = shadeSurfaceFlat(tiledUV, vNormal, u_view, albedo, normalView, specular, ao);
        fogT = computeFogAmount(); // shared curve — see AtmosphericFog.glsl
    }

    if (!visible)
    discard;

    // CRITICAL: this pass runs with blending ENABLED (GL_SRC_ALPHA,
    // GL_ONE_MINUS_SRC_ALPHA) — see RenderSystem.drawToMappedTargets.
    // Every output's alpha channel controls whether the write happens at
    // all: result = dst*(1-srcA) + src*srcA. Any output with alpha < 1.0
    // gets silently dropped and the buffer keeps its previous-frame-clear
    // value of (0,0,0,0). All three render targets MUST output alpha = 1.0
    // here, full stop. Do not pack real per-fragment data into alpha on
    // ANY of these three outputs unless this pass's blend mode changes.
    //
    // gMaterial: r = fogT, g = specular, b = ao, a = 1.0 (forced, no data).
    gAlbedo   = vec4(albedo, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(fogT, specular, ao, 1.0);
}