#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_normal;
varying vec2 v_texCoord;
varying vec3 v_worldPos;

uniform sampler2D u_texture;
uniform vec3 u_lightDir;  // normalized light direction
uniform vec4 u_color;

void main() {
    vec3 normal = normalize(v_normal);
    float light = max(dot(normal, normalize(u_lightDir)), 0.0);
    vec4 texColor = texture2D(u_texture, v_texCoord);
    gl_FragColor = texColor * u_color * light;
}
