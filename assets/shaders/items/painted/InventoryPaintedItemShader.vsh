#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in vec3  aNormal;
layout(location = 2) in vec2  aUV;
layout(location = 3) in float aTile;

// u_model carries the item into window pixels, y up and depth toward the
// viewer; u_projection maps those pixels to clip space.
uniform mat4 u_projection;
uniform mat4 u_model;

out vec2 vUV;
flat out int vTile;
out vec3 vPosition;

void main() {
    vec4 position = u_model * vec4(aPos, 1.0);

    vUV         = aUV;
    vTile       = int(aTile + 0.5);
    vPosition   = position.xyz;
    gl_Position = u_projection * position;
}
