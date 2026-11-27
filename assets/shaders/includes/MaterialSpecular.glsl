#ifndef MATERIAL_SPECULAR_GLSL
#define MATERIAL_SPECULAR_GLSL

// The highlight a surface stores from its maps, the one rule characters and items share: a dielectric's own specular
// scaled by its roughness about the neutral roughness the maps default to, and a metal's highlight, as sharp as the
// metal is smooth, taking its place by its metalness.

const float MATERIAL_ROUGHNESS_NEUTRAL = 0.5;

float resolveMaterialSpecular(float baseSpecular, float metallic, float roughness) {
    float dielectric = baseSpecular * (1.0 - roughness) / (1.0 - MATERIAL_ROUGHNESS_NEUTRAL);

    return mix(dielectric, 1.0 - roughness, metallic);
}

#endif
