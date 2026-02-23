#version 330 core
in vec2 vTexCoord;
#include "includes/StandardTextureLayoutData.glsl"
#include "includes/DirectionalLightData.glsl"
#include "surface/includes/Albedo.glsl"
out vec4 FragColor;
void main() {
    vec4 albedo = sampleLayer(vTexCoord, u_layer_albedo);
    if (albedo.a < 0.01)
    discard;
    FragColor = albedo;
}