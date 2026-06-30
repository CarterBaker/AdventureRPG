#ifndef NORMAL_GLSL
#define NORMAL_GLSL

// Axis-aligned TBN from any normal — works for voxel faces without vertex tangents.
// Pass the bevel-blended vNormal from TES so tangent space orients correctly
// on both flat faces and beveled edges.
mat3 buildTBN(vec3 N) {
    vec3 up = abs(N.y) < 0.999 ? vec3(0.0, 1.0, 0.0) : vec3(0.0, 0.0, 1.0);
    vec3 T  = normalize(cross(up, N));
    vec3 B  = cross(N, T);
    return mat3(T, B, N);
}

// Decodes normal map, runs it through TBN into view space.
vec3 sampleNormalViewSpace(vec2 tiledUV, int layer, vec3 vNormal, mat4 viewMat) {
    vec3 tsNormal = texture(u_textureArray, vec3(tiledUV, float(layer))).xyz * 2.0 - 1.0;
    mat3 TBN      = buildTBN(normalize(vNormal));
    vec3 worldN   = normalize(TBN * tsNormal);
    return normalize(mat3(viewMat) * worldN);
}

#endif