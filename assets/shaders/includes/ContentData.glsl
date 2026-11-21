#ifndef CONTENT_DATA_GLSL
#define CONTENT_DATA_GLSL

// Must match EngineSetting.CONTENT_ID_COUNT, CONTENT_AMOUNT_BITS, CONTENT_WORD_MASK and the counts in ubos/ContentData —
// GLSL has no visibility into the Java constants.
#define CONTENT_ID_COUNT 256
#define CONTENT_AMOUNT_BITS 3
#define CONTENT_AMOUNT_MASK 7
#define CONTENT_WORD_MASK 2047

// Source: ContentBufferSystem, indexed by content ID.
//
// contentTiles:        x = the overlay tile's ID, y = the most a cell holds.
// contentReveal:       rgb = the average albedo of the texels the overlay shows at its full level.
// contentRevealShares: the share of its tile the overlay shows at every coverage level, a byte per level.
layout(std140) uniform ContentData {
    vec4  u_contentTiles[CONTENT_ID_COUNT];
    vec4  u_contentReveal[CONTENT_ID_COUNT];
    ivec4 u_contentRevealShares[CONTENT_ID_COUNT];
};

#endif
