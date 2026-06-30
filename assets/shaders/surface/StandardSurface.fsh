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

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

void main() {
    vec2 tiledUV = tileUV(vUVLocalPos, vUVOrigin, vNormal, vOrient);

    vec4 albedo = sampleLayerTiled(tiledUV, u_layer_albedo);
    if (albedo.a < 0.01)
    discard;

    vec3 outNormal = sampleNormalViewSpace(tiledUV, u_layer_normal, vNormal, u_view);

    float ao       = sampleAO      (tiledUV, u_layer_ao);
    float specular = sampleSpecular(tiledUV, u_layer_specular);

    float halfD      = u_renderDistance * 0.5 - 0.5;
    float trueMax    = halfD * halfD * 2.0;
    float fogDist    = clamp(u_distanceFromCenter / trueMax, 0.0, 1.0);
    float linearDist = sqrt(fogDist);
    float fogT       = smoothstep(0.0, 0.5, linearDist) * 0.25
    + smoothstep(0.5, 1.0, linearDist) * 0.15;

    // CRITICAL: this pass runs with blending ENABLED (GL_SRC_ALPHA,
    // GL_ONE_MINUS_SRC_ALPHA) — see RenderSystem.drawToMappedTargets.
    // Every output's alpha channel controls whether the write happens at
    // all: result = dst*(1-srcA) + src*srcA. Any output with alpha < 1.0
    // gets silently dropped and the buffer keeps its previous-frame-clear
    // value of (0,0,0,0). All three render targets MUST output alpha = 1.0
    // here, full stop. Do not pack real per-fragment data into alpha on
    // ANY of these three outputs unless this pass's blend mode changes —
    // metallic lived here before and that's exactly what broke fog and AO.
    //
    // gMaterial: r = fogT, g = specular, b = ao, a = 1.0 (forced, no data).
    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal   = vec4(outNormal, 1.0);
    gMaterial = vec4(fogT, specular, ao, 1.0);
}