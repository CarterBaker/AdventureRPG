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

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

void main() {
    vec2 tiledUV = tileUV(vUVLocalPos, vUVOrigin, vNormal, vOrient);
    vec4 albedo  = sampleLayerTiled(tiledUV, u_layer_albedo);

    if (albedo.a < 0.01)
    discard;

    float halfD      = u_renderDistance * 0.5 - 0.5;
    float trueMax    = halfD * halfD * 2.0;
    float fogDist    = clamp(u_distanceFromCenter / trueMax, 0.0, 1.0);
    float linearDist = sqrt(fogDist);
    float fogT        = smoothstep(0.0, 0.5, linearDist) * 0.25
    + smoothstep(0.5, 1.0, linearDist) * 0.15;

    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal = vec4(normalize(mat3(u_view) * vNormal), 1.0);
    gMaterial = vec4(fogT, 0.0, 1.0, 1.0);
}