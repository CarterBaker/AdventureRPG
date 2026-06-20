#version 330 core

in vec3 vNormal;
in vec2 vUV;

#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "items/includes/ItemsStandard.glsl"

uniform sampler2DArray u_textureArray;

out vec4 FragColor;

void main() {
    vec4 albedo = texture(u_textureArray, vec3(vUV, float(u_layer_albedo)));

    if (albedo.a < 0.01)
    discard;

    vec3 normal = normalize(vNormal);

    float sunDiff  = max(dot(normal, normalize(-u_sunDirection)),  0.0);
    float moonDiff = max(dot(normal, normalize(-u_moonDirection)), 0.0);

    vec3 sunLight  = u_sunColor  * u_sunIntensity  * sunDiff;
    vec3 moonLight = u_moonColor * u_moonIntensity * moonDiff;

    float ambient  = 0.15;
    vec3  lighting = ambient + sunLight + moonLight;

    FragColor = vec4(albedo.rgb * lighting, albedo.a);
}