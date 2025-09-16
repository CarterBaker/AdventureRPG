#version 150

in vec3 a_position;
in vec4 a_color;  // declared but unused for now
// in vec3 a_normal;      // keep commented if not needed yet
// in vec2 a_texCoord0;   // keep commented if not needed yet

uniform mat4 u_model;
uniform mat4 u_view;
uniform mat4 u_projection;

void main() {
    gl_Position = u_projection * u_view * u_model * vec4(a_position, 1.0);
}
