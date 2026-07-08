#ifndef CLOUD_SETTINGS_DATA_GLSL
#define CLOUD_SETTINGS_DATA_GLSL

// Source: EngineSetting.HORIZON_DISTANCE (converted to world/block units —
// HORIZON_DISTANCE itself is chunk units, same convention OverheadManager
// and RegionSampleBranch use) and the two CLOUD_HORIZON_*_SCALE tunables.
// Pushed once at bootstrap by CloudRenderSystem — none of this changes
// after that, so no per-frame update is needed.
layout(std140) uniform CloudSettingsData {
    float u_cloudHorizonDistance;
    float u_cloudMinScale;
    float u_cloudMaxScale;
};

#endif