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

    vec3  albedo     = texture(u_gAlbedo,   v_texCoord).rgb;
    vec3  normalView = normalize(texture(u_gNormal, v_texCoord).rgb);
    vec3  normal     = normalize(mat3(u_inverseView) * normalView);
    vec4  material   = texture(u_gMaterial, v_texCoord);
    float fogT       = material.r;
    float vertAO     = material.b;
    float ssaoAO     = texture(u_ssaoTex, v_texCoord).r;
    float ao         = vertAO * ssaoAO;

    float skyFacing  = dot(normal, vec3(0.0, 1.0, 0.0));
    float skyFactor  = skyFacing * 0.5 + 0.5;
    float skyMax     = mix(0.12, 0.62, skyFactor);
    float dayFactor  = clamp(u_sunIntensity, 0.0, 1.0);
    float skyAmbient = skyMax * mix(0.05, 1.0, dayFactor) * ao;

    float sunDiff  = max(dot(normal, normalize(u_sunDirection)), 0.0);
    vec3  sunLight = u_sunColor * u_sunIntensity * sunDiff;

    float moonDiff  = max(dot(normal, normalize(u_moonDirection)), 0.0);
    float moonInt   = min(u_moonIntensity, 0.18);
    vec3  moonTint  = vec3(0.58, 0.74, 1.00);
    vec3  moonLight = u_moonColor * moonTint * moonInt * moonDiff;

    vec3 lit = albedo * (skyAmbient + sunLight + moonLight);

    vec3 fogColor = u_skyHorizonColor + 0.04;
    lit = mix(lit, fogColor, fogT);

    fragColor = vec4(lit, 1.0);
}