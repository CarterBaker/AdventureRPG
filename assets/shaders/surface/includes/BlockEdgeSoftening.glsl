#ifndef BLOCK_EDGE_SOFTENING_GLSL
#define BLOCK_EDGE_SOFTENING_GLSL

// cheap integer hash — unique per block coordinate
float blockHash(vec3 blockPos) {
    ivec3 i  = ivec3(floor(blockPos));
    int   n  = i.x * 1619 + i.y * 31337 + i.z * 6271;
    n        = (n << 13) ^ n;
    return fract(float(n * (n * n * 15731 + 789221) + 1376312589) / 2147483648.0);
}

vec3 applyBlockEdgeSoftening(vec3 litColor, vec3 localPos, vec3 normal) {

    vec3  b       = fract(localPos);
    vec3  e       = min(b, 1.0 - b);

    // mask out the face normal axis
    float edgeFactor;
    vec3  absNorm = abs(normal);
    if      (absNorm.y > 0.5) edgeFactor = min(e.x, e.z);
    else if (absNorm.x > 0.5) edgeFactor = min(e.y, e.z);
    else                      edgeFactor = min(e.x, e.y);

    // unique per block — varies edge width and shadow depth
    float hash       = blockHash(localPos);
    float edgeWidth  = mix(0.04, 0.12, hash);
    float shadowFloor = mix(0.55, 0.75, hash);

    float soft   = smoothstep(0.0, edgeWidth, edgeFactor);
    float shadow = mix(shadowFloor, 1.0, soft);

    return litColor * shadow;
}

#endif