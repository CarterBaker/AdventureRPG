#version 330 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in float aNorIndex;
layout (location = 2) in vec2  aUV;

uniform float u_rotation;

const vec3 NORMALS[6] = vec3[](
    vec3(0, 0, 1), vec3(1, 0, 0), vec3(0, 0,-1),
    vec3(-1, 0, 0), vec3(0, 1, 0), vec3(0,-1, 0));

out vec3 vNormal;
out vec2 vUV;

void main() {
    vec3 centered = aPos - vec3(0.5, 0.5, 0.5);

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

    vNormal = rotNormal;
    vUV     = aUV;

    float scale   = 0.8;
    float camDist = 3.0;
    float z       = camDist - rotPos.z;
    gl_Position = vec4(
        rotPos.x * scale / z,
        rotPos.y * scale / z,
        rotPos.z / 10.0,
        1.0);
}