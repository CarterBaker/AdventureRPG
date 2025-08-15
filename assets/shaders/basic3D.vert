attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

varying vec3 v_normal;
varying vec2 v_texCoord;
varying vec3 v_worldPos;

void main() {
    vec4 worldPosition = u_worldTrans * vec4(a_position, 1.0);
    v_worldPos = worldPosition.xyz;
    v_normal = mat3(u_worldTrans) * a_normal;
    v_texCoord = a_texCoord0;
    gl_Position = u_projViewTrans * worldPosition;
}
