#ifndef POST_PROCESS_DATA_GLSL
#define POST_PROCESS_DATA_GLSL
layout(std140) uniform PostProcessData {
    // Source: application Settings — runtime, user-configurable. Pushed by
    // SettingsSystem whenever an effect setting changes.

    // Outlines, inked by the lighting pass
    bool  u_outlineEnabled;
    int   u_outlineThickness;
    float u_outlineStrength;

    // Anti-aliasing
    bool  u_antiAliasing;

    // Depth of field
    bool  u_dofEnabled;
    float u_dofStrength;
    float u_dofBlur;

    // Bloom
    bool  u_bloomEnabled;
    float u_bloomIntensity;

    // Color
    float u_brightness;
    float u_contrast;
    float u_saturation;

    // Lens
    float u_vignette;
    float u_chromaticAberration;
    float u_filmGrain;
};
#endif
