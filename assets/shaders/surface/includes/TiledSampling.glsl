#ifndef TILED_SAMPLING_GLSL
#define TILED_SAMPLING_GLSL

#include "surface/includes/SurfaceTessellationTier.glsl"

// Lays a face's texture once per block. The face's planar coordinate runs continuously across a merged quad and the
// tile repeats over it, so its UVs need no wrapping of their own; a natural top or bottom face spins its tile by a
// hash of the block it belongs to, found a quarter block behind the sampled position — inside the owning sub-block,
// so a sub-block face lying inside its block, or on either side of a block boundary, always resolves to its own
// block. The spin turns the UV about each block's centre; FaceGradient.glsl turns the screen gradients with it.

int blockPosHash(ivec3 p) {
    int h = p.x * 1619 + p.y * 31337 + p.z * 6271;
    h ^= (h >> 14);
    h *= 1540483477;
    h ^= (h >> 10);
    h ^= (h >> 24);
    return h;
}

// The planar axes a face reads and the quarter turns its tile takes, as x and y
ivec2 resolveFaceLayout(vec3 localPos, vec3 normal, float encodedFaceF) {
    int encodedFace = int(encodedFaceF);

    if (encodedFace >= 24) {
        int faceOrdinal = encodedFace - 24;
        ivec3 blockPos = ivec3(floor(localPos - normal * (SUB_BLOCK_SIZE * 0.5)));
        // Mask to [0, CHUNK_SIZE-1] so block (1,1,1) hashes identically in every chunk.
        // u_chunkSize = 16 (const from SettingsData.glsl); power-of-two so & mask is exact,
        // and two's complement makes it correct for negative coordinates too.
        int hash = blockPosHash((blockPos & ivec3(int(u_chunkSize) - 1)) + ivec3(faceOrdinal * 7, faceOrdinal * 13, faceOrdinal * 3));
        return ivec2(0, abs(hash) & 3);
    }

    vec2 faceData = u_faceOrientations[encodedFace];
    return ivec2(int(faceData.x), int(faceData.y));
}

vec2 resolveFacePlanar(vec3 localPos, int axisMode) {
    if (axisMode == 0) return localPos.xz;
    if (axisMode == 1) return localPos.zy;
    return localPos.xy;
}

// A vector in the tile's frame turned by the spin
vec2 spinTileVector(vec2 v, int spin) {
    if (spin == 1) return vec2(-v.y,  v.x);
    if (spin == 2) return vec2(-v.x, -v.y);
    if (spin == 3) return vec2( v.y, -v.x);
    return v;
}

vec2 tileUV(vec3 localPos, vec3 normal, float encodedFace) {
    ivec2 faceLayout = resolveFaceLayout(localPos, normal, encodedFace);
    return spinTileVector(resolveFacePlanar(localPos, faceLayout.x) - 0.5, faceLayout.y) + 0.5;
}

#endif
