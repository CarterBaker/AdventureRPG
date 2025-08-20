#version 150

in vec2 a_position;
out vec3 v_dir;

uniform mat4 u_inverseProjection;
uniform mat4 u_inverseView;

void main() {
    // Convert from clip space to view space ray
    vec4 clip = vec4(a_position, 0.0, 1.0);
    vec4 view = u_inverseProjection * clip;
    view.z = -1.0; // force into far plane
    view.w = 0.0;

    // Transform into world space direction
    v_dir = normalize((u_inverseView * view).xyz);

    gl_Position = vec4(a_position, 0.0, 1.0);
}
