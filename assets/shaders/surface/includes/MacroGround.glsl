#ifndef MACRO_GROUND_GLSL
#define MACRO_GROUND_GLSL

#include "surface/includes/MacroCrinkle.glsl"

// Where macro terrain stands: crinkled (MacroCrinkle), then sunk a little beneath the chunk grid's rim, the way a
// terrain LOD hangs a skirt under its finer neighbour. The macro lattice is coarser than the blocks and the crinkle
// lifts it, so at the rim its ground could stand above the voxel ground it meets, and a view grazing the voxel edge
// would pass under it to the sky as a pale seam. Sunk by at least as much as either lifts it across a band just past
// the rim, it always meets the rim from below, where the voxel ground hides the step, and it rises back to its own
// height before the band ends. The grid spans u_renderDistance chunks centred on the grid's origin, so the rim is
// found from the larger of a grid-relative position's two horizontal distances.

const float MACRO_RIM_SINK_BLOCKS = 4.0;
const float MACRO_RIM_BAND_CHUNKS = 2.0;

// Blocks the macro ground at a grid-relative position sinks below its own height
float resolveMacroRimSink(vec2 position) {
    float rim     = u_renderDistance * 0.5 * u_chunkSize;
    float reach   = max(abs(position.x), abs(position.y));
    float share   = 1.0 - smoothstep(rim, rim + MACRO_RIM_BAND_CHUNKS * u_chunkSize, reach);
    float crinkle = dot(MACRO_CRINKLE_AMPLITUDES, vec3(1.0)) * max(u_macroCrinkle, 0.0);

    return (MACRO_RIM_SINK_BLOCKS + crinkle) * share;
}

// A grid-relative position on macro terrain moved to where it is drawn: the ground along its height, a tree
// stand-in a little every way, both sunk beneath the rim
vec3 placeOnMacroGround(vec3 position, bool tree) {
    vec3 placed = applyMacroCrinkle(position, tree);

    placed.y -= resolveMacroRimSink(position.xz);

    return placed;
}

#endif
