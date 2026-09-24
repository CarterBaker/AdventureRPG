#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;

uniform vec4 u_viewRect;

out vec2 vAtlasUV;

// aPos spans the unit square; it is stretched across u_viewRect, given in
// normalized device coordinates as (left, bottom, right, top).
void main() {
    vAtlasUV    = aPos.xy;
    gl_Position = vec4(mix(u_viewRect.xy, u_viewRect.zw, aPos.xy), 0.0, 1.0);
}
