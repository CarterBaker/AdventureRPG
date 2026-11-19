#version 330 core

in vec2 vUV;
flat in int vTile;
in vec3 vPosition;

#include "items/includes/PaintedAlbedo.glsl"

uniform vec3  u_lightDirection;
uniform float u_ambient;
uniform vec4  u_tint;

out vec4 FragColor;

/*
 * Painted items in the inventory are lit by the same fixed light in window
 * space as sub-voxel ones, the facet normal from screen-space derivatives,
 * and u_tint blends the lit colour toward a tint colour by its alpha.
 */
void main() {
    vec4 albedo = samplePaintedAlbedo(vTile, vUV);

    if (albedo.a < 0.01)
    discard;

    vec3  normal   = normalize(cross(dFdx(vPosition), dFdy(vPosition)));
    float diffuse  = max(dot(normal, normalize(-u_lightDirection)), 0.0);
    float lighting = u_ambient + (1.0 - u_ambient) * diffuse;
    vec3  lit      = albedo.rgb * lighting;

    FragColor = vec4(mix(lit, u_tint.rgb * lighting, u_tint.a), 1.0);
}
