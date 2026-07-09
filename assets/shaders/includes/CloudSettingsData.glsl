#ifndef CLOUD_SETTINGS_DATA_GLSL
#define CLOUD_SETTINGS_DATA_GLSL

// Source: EngineSetting.WEATHER_NEAR_RANGE_CHUNKS / WEATHER_FAR_RANGE_CHUNKS
// (converted to world/block units) and CLOUD_HORIZON_*_SCALE /
// CLOUD_VOLUME_FADE_START_RATIO. Pushed once at bootstrap by
// CloudRenderSystem — none of this changes after that, so no per-frame
// update is needed.
//
// u_cloudHorizonDistance / u_cloudMinScale / u_cloudMaxScale drive the
// existing near-field size LOD (see CloudVolumeShader.vsh) — cards shrink
// toward u_cloudMinScale as they approach u_cloudHorizonDistance, the same
// radius OverheadManager streams real cloud objects within.
//
// u_cloudSkyViewDistance is the distance out to which the sky dome's own
// cloud representation (see sky/util/Clouds.glsl) is considered to
// visually extend — derived from WEATHER_FAR_RANGE_CHUNKS, the same radius
// RegionSampleBranch already samples its 8 compass directions within, so
// the sky never claims to show weather farther out than the simulation
// actually resolves.
//
// u_cloudTransitionStart is the distance (derived from
// u_cloudHorizonDistance * CLOUD_VOLUME_FADE_START_RATIO) at which real
// cloud objects should begin taking over from the sky representation.
// Neither of these two is read by any shader yet — staged ahead of the
// sky/world crossfade rework so that rework only has to change GLSL, not
// this struct or the Java that populates it.
layout(std140) uniform CloudSettingsData {
    float u_cloudHorizonDistance;
    float u_cloudMinScale;
    float u_cloudMaxScale;
    float u_cloudSkyViewDistance;
    float u_cloudTransitionStart;
};

#endif