#version 330 core

in vec2 vUV;
in vec3 vPosition;

#include "items/includes/ItemsStandard.glsl"

uniform sampler2DArray u_textureArray;
uniform vec3  u_lightDirection;
uniform float u_ambient;
uniform vec4  u_tint;

out vec4 FragColor;

/*
 * Items in the inventory are lit by one fixed light in window space. The
 * facet normal comes from screen-space derivatives, since the transform
 * scales items freely. u_tint blends the lit colour toward a tint colour by
 * its alpha — greyed for hidden gear, green or red for a drop preview.
 */
void main() {
    vec4 albedo = texture(u_textureArray, vec3(vUV, float(u_layer_albedo)));

    if (albedo.a < 0.01)
    discard;

    vec3  normal   = normalize(cross(dFdx(vPosition), dFdy(vPosition)));
    float diffuse  = max(dot(normal, normalize(-u_lightDirection)), 0.0);
    float lighting = u_ambient + (1.0 - u_ambient) * diffuse;
    vec3  lit      = albedo.rgb * lighting;

    FragColor = vec4(mix(lit, u_tint.rgb * lighting, u_tint.a), 1.0);
}
