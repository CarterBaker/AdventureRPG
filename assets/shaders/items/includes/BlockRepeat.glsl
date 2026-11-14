#ifndef BLOCK_REPEAT_GLSL
#define BLOCK_REPEAT_GLSL

// Sub-voxel faces are merged across block boundaries and carry only their part's tile, so the tile repeats once per
// block, read from the position inside its block exactly as a single block lays its texels out on each face. The
// position runs on unbroken across a merged face and the tile repeats by itself, so the UV needs no wrapping and its
// screen gradients never jump (BlockAlbedo.glsl reads the tile with them). Faces are numbered as the sub-voxel mesher
// numbers them.

vec2 resolveBlockUV(vec3 local, int face) {
    if (face == 0) return vec2(local.x, local.y);
    if (face == 1) return vec2(1.0 - local.z, local.y);
    if (face == 2) return vec2(1.0 - local.x, local.y);
    if (face == 3) return vec2(local.z, local.y);
    if (face == 4) return vec2(local.x, local.z);
    return vec2(local.x, 1.0 - local.z);
}

#endif
