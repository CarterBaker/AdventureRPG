package application.runtime.menueventsmanager.menus.settings;

import application.runtime.RuntimeSetting;

public enum SettingsSlider {

    /*
     * The settings the menu shows as sliders. Each slider's name rides as its
     * drag argument, so the one drag handler knows which setting the track it
     * received controls. A post-processing slider's value is pushed to the
     * GPU as it moves; the rest are read live where they are used.
     */

    MOUSE_SENSITIVITY(RuntimeSetting.SETTINGS_SLIDER_MOUSE_SENSITIVITY, false),
    OUTLINE_STRENGTH(RuntimeSetting.SETTINGS_SLIDER_OUTLINE_STRENGTH, true),
    DEPTH_OF_FIELD_STRENGTH(RuntimeSetting.SETTINGS_SLIDER_DEPTH_OF_FIELD_STRENGTH, true),
    DEPTH_OF_FIELD_BLUR(RuntimeSetting.SETTINGS_SLIDER_DEPTH_OF_FIELD_BLUR, true),
    BLOOM_INTENSITY(RuntimeSetting.SETTINGS_SLIDER_BLOOM_INTENSITY, true),
    BRIGHTNESS(RuntimeSetting.SETTINGS_SLIDER_BRIGHTNESS, true),
    CONTRAST(RuntimeSetting.SETTINGS_SLIDER_CONTRAST, true),
    SATURATION(RuntimeSetting.SETTINGS_SLIDER_SATURATION, true),
    VIGNETTE(RuntimeSetting.SETTINGS_SLIDER_VIGNETTE, true),
    CHROMATIC_ABERRATION(RuntimeSetting.SETTINGS_SLIDER_CHROMATIC_ABERRATION, true),
    FILM_GRAIN(RuntimeSetting.SETTINGS_SLIDER_FILM_GRAIN, true);

    // Internal
    private final String label;
    private final boolean postProcessing;

    // Constructor \\

    SettingsSlider(String label, boolean postProcessing) {
        this.label = label;
        this.postProcessing = postProcessing;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isPostProcessing() {
        return postProcessing;
    }
}
