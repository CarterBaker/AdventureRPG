#ifndef SKY_REFLECTION_GLSL
#define SKY_REFLECTION_GLSL

#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"

// Glossy characters — wet eyes, metal — mirror the sky: the environment is the sky the sky shader paints, from the
// horizon band up to the zenith with the sun-side glow, dimmed below the horizon where the ground would be. How much
// a surface mirrors comes from the specular it stored past REFLECTION_GLOSS_START, weighted by Fresnel and held back by
// its occlusion. Characters write that specular falling with the Character Detail distance, so their reflections fade
// with it.

const float REFLECTION_GLOSS_START = 0.5;
const float REFLECTION_FRESNEL_BASE = 0.04;
const float REFLECTION_GROUND_SHADE = 0.3;
const float REFLECTION_GLOW_POWER = 8.0;

vec3 resolveSkyEnvironment(vec3 direction) {
    float up   = direction.y;
    vec3  sky  = mix(u_skyHorizonColor, u_skyZenithColor, smoothstep(0.0, 0.6, up));
    float glow = pow(max(dot(direction, normalize(u_sunDirection)), 0.0), REFLECTION_GLOW_POWER) * u_skyBlend.x;

    sky += u_skyGlowColor * glow;

    return up >= 0.0 ? sky : u_skyHorizonColor * mix(1.0, REFLECTION_GROUND_SHADE, smoothstep(0.0, 0.3, -up));
}

vec3 resolveSkyReflection(vec3 normal, vec3 toEye, float specular, float occlusion) {
    float gloss = smoothstep(REFLECTION_GLOSS_START, 1.0, specular);

    if (gloss <= 0.0)
    return vec3(0.0);

    float facing  = clamp(dot(normal, toEye), 0.0, 1.0);
    float fresnel = REFLECTION_FRESNEL_BASE + (1.0 - REFLECTION_FRESNEL_BASE) * pow(1.0 - facing, 5.0);

    return resolveSkyEnvironment(reflect(-toEye, normal)) * gloss * mix(0.25, 1.0, fresnel) * occlusion;
}

#endif
