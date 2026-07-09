#ifndef CLOUD_SETTINGS_DATA_GLSL
#define CLOUD_SETTINGS_DATA_GLSL

// Source: u_cloudHorizonDistance derives from whichever is smaller of the
// player's configured render distance (Settings.maxRenderDistance) and
// EngineSetting.WEATHER_NEAR_RANGE_CHUNKS (converted to world/block units,
// scaled by CLOUD_HORIZON_RENDER_DISTANCE_SCALE) — see
// CloudRenderSystem.pushCloudSettings(). u_cloudSkyViewDistance still
// derives from the fixed WEATHER_FAR_RANGE_CHUNKS, untouched by render
// distance. Both combine with CLOUD_HORIZON_*_SCALE /
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
// actually resolves. Stays a sky-dome-only bound — never read by the
// per-instance cloud object shader, since those instances never exist
// beyond u_cloudHorizonDistance in the first place.
//
// u_cloudTransitionStart is the distance (derived from
// u_cloudHorizonDistance * CLOUD_VOLUME_FADE_START_RATIO) at which real
// cloud objects begin fading out toward u_cloudHorizonDistance — see
// CloudVolumeShader.vsh's horizonFade. Combined with the existing size
// LOD, an object cloud has both shrunk and faded to invisible well before
// OverheadManager ever retires its cell, so the near-range handoff between
// the sky representation and real cloud objects has no hard pop in either
// direction.
layout(std140) uniform CloudSettingsData {
    float u_cloudHorizonDistance;
    float u_cloudMinScale;
    float u_cloudMaxScale;
    float u_cloudSkyViewDistance;
    float u_cloudTransitionStart;
};

#endif