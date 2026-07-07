#version 330 core
in  vec2 v_texCoord;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/GBufferData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "postprocessing/includes/ViewPosReconstruct.glsl"

// fogT (material.r) arrives pre-computed from StandardSurface.fsh via
// computeFogAmount() in surface/includes/AtmosphericFog.glsl — that's the
// single source of truth for the distance curve. It can't be recomputed
// here: u_distanceFromCenter is only valid during a chunk's own draw call,
// and this is a single deferred fullscreen pass that runs after every
// chunk has already drawn, so that UBO would just hold stale leftover data.
//
// FOG_SHADOW_SCALE / FOG_LIT_SCALE re-weight the incoming fogT by how
// directly lit the fragment is, since fog should read stronger on sunlit
// distant terrain and weaker in shadow — not the reverse.
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

    vec4  material = texture(u_gMaterial, v_texCoord);
    float fogT     = material.r;
    float specular = material.g;
    float vertAO   = material.b;

    float ssaoAO = texture(u_ssaoTex, v_texCoord).r;
    float ao     = vertAO * ssaoAO;

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

    float litAmount = clamp(sunDiff + moonDiff * 0.5, 0.0, 1.0);
    float fogBlend  = clamp(fogT * mix(FOG_SHADOW_SCALE, FOG_LIT_SCALE, litAmount), 0.0, 1.0);

    vec3 fogColor = u_skyHorizonColor + 0.04;
    lit = mix(lit, fogColor, fogBlend);

    fragColor = vec4(lit, 1.0);
}