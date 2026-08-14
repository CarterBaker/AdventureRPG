#ifndef SETTINGS_DATA_GLSL
#define SETTINGS_DATA_GLSL
layout(std140) uniform SettingsData {
    // Source: application Settings — runtime, user-configurable
    float u_renderDistance;

    // Source: EngineSetting.CHUNK_SIZE — compile-time constant, uploaded once on awake
    float u_chunkSize;

    // Source: application Settings — runtime, user-configurable. Chebyshev
    // chunk radius of the near tessellation ring (bevel + heightmap). See
    // surface/includes/SurfaceTessellationTier.glsl.
    float u_nearTessellationRadius;
};
#endif