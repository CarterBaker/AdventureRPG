#ifndef WORLD_DISTORTION_DATA_GLSL
#define WORLD_DISTORTION_DATA_GLSL

// Must match EngineSetting.CHUNK_SIZE — GLSL array sizes are compile-time,
// so this is a manually-kept mirror, same convention WaterShader.vsh uses
// for its own LIQUID_LEVEL_MAX constant.
#define WORLD_DISTORTION_GRID_SIZE 16

layout(std140) uniform WorldDistortionData {
    vec4 u_worldDistortion[WORLD_DISTORTION_GRID_SIZE * WORLD_DISTORTION_GRID_SIZE];
};

#endif