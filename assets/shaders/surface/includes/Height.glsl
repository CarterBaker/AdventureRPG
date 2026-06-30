#ifndef HEIGHT_GLSL
#define HEIGHT_GLSL

// Parallax UV offset. Call before every other sampler.
// viewDirTS is the view direction transformed into tangent space:
//   vec3 camPos    = (u_inverseView * vec4(0,0,0,1)).xyz;
//   vec3 viewDirW  = normalize(camPos - vLocalPos);
//   vec3 viewDirTS = normalize(transpose(TBN) * viewDirW);
vec2 parallaxUV(vec2 tiledUV, int layer, vec3 viewDirTS, float scale) {
    float h = texture(u_textureArray, vec3(tiledUV, float(layer))).r;
    return tiledUV + (viewDirTS.xy / max(viewDirTS.z, 0.1)) * ((h - 0.5) * scale);
}

#endif