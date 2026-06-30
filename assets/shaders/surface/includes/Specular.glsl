#ifndef SPECULAR_GLSL
#define SPECULAR_GLSL

// 0.0 = fully matte, 1.0 = fully specular.
float sampleSpecular(vec2 tiledUV, int layer) {
    return texture(u_textureArray, vec3(tiledUV, float(layer))).r;
}

#endif