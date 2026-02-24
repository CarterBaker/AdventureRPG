#ifndef ALBEDO_GLSL
#define ALBEDO_GLSL

uniform sampler2DArray u_textureArray;

vec4 sampleLayer(vec2 uv, int layer) {
    return texture(u_textureArray, vec3(uv, float(layer)));
}

vec4 sampleLayerTiled(vec2 tiledUV, int layer) {
    return texture(u_textureArray, vec3(tiledUV, float(layer)));
}

#endif