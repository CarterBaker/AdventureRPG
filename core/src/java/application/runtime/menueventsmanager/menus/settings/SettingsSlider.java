package application.runtime.menueventsmanager.menus.settings;

import application.runtime.RuntimeSetting;

public enum SettingsSlider {

    /*
     * The settings the menu shows as sliders. Each slider's name rides as its
     * drag argument, so the one drag handler knows which setting the track it
     * received controls. A post-processing slider's value is pushed to the
     * GPU as it moves, and so is a render slider's, through the settings
     * every world shader reads; the rest are read live where they are used,
     * the volumes by SoundManager.
     */

    MOUSE_SENSITIVITY(RuntimeSetting.SETTINGS_SLIDER_MOUSE_SENSITIVITY, false, false),
    OUTLINE_STRENGTH(RuntimeSetting.SETTINGS_SLIDER_OUTLINE_STRENGTH, true, false),
    DEPTH_OF_FIELD_STRENGTH(RuntimeSetting.SETTINGS_SLIDER_DEPTH_OF_FIELD_STRENGTH, true, false),
    DEPTH_OF_FIELD_BLUR(RuntimeSetting.SETTINGS_SLIDER_DEPTH_OF_FIELD_BLUR, true, false),
    DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH(RuntimeSetting.SETTINGS_SLIDER_DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH, false, false),
    BLOOM_INTENSITY(RuntimeSetting.SETTINGS_SLIDER_BLOOM_INTENSITY, true, false),
    BRIGHTNESS(RuntimeSetting.SETTINGS_SLIDER_BRIGHTNESS, true, false),
    CONTRAST(RuntimeSetting.SETTINGS_SLIDER_CONTRAST, true, false),
    SATURATION(RuntimeSetting.SETTINGS_SLIDER_SATURATION, true, false),
    VIGNETTE(RuntimeSetting.SETTINGS_SLIDER_VIGNETTE, true, false),
    CHROMATIC_ABERRATION(RuntimeSetting.SETTINGS_SLIDER_CHROMATIC_ABERRATION, true, false),
    FILM_GRAIN(RuntimeSetting.SETTINGS_SLIDER_FILM_GRAIN, true, false),
    CAMERA_SHAKE_INTENSITY(RuntimeSetting.SETTINGS_SLIDER_CAMERA_SHAKE_INTENSITY, false, false),
    MASTER_VOLUME(RuntimeSetting.SETTINGS_SLIDER_MASTER_VOLUME, false, false),
    MUSIC_VOLUME(RuntimeSetting.SETTINGS_SLIDER_MUSIC_VOLUME, false, false),
    EFFECT_VOLUME(RuntimeSetting.SETTINGS_SLIDER_EFFECT_VOLUME, false, false),
    AMBIENT_VOLUME(RuntimeSetting.SETTINGS_SLIDER_AMBIENT_VOLUME, false, false),
    INTERFACE_VOLUME(RuntimeSetting.SETTINGS_SLIDER_INTERFACE_VOLUME, false, false),
    VOICE_VOLUME(RuntimeSetting.SETTINGS_SLIDER_VOICE_VOLUME, false, false);

    // Internal
    private final String label;
    private final boolean postProcessing;
    private final boolean render;

    // Constructor \\

    SettingsSlider(String label, boolean postProcessing, boolean render) {
        this.label = label;
        this.postProcessing = postProcessing;
        this.render = render;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isPostProcessing() {
        return postProcessing;
    }

    public boolean isRender() {
        return render;
    }
}
