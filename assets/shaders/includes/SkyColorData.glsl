#ifndef SKY_COLOR_DATA_GLSL
#define SKY_COLOR_DATA_GLSL

layout(std140) uniform SkyColorData {
    vec3  u_skyZenithColor;       // top of the dome
    vec3  u_skyHorizonColor;      // horizon band
    vec3  u_skyGlowColor;         // sun-side twilight glow
    vec3  u_skyBeltColor;         // anti-solar twilight belt
    vec3  u_skyCloudColor;        // cloud albedo tint
    vec3  u_skyCloudLightColor;   // tint on sunlight reaching clouds
    vec3  u_skyCloudShadowColor;  // ambient tint in cloud shade
    vec3  u_skyFogColor;          // distance fog
    vec4  u_skyBlend;             // x glow strength, y belt strength, z daylight, w overcast
};

#endif
