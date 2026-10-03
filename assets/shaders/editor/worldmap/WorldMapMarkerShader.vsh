#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;

uniform vec2 u_markerCenter;
uniform vec2 u_markerDirection;
uniform vec2 u_markerScale;

out vec2 vLocal;

// Places the character's arrow. aPos spans the unit square, recentred to -1..1
// and turned so local +y points along u_markerDirection, a screen direction
// with y up. u_markerCenter is the arrow's centre and u_markerScale one local
// unit, both in normalized device coordinates, so the arrow keeps its pixel
// size and shape at any window aspect.
void main() {
    vec2 forward = normalize(u_markerDirection);
    vec2 right   = vec2(forward.y, -forward.x);

    vLocal = aPos.xy * 2.0 - 1.0;

    vec2 offset = right * vLocal.x + forward * vLocal.y;

    gl_Position = vec4(u_markerCenter + offset * u_markerScale, 0.0, 1.0);
}
