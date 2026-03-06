#version 330 core

in vec3 vNormal;
in vec2 vUV;

uniform sampler2DArray u_textureArray;
uniform int u_layer_albedo;

out vec4 FragColor;

void main() {
    vec4 albedo = texture(u_textureArray, vec3(vUV, float(u_layer_albedo)));
    if (albedo.a < 0.01) discard;
    FragColor = albedo;
}