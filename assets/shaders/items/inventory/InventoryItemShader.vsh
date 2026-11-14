#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in float aTile;

// u_model carries the item into window pixels, y up and depth toward the
// viewer; u_projection maps those pixels to clip space.
uniform mat4 u_projection;
uniform mat4 u_model;

flat out int vTile;
out vec3 vModelPosition;
out vec3 vPosition;
flat out int vFace;

void main() {
    vec4 position = u_model * vec4(aPos, 1.0);

    vTile          = int(aTile + 0.5);
    vModelPosition = aPos;
    vPosition      = position.xyz;
    vFace          = int(aNorIndex);
    gl_Position = u_projection * position;
}
