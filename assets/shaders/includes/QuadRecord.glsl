#ifndef QUAD_RECORD_GLSL
#define QUAD_RECORD_GLSL

// Pulls packed quad records from the draw's buffer texture by vertex index, with no vertex attributes. Every quad is
// one patch of four consecutive vertices, listed origin, +A, +A+B, +B; the vertex stage passes gl_VertexID in, since
// every include reaches every stage. Word 0 of every layout is the quad's position in sub-blocks from the draw's
// origin: x in the low ten bits, z in the next ten and y in the top twelve. Further words may hold a stream of 24-bit words packed four to three ints. Tangents follow Direction3Vector: A runs east on
// north, south, up and down faces and north on east and west faces; B runs up on every upright face and north on up
// and down faces. Must match the Quad Records group in EngineSetting and QuadRecordUtility; QUAD_SUB_BLOCK_SIZE must
// match EngineSetting.SUB_BLOCK_SIZE.

uniform usamplerBuffer u_quadBuffer;

const float QUAD_SUB_BLOCK_SIZE    = 0.5;
const uint  QUAD_POSITION_XZ_MASK  = 1023u;
const int   QUAD_POSITION_Z_SHIFT  = 10;
const int   QUAD_POSITION_Y_SHIFT  = 20;
const uint  QUAD_STREAM_WORD_MASK  = 16777215u;
const int   QUAD_FACE_MASK         = 7;
const int   QUAD_FACE_DOWN         = 5;

const ivec3 QUAD_NORMALS[6] = ivec3[](
    ivec3( 0, 0, 1),
    ivec3( 1, 0, 0),
    ivec3( 0, 0,-1),
    ivec3(-1, 0, 0),
    ivec3( 0, 1, 0),
    ivec3( 0,-1, 0));

const ivec3 QUAD_TANGENTS_A[6] = ivec3[](
    ivec3(1, 0, 0),
    ivec3(0, 0, 1),
    ivec3(1, 0, 0),
    ivec3(0, 0, 1),
    ivec3(1, 0, 0),
    ivec3(1, 0, 0));

const ivec3 QUAD_TANGENTS_B[6] = ivec3[](
    ivec3(0, 1, 0),
    ivec3(0, 1, 0),
    ivec3(0, 1, 0),
    ivec3(0, 1, 0),
    ivec3(0, 0, 1),
    ivec3(0, 0, 1));

int getQuadIndex(int vertexID) {
    return vertexID >> 2;
}

int getQuadCorner(int vertexID) {
    return vertexID & 3;
}

ivec3 unpackQuadPosition(uint word) {
    return ivec3(
        int(word & QUAD_POSITION_XZ_MASK),
        int(word >> QUAD_POSITION_Y_SHIFT),
        int((word >> QUAD_POSITION_Z_SHIFT) & QUAD_POSITION_XZ_MASK));
}

// How many extents along A and B a patch corner lies from the origin
ivec2 getQuadCornerSteps(int corner) {
    return ivec2((corner == 1 || corner == 2) ? 1 : 0, (corner >= 2) ? 1 : 0);
}

// The offset of a corner from the origin, the extents in whatever unit the layout stores them
ivec3 resolveQuadCornerOffset(int face, int corner, int sizeA, int sizeB) {
    ivec2 steps = getQuadCornerSteps(corner);
    return QUAD_TANGENTS_A[face] * (sizeA * steps.x) + QUAD_TANGENTS_B[face] * (sizeB * steps.y);
}

// Four 24-bit words from the three ints they are packed into
uvec4 unpackQuadWords(uint first, uint second, uint third) {
    return uvec4(
        first & QUAD_STREAM_WORD_MASK,
        (first >> 24) | ((second << 8) & QUAD_STREAM_WORD_MASK),
        (second >> 16) | ((third << 16) & QUAD_STREAM_WORD_MASK),
        third >> 8);
}

#endif
