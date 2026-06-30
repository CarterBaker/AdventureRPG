#ifndef AO_GLSL
#define AO_GLSL

// 1.0 = fully exposed, 0.0 = fully occluded.
float sampleAO(vec2 tiledUV, int layer) {
    return texture(u_textureArray, vec3(tiledUV, float(layer))).r;
}

#endif