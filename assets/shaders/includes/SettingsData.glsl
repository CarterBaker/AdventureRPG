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

    // Source: application Settings — runtime, user-configurable. Distance in
    // blocks out to which characters draw their fine detail: normal maps,
    // relief and the eyes' depth and reflections. See
    // characters/includes/CharacterMaterial.glsl.
    float u_characterDetail;

    // Source: application Settings — runtime, user-configurable. The
    // anisotropic filtering every tile pool samples with, held within what
    // the driver allows; tile reads measure their footprint with it. See
    // includes/TileTextures.glsl.
    float u_textureAnisotropy;
};
#endif