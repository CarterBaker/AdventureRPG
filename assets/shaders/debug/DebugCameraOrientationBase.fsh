#version 150
in vec3 v_dir;
in vec2 v_screenPos;
out vec4 fragColor;

#include "includes/CameraData.glsl"

void main() {
    vec3 dir = normalize(v_dir);

    // Background checkerboard using spherical coordinates
    float phi = atan(dir.z, dir.x);
    float theta = acos(clamp(dir.y, -1.0, 1.0));
    float checkerSize = 0.3;
    float checker1 = mod(floor(phi / checkerSize) + floor(theta / checkerSize), 2.0);

    // Background colors based on direction
    vec3 color1 = vec3(0.2);
    vec3 color2 = vec3(0.4);

    // Color based on world axes
    color1.r += max(0.0, dir.x) * 0.3;    // +X = red
    color1.gb += max(0.0, -dir.x) * 0.3;  // -X = cyan
    color1.g += max(0.0, dir.y) * 0.3;    // +Y = green
    color1.rb += max(0.0, -dir.y) * 0.3;  // -Y = magenta

    color2.r += max(0.0, dir.x) * 0.5;
    color2.gb += max(0.0, -dir.x) * 0.5;
    color2.g += max(0.0, dir.y) * 0.5;
    color2.rb += max(0.0, -dir.y) * 0.5;

    vec3 bgColor = mix(color1, color2, checker1);

    float lineWidth = 0.005;

    // Great circles for each axis plane
    // XY plane (Z=0) - Blue circle
    float zDist = abs(dir.z);
    float zCircle = step(zDist, lineWidth);
    if (zCircle > 0.5) {
        bgColor = mix(bgColor, vec3(0.0, 0.0, 1.0), 0.8);
    }

    // XZ plane (Y=0) - Green circle
    float yDist = abs(dir.y);
    float yCircle = step(yDist, lineWidth);
    if (yCircle > 0.5) {
        bgColor = mix(bgColor, vec3(0.0, 1.0, 0.0), 0.8);
    }

    // YZ plane (X=0) - Red circle
    float xDist = abs(dir.x);
    float xCircle = step(xDist, lineWidth);
    if (xCircle > 0.5) {
        bgColor = mix(bgColor, vec3(1.0, 0.0, 0.0), 0.8);
    }

    fragColor = vec4(bgColor, 1.0);
}