#version 150
in vec3 v_dir;
out vec4 fragColor;

void main() {
    // Normalize the direction to get consistent values
    vec3 dir = normalize(v_dir);

    // Map direction components from [-1, 1] to [0, 1] for color display
    vec3 color = dir * 0.5 + 0.5;

    fragColor = vec4(color, 1.0);
}