#ifndef CLOUD_SETTINGS_DATA_GLSL
#define CLOUD_SETTINGS_DATA_GLSL

// Pushed once at bootstrap by CloudRenderSystem. u_cloudHorizonDistance /
// u_cloudTransitionStart drive the overhead cloud object alpha fade-out —
// real cloud objects hold true size the whole time they're visible and
// only fade near the edge of the simulated weather radius, never shrink.
// u_cloudMinScale / u_cloudMaxScale are reused as a distance-based detail
// factor for those same objects (see VolumetricCloudUtility.glsl) rather
// than a geometric scale. u_cloudSkyViewDistance bounds the sky dome's own
// distant weather preview and is never read by the per-instance shader.
layout(std140) uniform CloudSettingsData {
    float u_cloudHorizonDistance;
    float u_cloudMinScale;
    float u_cloudMaxScale;
    float u_cloudSkyViewDistance;
    float u_cloudTransitionStart;
};

#endif