#version 330 core

in vec3 vWorldNormal;
in vec2 vTexCoord;
in vec4 vBiomeColor;

#include "includes/StandardTextureLayoutData.glsl"
#include "includes/DirectionalLightData.glsl"
#include "surface/includes/Albedo.glsl"

out vec4 FragColor;

const float AMBIENT = 0.08;

void main() {
    vec3 normal = normalize(vWorldNormal);
    vec4 albedo = sampleLayer(vTexCoord, u_layer_albedo);

    if (albedo.a < 0.01)
    discard;

    float lightDot = max(dot(normal, -u_lightDirection), 0.0);
    vec3  light    = u_lightColor * u_lightIntensity * lightDot + vec3(AMBIENT);

    FragColor = vec4(albedo.rgb * vBiomeColor.rgb * light, albedo.a);
}