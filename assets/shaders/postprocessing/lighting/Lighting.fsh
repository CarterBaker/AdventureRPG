#version 330 core

in  vec2 v_texCoord;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/GBufferData.glsl"
#include "includes/DirectionalLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "postprocessing/includes/ViewPosReconstruct.glsl"

void main() {
    float depth = texture(u_gDepth, v_texCoord).r;

    if (depth >= 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }

    vec3  albedo   = texture(u_gAlbedo,   v_texCoord).rgb;
    vec3  normal   = normalize(texture(u_gNormal, v_texCoord).rgb);
    vec4  material = texture(u_gMaterial, v_texCoord);
    float vertAO   = material.b;
    float fogT     = material.a;
    float ssaoAO   = texture(u_ssaoTex,   v_texCoord).r;
    float ao       = vertAO * ssaoAO;

    float diff    = max(dot(normal, normalize(-u_lightDirection)), 0.0);
    float ambient = 0.15 * ao;
    vec3  lit     = albedo * u_lightColor * u_lightIntensity * (ambient + diff * (1.0 - ambient));

    vec3 fogColor = u_skyHorizonColor + 0.04;
    lit = mix(lit, fogColor, fogT);

    fragColor = vec4(lit, 1.0);
}