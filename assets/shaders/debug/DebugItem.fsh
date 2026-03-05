#version 330 core

in vec3       vLocalPos;
in vec3       vNormal;
flat in vec2  vUVOrigin;
flat in float vOrient;

#include "items/includes/ItemsStandard.glsl"
#include "includes/BlockOrientationMapData.glsl"
#include "surface/includes/TiledSampling.glsl"

uniform sampler2DArray u_textureArray;

vec4 sampleLayerTiled(vec2 tiledUV, int layer) {
    return texture(u_textureArray, vec3(tiledUV, float(layer)));
}

out vec4 FragColor;

void main() {
    vec2 tiledUV = tileUV(vLocalPos, vUVOrigin, vNormal, vOrient);
    vec4 albedo  = sampleLayerTiled(tiledUV, u_layer_albedo);

    if (albedo.a < 0.01)
    discard;

    FragColor = albedo;
}