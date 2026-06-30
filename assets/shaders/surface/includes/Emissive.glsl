#ifndef EMISSIVE_GLSL
#define EMISSIVE_GLSL

// RGB glow. Caller converts to luminance scalar and packs into gAlbedo.a.
vec3 sampleEmissive(vec2 tiledUV, int layer) {
    return texture(u_textureArray, vec3(tiledUV, float(layer))).rgb;
}

#endif