#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;

uniform vec4 u_uvToClip;

out vec2 vUV;

// aPos spans the unit square, stretched over the whole window; each corner carries the UV the view shows there,
// u_uvToClip mapping a UV to normalized device coordinates as (scale x, scale y, offset x, offset y).
void main() {
    vec2 clip   = aPos.xy * 2.0 - 1.0;
    vUV         = (clip - u_uvToClip.zw) / u_uvToClip.xy;
    gl_Position = vec4(clip, 0.0, 1.0);
}
