#ifndef TILED_SAMPLING_GLSL
#define TILED_SAMPLING_GLSL

int blockPosHash(ivec3 p) {
    int h = p.x * 1619 + p.y * 31337 + p.z * 6271;
    h ^= (h >> 14);
    h *= 1540483477;
    h ^= (h >> 10);
    h ^= (h >> 24);
    return h;
}

vec2 tileUV(vec3 localPos, vec2 uvOrigin, vec3 normal, float encodedFaceF) {

    int encodedFace = int(encodedFaceF);
    int axisMode;
    int spin;

    if (encodedFace >= 24) {
        int faceOrdinal = encodedFace - 24;
        axisMode = 0;

        ivec3 blockPos = ivec3(floor(localPos - normal * 0.5));

        int hash = blockPosHash(blockPos + ivec3(faceOrdinal * 7, faceOrdinal * 13, faceOrdinal * 3));
        spin = abs(hash) & 3;

    } else {
        vec2 faceData = u_faceOrientations[encodedFace];
        axisMode = int(faceData.x);
        spin     = int(faceData.y);
    }

    vec2 planar;
    if      (axisMode == 0) planar = localPos.xz;
    else if (axisMode == 1) planar = localPos.zy;
    else                    planar = localPos.xy;

    vec2 c = fract(planar) - 0.5;
    vec2 rotated;
    if      (spin == 1) rotated = vec2(-c.y,  c.x);
    else if (spin == 2) rotated = vec2(-c.x, -c.y);
    else if (spin == 3) rotated = vec2( c.y, -c.x);
    else                rotated = c;

    return uvOrigin + (rotated + 0.5) * u_uvPerBlock;
}

#endif