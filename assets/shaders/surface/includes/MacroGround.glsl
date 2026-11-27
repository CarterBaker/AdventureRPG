#ifndef MACRO_GROUND_GLSL
#define MACRO_GROUND_GLSL

#include "includes/SettingsData.glsl"

// Where macro terrain stands: at its own height, sunk a little where it meets the chunk grid's rim, the way a terrain
// LOD hangs a skirt under its finer neighbour. The macro lattice is coarser than the blocks, so at the rim its ground
// could stand above the voxel ground it meets, and a view grazing the voxel edge would pass under it to the sky as a
// pale seam. Sunk right at the rim, it always meets the rim from below, where the voxel ground hides the step, and it
// rises back to its own height within a chunk either side, so ground filling a chunk still streaming in further inside
// never shows sunk. The grid spans u_renderDistance chunks centred on the grid's origin, so the rim is found from the
// larger of a grid-relative position's two horizontal distances.

const float MACRO_RIM_SINK_BLOCKS = 4.0;
const float MACRO_RIM_BAND_CHUNKS = 1.0;

// Blocks the macro ground at a grid-relative position sinks below its own height
float resolveMacroRimSink(vec2 position) {
    float rim   = u_renderDistance * 0.5 * u_chunkSize;
    float reach = max(abs(position.x), abs(position.y));
    float share = 1.0 - smoothstep(0.0, MACRO_RIM_BAND_CHUNKS * u_chunkSize, abs(reach - rim));

    return MACRO_RIM_SINK_BLOCKS * share;
}

// A grid-relative position on macro terrain moved to where it is drawn, sunk beneath the rim
vec3 placeOnMacroGround(vec3 position) {
    return position - vec3(0.0, resolveMacroRimSink(position.xz), 0.0);
}

#endif
