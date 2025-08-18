#version 450 core

layout(location = 0) in vec3 v_normal;
layout(location = 1) in vec3 v_fragPos;
layout(location = 2) in vec2 v_texCoord;

layout(location = 0) out vec4 fragColor;

// Uniforms
layout(set = 0, binding = 1) uniform sampler2D u_texture;
layout(set = 0, binding = 2) uniform Light {
    vec3 u_lightDir;
    vec3 u_lightColor;
    vec3 u_ambientColor;
};

void main() {
    vec3 normal = normalize(v_normal);

    float diff = max(dot(normal, normalize(-u_lightDir)), 0.0);
    vec3 diffuse = diff * u_lightColor;

    vec3 albedo = texture(u_texture, v_texCoord).rgb;
    vec3 color = albedo * (diffuse + u_ambientColor);

    fragColor = vec4(color, 1.0);
}
