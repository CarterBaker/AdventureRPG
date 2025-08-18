#version 450 core

// Vertex attributes
layout(location = 0) in vec3 a_position;
layout(location = 1) in vec3 a_normal;
layout(location = 2) in vec2 a_texCoord;

// Outputs to fragment shader
layout(location = 0) out vec3 v_normal;
layout(location = 1) out vec3 v_fragPos;
layout(location = 2) out vec2 v_texCoord;

// Uniforms
layout(set = 0, binding = 0) uniform Matrices {
    mat4 u_worldTrans;
    mat4 u_projViewTrans;
};

void main() {
    v_fragPos = vec3(u_worldTrans * vec4(a_position, 1.0));
    v_normal = mat3(transpose(inverse(u_worldTrans))) * a_normal;
    v_texCoord = a_texCoord;

    gl_Position = u_projViewTrans * vec4(a_position, 1.0);
}
