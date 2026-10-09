#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;

#include "includes/CameraData.glsl"

// Face order matches the item shader and the sub-voxel mesher.
const vec3 NORMALS[6] = vec3[](
    vec3(0, 0, 1), vec3(1, 0, 0), vec3(0, 0,-1),
    vec3(-1, 0, 0), vec3(0, 1, 0), vec3(0,-1, 0));

out vec3 vNormal;
out vec3 vPosition;
out vec2 vUVOrigin;
flat out int vFace;

void main() {
    vNormal     = NORMALS[int(aNorIndex)];
    vPosition   = aPos;
    vUVOrigin   = aUV;
    vFace       = int(aNorIndex);
    gl_Position = u_viewProjection * vec4(aPos, 1.0);
}
