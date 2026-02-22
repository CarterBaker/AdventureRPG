#version 330 core

in vec3 vWorldNormal;
in vec2 vTexCoord;
in vec4 vBiomeColor;

#include "includes/StandardTextureLayoutData.glsl"
#include "includes/NaturalLightData.glsl"
#include "surface/includes/Albedo.glsl"

out vec4 FragColor;

const float AMBIENT = 0.08;

void main() {
    vec3 normal = normalize(vWorldNormal);
    vec4 albedo = sampleLayer(vTexCoord, u_layer_albedo);

    if (albedo.a < 0.01)
    discard;

    float sunDot  = max(dot(normal, -u_sunDirection),  0.0);
    float moonDot = max(dot(normal, -u_moonDirection), 0.0);

    vec3 light = (u_sunColor  * u_sunIntensity  * sunDot)
    + (u_moonColor * u_moonIntensity * moonDot)
    + vec3(AMBIENT);

    FragColor = vec4(albedo.rgb * vBiomeColor.rgb * light, albedo.a);
}