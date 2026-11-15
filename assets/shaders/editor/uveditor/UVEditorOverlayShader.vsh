#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aOther;
layout(location = 2) in vec2 aShape;
layout(location = 3) in vec4 aColor;

uniform vec4 u_uvToClip;
uniform vec2 u_viewSize;

out vec4 vColor;

// aPos is a point in UV, mapped to the window by u_uvToClip (scale x, scale y, offset x, offset y). A dot's corner
// sits aShape pixels off it; a line's side sits aShape.x pixels across the line toward aOther, its aShape.y zero,
// so both keep their size on screen however far the view is zoomed.
void main() {
    vec2 clip   = aPos.xy * u_uvToClip.xy + u_uvToClip.zw;
    vec2 offset = aShape;

    if (aShape.y == 0.0) {
        vec2  other     = aOther.xy * u_uvToClip.xy + u_uvToClip.zw;
        vec2  direction = (other - clip) * u_viewSize;
        float span      = length(direction);
        vec2  across    = span > 0.0 ? vec2(-direction.y, direction.x) / span : vec2(0.0, 1.0);
        offset = across * aShape.x;
    }

    vColor      = aColor;
    gl_Position = vec4(clip + offset * 2.0 / u_viewSize, 0.0, 1.0);
}
