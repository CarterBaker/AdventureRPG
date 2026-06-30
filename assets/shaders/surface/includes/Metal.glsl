#ifndef METAL_GLSL
#define METAL_GLSL

// 0.0 = dielectric, 1.0 = metal.
float sampleMetal(vec2 tiledUV, int layer) {
    return texture(u_textureArray, vec3(tiledUV, float(layer))).r;
}

#endif