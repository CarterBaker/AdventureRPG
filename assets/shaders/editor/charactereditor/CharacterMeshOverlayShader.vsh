#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aOther;
layout(location = 2) in vec2 aShape;
layout(location = 3) in vec4 aColor;

#include "includes/CameraData.glsl"

uniform float u_depthBias;

out vec4 vColor;

// A dot's corner sits aShape pixels off its vertex. A line's side sits aShape.x pixels across the line toward aOther,
// its aShape.y zero. Both keep their size on screen however far the camera is, and u_depthBias draws them a hair
// toward the camera so they win the depth test against the surface they lie on.
void main() {
    vec4 clip = u_viewProjection * vec4(aPos, 1.0);
    vec2 offset = aShape;

    if (aShape.y == 0.0) {
        vec4 otherClip = u_viewProjection * vec4(aOther, 1.0);
        vec2 direction = (otherClip.xy / otherClip.w - clip.xy / clip.w) * u_viewport;
        float span = length(direction);
        vec2 across = span > 0.0 ? vec2(-direction.y, direction.x) / span : vec2(0.0, 1.0);
        offset = across * aShape.x;
    }

    clip.xy += offset * 2.0 / u_viewport * clip.w;
    clip.z -= u_depthBias * clip.w;
    vColor = aColor;
    gl_Position = clip;
}
