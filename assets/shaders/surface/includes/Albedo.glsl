#ifndef ALBEDO_GLSL
#define ALBEDO_GLSL

uniform sampler2DArray u_textureArray;

vec4 sampleLayer(vec2 uv, int layer) {
    return texture(u_textureArray, vec3(uv, float(layer)));
}

#endif