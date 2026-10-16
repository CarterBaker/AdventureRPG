#ifndef CONTENT_DATA_GLSL
#define CONTENT_DATA_GLSL

// Must match EngineSetting.CONTENT_ID_COUNT and the counts in ubos/ContentData — GLSL has no visibility into the Java
// constants.
#define CONTENT_ID_COUNT 256

// Source: ContentBufferSystem, indexed by content ID.
//
// contentTiles:  xy = the overlay tile's atlas origin.
// contentReveal: rgb = the average albedo of the texels the overlay shows, a = the share of its tile it shows.
layout(std140) uniform ContentData {
    vec4 u_contentTiles[CONTENT_ID_COUNT];
    vec4 u_contentReveal[CONTENT_ID_COUNT];
};

#endif
