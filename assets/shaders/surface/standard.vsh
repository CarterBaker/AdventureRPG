#version 330

in vec3 a_position;
in float a_colorPacked;
in vec3 a_normal;
in vec3 a_tangent;
in vec2 a_texCoord0;

uniform mat4 u_model;
uniform mat4 u_view;
uniform mat4 u_projection;
uniform mat4 u_transform;

out vec3 v_worldPos;
out vec3 v_normal;
out vec3 v_tangent;
out vec2 v_uv;
out vec4 v_color;

vec4 unpackColor(float f) {
    uint rgba = floatBitsToUint(f);
    float r = float((rgba >> 24) & 0xFFu) / 255.0;
    float g = float((rgba >> 16) & 0xFFu) / 255.0;
    float b = float((rgba >> 8) & 0xFFu) / 255.0;
    float a = float(rgba & 0xFFu) / 255.0;
    return vec4(r, g, b, a);
}

void main() {
    vec4 worldPos = u_transform * u_model * vec4(a_position, 1.0);
    v_worldPos = worldPos.xyz;

    mat3 normalMatrix = transpose(inverse(mat3(u_model * u_transform)));
    v_normal = normalize(normalMatrix * a_normal);
    v_tangent = normalize(normalMatrix * a_tangent);

    v_uv = a_texCoord0;
    v_color = unpackColor(a_colorPacked);

    gl_Position = u_projection * u_view * worldPos;
}
