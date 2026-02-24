#version 330 core

in vec3       vLocalPos;
in vec3       vNormal;
flat in vec2  vUVOrigin;
flat in float vOrient;

#include "surface/includes/StandardTextureLayoutData.glsl"
#include "includes/BlockOrientationMapData.glsl"
#include "includes/DirectionalLightData.glsl"
#include "surface/includes/TiledSampling.glsl"
#include "surface/includes/Albedo.glsl"

out vec4 FragColor;

void main() {
    vec2 tiledUV = tileUV(vLocalPos, vUVOrigin, vNormal, vOrient);
    vec4 albedo  = sampleLayerTiled(tiledUV, u_layer_albedo);

    if (albedo.a < 0.01)
    discard;

    float diff    = max(dot(normalize(vNormal), normalize(-u_lightDirection)), 0.0);
    float ambient = 0.15;
    vec3  lighting = u_lightColor * u_lightIntensity * (ambient + diff * (1.0 - ambient));

    FragColor = vec4(albedo.rgb * lighting, albedo.a);
}