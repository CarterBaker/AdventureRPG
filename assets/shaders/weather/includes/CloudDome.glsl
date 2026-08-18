#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

// Bends a cloud entry's authored altitude down toward the world's fixed
// sea level — never the camera's own eye level — as the view ray tilts
// toward the horizon, purely as a function of the ray's elevation angle.
// Anchoring to one absolute elevation means the horizon reads the same
// way no matter how high or low the camera is standing: distant clouds
// always rise up out of the ocean and arc overhead to their true
// altitude, with no seam at any view angle, including straight up, where
// elevation is exactly 1 and the authored altitude comes through
// completely unbent.

// Must mirror EngineSetting.WEATHER_SEA_LEVEL_BLOCKS — GLSL has no
// visibility into the Java constant, the same convention WaterShader.vsh
// already uses for its own LIQUID_LEVEL_MAX mirror.
const float CLOUD_DOME_SEA_LEVEL_BLOCKS = 512.0;
const float CLOUD_DOME_CURVE_POWER      = 2.2;

float resolveCloudDomeAltitude(float authoredAltitude, float rayDirY) {
    float elevation01 = clamp(rayDirY, 0.0, 1.0);
    float bend         = pow(1.0 - elevation01, CLOUD_DOME_CURVE_POWER);
    return mix(authoredAltitude, CLOUD_DOME_SEA_LEVEL_BLOCKS, bend);
}

#endif