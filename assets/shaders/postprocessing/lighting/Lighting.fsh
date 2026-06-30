#version 330 core
in  vec2 v_texCoord;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/GBufferData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "postprocessing/includes/ViewPosReconstruct.glsl"

void main() {
    float depth = texture(u_gDepth, v_texCoord).r;
    if (depth >= 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }

    vec3 albedo = texture(u_gAlbedo, v_texCoord).rgb;

    vec3 normalView = normalize(texture(u_gNormal, v_texCoord).rgb);
    vec3 normal     = normalize(mat3(u_inverseView) * normalView);

    vec4  material = texture(u_gMaterial, v_texCoord);
    float fogT     = material.r;
    float specular = material.g;
    float vertAO   = material.b;

    float ssaoAO = texture(u_ssaoTex, v_texCoord).r;
    float ao     = vertAO * ssaoAO;

    // Fixed dielectric F0 — no metallic channel right now.
    vec3 specColor = vec3(0.04);

    vec3 fragPosView = reconstructViewPos(v_texCoord);
    vec3 viewDir      = normalize(-fragPosView);

    float skyFacing  = dot(normal, vec3(0.0, 1.0, 0.0));
    float skyFactor  = skyFacing * 0.5 + 0.5;
    float skyMax     = mix(0.12, 0.62, skyFactor);
    float dayFactor  = clamp(u_sunIntensity, 0.0, 1.0);
    float skyAmbient = skyMax * mix(0.05, 1.0, dayFactor) * ao;

    vec3  sunDirView = normalize(mat3(u_view) * u_sunDirection);
    float sunDiff    = max(dot(normalView, sunDirView), 0.0);
    vec3  sunHalf    = normalize(viewDir + sunDirView);
    float specPower  = mix(4.0, 256.0, specular);
    float sunSpec    = pow(max(dot(normalView, sunHalf), 0.0), specPower) * specular;

    vec3 sunContrib = u_sunColor * u_sunIntensity
    * (albedo * sunDiff + specColor * sunSpec);

    vec3  moonDirView = normalize(mat3(u_view) * u_moonDirection);
    float moonDiff    = max(dot(normalView, moonDirView), 0.0);
    float moonInt     = min(u_moonIntensity, 0.18);
    vec3  moonTint    = vec3(0.58, 0.74, 1.00);
    vec3  moonHalf    = normalize(viewDir + moonDirView);
    float moonSpec    = pow(max(dot(normalView, moonHalf), 0.0), specPower) * specular * 0.25;

    vec3 moonContrib = u_moonColor * moonTint * moonInt
    * (albedo * moonDiff + specColor * moonSpec);

    vec3 lit = albedo * skyAmbient + sunContrib + moonContrib;

    vec3 fogColor = u_skyHorizonColor + 0.04;
    lit = mix(lit, fogColor, fogT);

    fragColor = vec4(lit, 1.0);
}