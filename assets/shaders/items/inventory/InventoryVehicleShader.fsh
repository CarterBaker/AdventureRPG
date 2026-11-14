#version 330 core

flat in int vTile;
in vec3 vModelPosition;
in vec3 vPosition;
flat in int vFace;

#include "items/includes/BlockAlbedo.glsl"

uniform vec3  u_lightDirection;
uniform float u_ambient;
uniform vec4  u_tint;

out vec4 FragColor;

/*
 * A vehicle drawn as an icon, lit exactly as an inventory item is: one fixed
 * light in window space with the facet normal from screen-space derivatives.
 * Its faces are merged across block boundaries, so its tile repeats once
 * per block of the model grid, read from the model position exactly as the
 * vehicle's world shader reads it. u_tint blends the lit colour toward a tint
 * colour by its alpha.
 */
void main() {
    vec4 albedo = sampleBlockAlbedo(vTile, vModelPosition, vFace);

    if (albedo.a < 0.01)
    discard;

    vec3  normal   = normalize(cross(dFdx(vPosition), dFdy(vPosition)));
    float diffuse  = max(dot(normal, normalize(-u_lightDirection)), 0.0);
    float lighting = u_ambient + (1.0 - u_ambient) * diffuse;
    vec3  lit      = albedo.rgb * lighting;

    FragColor = vec4(mix(lit, u_tint.rgb * lighting, u_tint.a), 1.0);
}
