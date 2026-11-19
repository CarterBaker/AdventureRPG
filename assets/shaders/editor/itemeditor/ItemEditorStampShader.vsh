#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;

#include "includes/CameraData.glsl"

uniform float u_inflate;

// Face order matches the item shader and the sub-voxel mesher.
const vec3 NORMALS[6] = vec3[](
    vec3(0, 0, 1), vec3(1, 0, 0), vec3(0, 0,-1),
    vec3(-1, 0, 0), vec3(0, 1, 0), vec3(0,-1, 0));

// The stamp's merged cubes lie in block units like the item they build on or dig from. Each face is pushed out
// along its normal by u_inflate, in blocks, so a stamp over cubes it digs never z-fights them.
void main() {
    vec3 position = aPos + NORMALS[int(aNorIndex)] * u_inflate;
    gl_Position   = u_viewProjection * vec4(position, 1.0);
}
