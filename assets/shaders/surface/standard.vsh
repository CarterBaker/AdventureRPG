#version 150

in vec3 a_position;
in vec4 a_color;    // still unused
in vec3 a_normal;   // required for lighting
in vec3 a_tangent;  // required for normal mapping
in vec2 a_texCoord0;

uniform mat4 u_model;
uniform mat4 u_view;
uniform mat4 u_projection;

// Varyings expected by pbr.glsl
out vec3 v_worldPos;
out vec3 v_normal;
out vec3 v_tangent;
out vec2 v_uv;
out vec4 v_color;

void main() {
    // Transform position into world space
    vec4 worldPos = u_model * vec4(a_position, 1.0);
    v_worldPos = worldPos.xyz;

    // Transform normal and tangent into world space
    mat3 normalMatrix = transpose(inverse(mat3(u_model)));
    v_normal = normalize(normalMatrix * a_normal);
    v_tangent = normalize(normalMatrix * a_tangent);

    // Pass through UVs
    v_uv = a_texCoord0;

    // Pass vertex color to fragment shader
    v_color = a_color;

    // Final clip-space position
    gl_Position = u_projection * u_view * worldPos;
}