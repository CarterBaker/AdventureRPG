#version 330 core

in vec3 vNormal;
in vec3 vPosition;
flat in int vTile;
flat in int vFace;

#include "includes/CameraData.glsl"
#include "items/includes/BlockAlbedo.glsl"

uniform vec3  u_lightDirection;
uniform float u_ambient;

out vec4 FragColor;

// A wall is one quad seen from both sides, so its normal turns toward the camera. Faces merge across block
// boundaries, so the tile repeats once per block of the model grid.
void main() {
    vec4 albedo = sampleBlockAlbedo(vTile, vPosition, vFace);

    if (albedo.a < 0.01)
    discard;

    vec3 normal = normalize(vNormal);

    if (dot(normal, u_cameraPosition - vPosition) < 0.0)
    normal = -normal;

    float diffuse  = max(dot(normal, normalize(-u_lightDirection)), 0.0);
    float lighting = u_ambient + (1.0 - u_ambient) * diffuse;

    FragColor = vec4(albedo.rgb * lighting, albedo.a);
}
