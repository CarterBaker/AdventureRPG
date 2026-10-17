#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;

#include "includes/CameraData.glsl"

uniform mat4 u_model;

// aPos spans one unit cell; it is centred on the origin so u_model places, turns and sizes the marker about its
// middle — a bone's joint, or the centre of the box an equipment anchor fills.
void main() {
    gl_Position = u_viewProjection * u_model * vec4(aPos - vec3(0.5), 1.0);
}
