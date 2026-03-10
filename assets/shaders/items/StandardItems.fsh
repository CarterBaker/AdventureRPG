#version 330 core
in vec3 vNormal;
in vec2 vUV;

#include "includes/DirectionalLightData.glsl"
#include "items/includes/ItemsStandard.glsl"

uniform sampler2DArray u_textureArray;

out vec4 FragColor;

void main() {
    vec4 albedo = texture(u_textureArray, vec3(vUV, float(u_layer_albedo)));
    if (albedo.a < 0.01) discard;

    float diff    = max(dot(normalize(vNormal), normalize(-u_lightDirection)), 0.0);
    float ambient = 0.15;
    vec3 lighting = u_lightColor * u_lightIntensity * (ambient + diff * (1.0 - ambient));

    FragColor = vec4(albedo.rgb * lighting, albedo.a);
}