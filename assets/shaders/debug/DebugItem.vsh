#version 330 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in float aNorIndex;
layout (location = 2) in float aColor;
layout (location = 3) in vec2  aUVOrigin;
layout (location = 4) in float aOrient;

uniform float u_rotation;

const vec3 NORMALS[6] = vec3[](
    vec3(0, 0, 1), vec3(1, 0, 0), vec3(0, 0,-1),
    vec3(-1, 0, 0), vec3(0, 1, 0), vec3(0,-1, 0));

out vec3       vLocalPos;
out vec3       vNormal;
flat out vec2  vUVOrigin;
flat out float vOrient;

void main() {
    // Step 1: center the mesh — verts are 0-1, pivot must be 0,0,0
    vec3 centered = aPos - vec3(0.5, 0.5, 0.5);

    // Step 2: rotate around Y axis
    float c = cos(u_rotation);
    float s = sin(u_rotation);

    vec3 rotPos = vec3(
        c * centered.x + s * centered.z,
        centered.y,
        -s * centered.x + c * centered.z);

    vec3 rawNormal = NORMALS[int(aNorIndex)];
    vec3 rotNormal = vec3(
        c * rawNormal.x + s * rawNormal.z,
        rawNormal.y,
        -s * rawNormal.x + c * rawNormal.z);

    vLocalPos = rotPos;
    vNormal   = rotNormal;
    vUVOrigin = aUVOrigin;
    vOrient   = aOrient;

    // Step 3: fixed debug perspective, camera at Z=3 looking at origin
    float scale    = 0.8;
    float camDist  = 3.0;
    float z        = camDist - rotPos.z;

    gl_Position = vec4(
        rotPos.x * scale / z,
        rotPos.y * scale / z,
        rotPos.z / 10.0,
        1.0);
}