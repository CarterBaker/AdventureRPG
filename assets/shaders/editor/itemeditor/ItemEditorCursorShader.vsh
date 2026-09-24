#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;

#include "includes/CameraData.glsl"

uniform vec3  u_cursorCell;
uniform float u_resolution;
uniform float u_inflate;

// aPos spans one unit cell. It is grown by u_inflate on every side so the
// cursor never z-fights the cube it surrounds, moved to the cursor cell,
// and scaled from cell units down to block units.
void main() {
    vec3 cellPos = aPos * (1.0 + 2.0 * u_inflate) - vec3(u_inflate) + u_cursorCell;
    gl_Position  = u_viewProjection * vec4(cellPos / u_resolution, 1.0);
}
