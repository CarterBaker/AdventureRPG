#ifndef ALBEDO_GLSL
#define ALBEDO_GLSL

vec4 sampleLayer(vec2 uv, int layer) {
    return texture(u_textureArray, vec3(uv, float(layer)));
}

vec4 sampleLayerTiled(vec2 tiledUV, int layer) {
    return texture(u_textureArray, vec3(tiledUV, float(layer)));
}

#endif