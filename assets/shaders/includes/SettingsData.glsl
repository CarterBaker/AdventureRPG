#ifndef SETTINGS_DATA_GLSL
#define SETTINGS_DATA_GLSL
layout(std140) uniform SettingsData {
    // Source: application Settings — runtime, user-configurable
    float u_renderDistance;

    // Source: EngineSetting.CHUNK_SIZE — compile-time constant, uploaded once on awake
    float u_chunkSize;

    // Source: application Settings — runtime, user-configurable. Radius in
    // blocks of every piece of near surface detail: tessellation, bevel,
    // relief, material maps and covering growth. See
    // surface/includes/SurfaceTessellationTier.glsl.
    float u_detailRadius;
};
#endif