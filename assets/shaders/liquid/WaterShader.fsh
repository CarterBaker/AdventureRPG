#version 330 core

in vec3  vWorldNormal;
in vec3  vOceanPos;
in float vOceanSurface;
in float vTurbulence;
out vec4 FragColor;

#include "liquid/includes/OceanSurface.glsl"

const vec3  WATER_COLOR = vec3(0.35, 0.6, 0.9);
const float WATER_ALPHA = 0.55;

// Open ocean darkens toward deep water as the sea roughens, and breaks into
// foam on the crests of a storm swell. It is shaded against a tilted light so
// the slope of every wave and ripple reads, where still water keeps the flat
// top-down shade it always had.
const vec3  OCEAN_ROUGH_COLOR           = vec3(0.14, 0.34, 0.56);
const vec3  OCEAN_FOAM_COLOR            = vec3(0.92, 0.95, 0.97);
const float OCEAN_FOAM_ALPHA            = 0.85;
const float OCEAN_ROUGH_TURBULENCE_FULL = 3.0;
const vec3  OCEAN_SHADE_DIRECTION       = vec3(0.45, 0.8, 0.35);

void main() {
    vec3  normal = normalize(vWorldNormal);
    vec3  color  = WATER_COLOR;
    float alpha  = WATER_ALPHA;
    float shade  = clamp(0.6 + normal.y * 0.4, 0.6, 1.0);

    if (vOceanSurface > 0.5 && normal.y > 0.5) {
        vec3  oceanNormal = computeOceanNormal(vOceanPos.xz, vTurbulence);
        float roughness   = clamp(vTurbulence / OCEAN_ROUGH_TURBULENCE_FULL, 0.0, 1.0);
        float foam        = computeOceanFoam(vOceanPos.xz, vTurbulence);

        color = mix(WATER_COLOR, OCEAN_ROUGH_COLOR, roughness);
        color = mix(color, OCEAN_FOAM_COLOR, foam);
        alpha = mix(WATER_ALPHA, OCEAN_FOAM_ALPHA, foam);
        shade = clamp(0.6 + dot(oceanNormal, normalize(OCEAN_SHADE_DIRECTION)) * 0.4, 0.5, 1.0);
    }

    FragColor = vec4(color * shade, alpha);
}
