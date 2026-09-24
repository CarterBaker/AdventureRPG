#version 330 core

in vec3 vNormal;
in vec2 vUV;

#include "items/includes/ItemsStandard.glsl"

uniform sampler2DArray u_textureArray;
uniform vec3  u_lightDirection;
uniform float u_ambient;

out vec4 FragColor;

void main() {
    vec4 albedo = texture(u_textureArray, vec3(vUV, float(u_layer_albedo)));

    if (albedo.a < 0.01)
    discard;

    float diffuse  = max(dot(normalize(vNormal), normalize(-u_lightDirection)), 0.0);
    float lighting = u_ambient + (1.0 - u_ambient) * diffuse;

    FragColor = vec4(albedo.rgb * lighting, albedo.a);
}
