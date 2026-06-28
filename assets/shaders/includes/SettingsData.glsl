#ifndef SETTINGS_DATA_GLSL
#define SETTINGS_DATA_GLSL
layout(std140) uniform SettingsData {
    // Source: application Settings — runtime, user-configurable
    float u_renderDistance;

    // Source: EngineSetting.CHUNK_SIZE — compile-time constant, uploaded once on awake
    float u_chunkSize;
};
#endif