#ifndef CLOUD_SETTINGS_DATA_GLSL
#define CLOUD_SETTINGS_DATA_GLSL

// Source: EngineSetting.WEATHER_NEAR_RANGE_CHUNKS (converted to world/block
// units — the same near-range radius OverheadManager streams real cloud
// objects within) and the two CLOUD_HORIZON_*_SCALE tunables. Pushed once
// at bootstrap by CloudRenderSystem — none of this changes after that, so
// no per-frame update is needed.
layout(std140) uniform CloudSettingsData {
    float u_cloudHorizonDistance;
    float u_cloudMinScale;
    float u_cloudMaxScale;
};

#endif