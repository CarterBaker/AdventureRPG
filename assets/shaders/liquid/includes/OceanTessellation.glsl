#ifndef OCEAN_TESSELLATION_GLSL
#define OCEAN_TESSELLATION_GLSL

#include "includes/SettingsData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/OceanData.glsl"

/*
 * Tessellation density for the tidal sea, in vertices per block, decided per chunk from its Chebyshev
 * distance to the player's chunk: four near, two in the mid ring, one out to the far radius, and none past
 * it. Every level is a whole number of blocks times a power of two, so every tessellated vertex lies on the
 * quarter-block lattice, and a long merged quad places its vertices exactly where the short quads beside it
 * place theirs. Within a chunk every edge uses the chunk's density; an edge on a chunk border, and a side
 * face standing on one, uses the finer of the two chunks', so both sides of a border always agree. Waves are
 * displaced only out to the far radius and fade to exactly zero before the first chunk past it, where the sea
 * is flat and needs no vertices between its corners.
 */

const float OCEAN_NEAR_DENSITY        = 4.0;
const float OCEAN_MID_DENSITY         = 2.0;
const float OCEAN_FAR_DENSITY         = 1.0;
const float OCEAN_LATTICE_DENSITY     = 4.0;
const float OCEAN_MAX_TESS_LEVEL      = 64.0;
const float OCEAN_CHUNK_PROBE_BLOCKS  = 0.5;
const float OCEAN_VERTICAL_EDGE_RATIO = 0.5;

ivec2 resolveOceanChunk(vec2 pos) {
    return ivec2(floor(pos / u_chunkSize));
}

int resolveOceanChunkDistance(ivec2 chunk) {
    ivec2 offset = abs(chunk - resolveOceanChunk(u_playerPosition.xz));
    return max(offset.x, offset.y);
}

float resolveOceanDensity(ivec2 chunk) {
    int distance = resolveOceanChunkDistance(chunk);

    if (distance <= int(u_oceanTessellation.x))
    return OCEAN_NEAR_DENSITY;

    if (distance <= int(u_oceanTessellation.y))
    return OCEAN_MID_DENSITY;

    if (distance <= int(u_oceanTessellation.z))
    return OCEAN_FAR_DENSITY;

    return 0.0;
}

bool isOceanVerticalEdge(vec3 edge) {
    return abs(edge.y) > length(edge) * OCEAN_VERTICAL_EDGE_RATIO;
}

float resolveOceanTessLevel(float lengthBlocks, float density) {
    return clamp(floor(lengthBlocks * density + 0.5), 1.0, OCEAN_MAX_TESS_LEVEL);
}

// The chunk a patch belongs to, probed half a block inside the water it bounds so a face lying exactly on a
// chunk boundary still resolves to the chunk that owns its block.
ivec2 resolveOceanPatchChunk(vec3 center, vec3 normal) {
    return resolveOceanChunk(center.xz - normal.xz * OCEAN_CHUNK_PROBE_BLOCKS);
}

// The chunk on the far side of an edge: across the edge for a horizontal face, across the face for a side
// face, whose horizontal edges lie in the same boundary plane the face does.
ivec2 resolveOceanNeighborChunk(vec3 edgeMid, vec3 center, vec3 normal) {
    if (abs(normal.y) > OCEAN_VERTICAL_EDGE_RATIO)
    return resolveOceanChunk(edgeMid.xz + normalize(edgeMid.xz - center.xz) * OCEAN_CHUNK_PROBE_BLOCKS);

    return resolveOceanChunk(center.xz + normal.xz * OCEAN_CHUNK_PROBE_BLOCKS);
}

float resolveOceanEdgeLevel(vec3 from, vec3 to, vec3 center, vec3 normal, float patchDensity) {
    vec3 edge = to - from;

    if (isOceanVerticalEdge(edge))
    return 1.0;

    float neighborDensity = resolveOceanDensity(resolveOceanNeighborChunk((from + to) * 0.5, center, normal));

    return resolveOceanTessLevel(length(edge), max(patchDensity, neighborDensity));
}

float resolveOceanInnerLevel(vec3 from, vec3 to, float patchDensity) {
    vec3 edge = to - from;

    if (isOceanVerticalEdge(edge))
    return 1.0;

    return resolveOceanTessLevel(length(edge), patchDensity);
}

vec2 snapToOceanLattice(vec2 pos) {
    return round(pos * OCEAN_LATTICE_DENSITY) / OCEAN_LATTICE_DENSITY;
}

// 1 inside the far radius, easing to exactly 0 at the edge of the last tessellated chunk ring.
float resolveOceanDisplacementFade(vec2 pos) {
    vec2  playerChunkCenter = (vec2(resolveOceanChunk(u_playerPosition.xz)) + 0.5) * u_chunkSize;
    vec2  offset            = abs(pos - playerChunkCenter) / u_chunkSize;
    float distance          = max(offset.x, offset.y);
    float fadeEnd           = u_oceanTessellation.z + 0.5;

    return 1.0 - smoothstep(fadeEnd - u_oceanTessellation.w, fadeEnd, distance);
}

#endif
