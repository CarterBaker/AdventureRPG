#version 400 core

in vec3       vLocalPos;
in vec3       vUVLocalPos;
in vec3       vNormal;
flat in vec2  vUVOrigin;
flat in float vOrient;

#include "surface/includes/SurfaceStandard.glsl"
#include "includes/BlockOrientationMapData.glsl"
#include "includes/DirectionalLightData.glsl"
#include "surface/includes/AtmosphericFog.glsl"
#include "surface/includes/TiledSampling.glsl"
#include "surface/includes/Albedo.glsl"

out vec4 FragColor;

void main() {
    // UV is computed from the UNDISPLACED position, so the texture stays
    // anchored to its original tile in the atlas and doesn't bleed into
    // neighboring tiles (e.g. dirt showing through on a grass top face)
    // when the geometry itself is displaced.
    vec2 tiledUV = tileUV(vUVLocalPos, vUVOrigin, vNormal, vOrient);
    vec4 albedo  = sampleLayerTiled(tiledUV, u_layer_albedo);

    if (albedo.a < 0.01)
    discard;

    float diff     = max(dot(normalize(vNormal), normalize(-u_lightDirection)), 0.0);
    float ambient  = 0.15;
    vec3  lighting = u_lightColor * u_lightIntensity * (ambient + diff * (1.0 - ambient));
    vec3  lit      = albedo.rgb * lighting;

    lit = applyAtmosphericFog(lit);

    FragColor = vec4(lit, albedo.a);
}