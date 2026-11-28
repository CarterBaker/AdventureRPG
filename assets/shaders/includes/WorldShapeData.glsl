#ifndef WORLD_SHAPE_DATA_GLSL
#define WORLD_SHAPE_DATA_GLSL
layout(std140) uniform WorldShapeData {
    // Source: WorldShapeBufferSystem — the active world's shape, pushed when the active world changes. One over
    // twice a globe's radius in blocks, zero on a flat torus. See includes/WorldCurvature.glsl.
    float u_horizonCurvature;
};
#endif
