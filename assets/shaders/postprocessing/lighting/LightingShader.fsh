// LightingShader.fsh
#version 330 core
in  vec2 v_texCoord;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/GBufferData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "postprocessing/includes/ViewPosReconstruct.glsl"
#include "postprocessing/lighting/includes/AtmosphericFog.glsl"

// FOG_SHADOW_SCALE / FOG_LIT_SCALE re-weight computeFogAmount()'s result by
// how directly lit the fragment is, since fog should read stronger on
// sunlit distant terrain and weaker in shadow — not the reverse.
const float FOG_SHADOW_SCALE = 0.35;
const float FOG_LIT_SCALE    = 1.6;

void main() {
    float depth = texture(u_gDepth, v_texCoord).r;
    if (depth >= 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }

    vec3 albedo = texture(u_gAlbedo, v_texCoord).rgb;

    vec3 normalView = normalize(texture(u_gNormal, v_texCoord).rgb);
    vec3 normal     = normalize(mat3(u_inverseView) * normalView);

    vec4  material      = texture(u_gMaterial, v_texCoord);
    float sunVisibility = material.r;
    float specular      = material.g;
    float vertAO        = material.b;

    float ssaoAO = texture(u_ssaoTex, v_texCoord).r;
    float ao     = vertAO * ssaoAO;

    vec3 specColor = vec3(0.04);

    vec3 fragPosView  = reconstructViewPos(v_texCoord);
    vec3 fragPosWorld = (u_inverseView * vec4(fragPosView, 1.0)).xyz;
    vec3 viewDir      = normalize(-fragPosView);

    float skyFacing  = dot(normal, vec3(0.0, 1.0, 0.0));
    float skyFactor  = skyFacing * 0.5 + 0.5;
    float skyMax     = mix(0.12, 0.62, skyFactor);
    float dayFactor  = clamp(u_sunIntensity, 0.0, 1.0);
    float skyAmbient = skyMax * mix(0.05, 1.0, dayFactor) * ao;

    vec3  sunDirWorld = normalize(u_sunDirection);
    vec3  sunDirView  = normalize(mat3(u_view) * sunDirWorld);
    float sunDiff     = max(dot(normalView, sunDirView), 0.0);
    vec3  sunHalf     = normalize(viewDir + sunDirView);
    float specPower   = mix(4.0, 256.0, specular);
    float sunSpec     = pow(max(dot(normalView, sunHalf), 0.0), specPower) * specular;

    vec3 sunContrib = u_sunColor * u_sunIntensity * sunVisibility
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

    float litAmount = clamp(sunDiff + moonDiff * 0.5, 0.0, 1.0);
    float fogT      = computeFogAmount(fragPosWorld);
    float fogBlend  = clamp(fogT * mix(FOG_SHADOW_SCALE, FOG_LIT_SCALE, litAmount), 0.0, 1.0);

    lit = mix(lit, u_skyFogColor, fogBlend);

    fragColor = vec4(lit, 1.0);
}