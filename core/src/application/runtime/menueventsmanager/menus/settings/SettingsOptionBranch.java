package application.runtime.menueventsmanager.menus.settings;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;

public class SettingsOptionBranch extends BranchPackage {

    /*
     * Fills the Display, Graphics, Main Menu, Audio and Gameplay tabs and the
     * Controls tab's mouse section, and writes each change into Settings
     * through SettingsValueUtility, the model every settings menu shares.
     * Display, field of view, both tree detail distances and every
     * post-processing effect take effect at once through SettingsSystem, the
     * trees redrawing only those crossing the line, and sensitivity is read
     * live by InputManager, as is the third person depth of field by
     * AutoFocusSystem, the camera's shake and head bob by PlayerManager, and
     * every volume by SoundManager, so a sound playing changes as it moves.
     * Render distance, terrain detail, distant terrain and the main menu's
     * world reshape how the world streams and shades — a distance change
     * reloads every chunk — so they only mark the session, and
     * SettingsMenuBranch applies them once when the menu closes.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private SettingsSystem settingsSystem;
    private SettingsMenuBranch settingsMenuBranch;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.settingsSystem = get(SettingsSystem.class);
        this.settingsMenuBranch = get(SettingsMenuBranch.class);
    }

    // Populate \\

    void populateDisplay(SettingsSessionStruct session) {
        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_WINDOW);
        injectOptionRow(session, SettingsOption.FULLSCREEN);
        injectOptionRow(session, SettingsOption.VSYNC);
    }

    void populateGraphics(SettingsSessionStruct session) {

        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_VIEW);
        injectOptionRow(session, SettingsOption.FIELD_OF_VIEW);

        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_WORLD);
        injectOptionRow(session, SettingsOption.RENDER_DISTANCE);
        injectOptionRow(session, SettingsOption.TERRAIN_DETAIL);
        injectOptionRow(session, SettingsOption.TREE_DETAIL);
        injectOptionRow(session, SettingsOption.MACRO_TERRAIN);
        settingsMenuBranch.injectNote(session, RuntimeSetting.SETTINGS_NOTE_MACRO_TERRAIN);
        settingsMenuBranch.injectNote(session, RuntimeSetting.SETTINGS_NOTE_WORLD_APPLY);

        populateEffects(session);
    }

    void populateMainMenu(SettingsSessionStruct session) {
        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_MENU_WORLD);
        injectOptionRow(session, SettingsOption.MENU_CHUNK_STREAMING);
        injectOptionRow(session, SettingsOption.MENU_RENDER_DISTANCE);
        injectOptionRow(session, SettingsOption.MENU_TREE_DETAIL);
        injectOptionRow(session, SettingsOption.MENU_MACRO_TERRAIN);
        settingsMenuBranch.injectNote(session, RuntimeSetting.SETTINGS_NOTE_MENU_WORLD);
        settingsMenuBranch.injectNote(session, RuntimeSetting.SETTINGS_NOTE_WORLD_APPLY);
    }

    private void populateEffects(SettingsSessionStruct session) {

        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_OUTLINES);
        injectOptionRow(session, SettingsOption.OUTLINES);
        injectOptionRow(session, SettingsOption.OUTLINE_THICKNESS);
        injectSlider(session, SettingsSlider.OUTLINE_STRENGTH);

        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_DEPTH_OF_FIELD);
        injectOptionRow(session, SettingsOption.DEPTH_OF_FIELD);
        injectSlider(session, SettingsSlider.DEPTH_OF_FIELD_STRENGTH);
        injectSlider(session, SettingsSlider.DEPTH_OF_FIELD_BLUR);
        settingsMenuBranch.injectNote(session, RuntimeSetting.SETTINGS_NOTE_AUTO_FOCUS);
        injectOptionRow(session, SettingsOption.DEPTH_OF_FIELD_THIRD_PERSON);
        injectSlider(session, SettingsSlider.DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH);
        settingsMenuBranch.injectNote(session, RuntimeSetting.SETTINGS_NOTE_THIRD_PERSON_FOCUS);

        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_IMAGE);
        injectOptionRow(session, SettingsOption.ANTI_ALIASING);
        injectOptionRow(session, SettingsOption.BLOOM);
        injectSlider(session, SettingsSlider.BLOOM_INTENSITY);

        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_COLOR);
        injectSlider(session, SettingsSlider.BRIGHTNESS);
        injectSlider(session, SettingsSlider.CONTRAST);
        injectSlider(session, SettingsSlider.SATURATION);

        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_LENS);
        injectSlider(session, SettingsSlider.VIGNETTE);
        injectSlider(session, SettingsSlider.CHROMATIC_ABERRATION);
        injectSlider(session, SettingsSlider.FILM_GRAIN);
    }

    void populateMouse(SettingsSessionStruct session) {
        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_MOUSE);
        injectSlider(session, SettingsSlider.MOUSE_SENSITIVITY);
    }

    void populateAudio(SettingsSessionStruct session) {
        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_VOLUME);
        injectSlider(session, SettingsSlider.MASTER_VOLUME);
        injectSlider(session, SettingsSlider.MUSIC_VOLUME);
        injectSlider(session, SettingsSlider.EFFECT_VOLUME);
        injectSlider(session, SettingsSlider.AMBIENT_VOLUME);
        injectSlider(session, SettingsSlider.INTERFACE_VOLUME);
        injectSlider(session, SettingsSlider.VOICE_VOLUME);
    }

    void populateGameplay(SettingsSessionStruct session) {
        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_CAMERA);
        injectOptionRow(session, SettingsOption.CAMERA_SHAKE);
        injectSlider(session, SettingsSlider.CAMERA_SHAKE_INTENSITY);
        injectOptionRow(session, SettingsOption.HEAD_BOB);
        settingsMenuBranch.injectNote(session, RuntimeSetting.SETTINGS_NOTE_HEAD_BOB);
    }

    // Options \\

    private void injectOptionRow(SettingsSessionStruct session, SettingsOption option) {
        menuManager.inject(
                session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_OPTIONS,
                RuntimeSetting.MENU_SETTINGS_OPTION_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(option.getLabel());
                    row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ROW_VALUE)
                            .setFontText(SettingsValueUtility.formatOption(settings, option));
                    row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ROW_PREVIOUS).setActionArgOverride(option.name());
                    row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ROW_NEXT).setActionArgOverride(option.name());
                });
    }

    public void previousOption(String optionName, WindowInstance window) {
        stepOption(window, SettingsOption.valueOf(optionName), -1);
    }

    public void nextOption(String optionName, WindowInstance window) {
        stepOption(window, SettingsOption.valueOf(optionName), 1);
    }

    private void stepOption(WindowInstance window, SettingsOption option, int step) {

        SettingsSessionStruct session = settingsMenuBranch.getSession(window);

        if (session == null)
            return;

        SettingsValueUtility.stepOption(settingsSystem, settings, option, step);

        if (option.isWorldSetting())
            session.markRenderSettingsChanged();

        settingsMenuBranch.refreshOptions(session);
    }

    // Sliders \\

    private void injectSlider(SettingsSessionStruct session, SettingsSlider slider) {
        menuManager.inject(
                session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_OPTIONS,
                RuntimeSetting.MENU_SETTINGS_SLIDER_ROW,
                row -> {
                    ElementInstance track = row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_SLIDER_TRACK);
                    track.setOnDragArgOverride(slider.name());
                    track.findChildById(RuntimeSetting.ELEMENT_SETTINGS_SLIDER_LABEL).setFontText(slider.getLabel());
                    refreshSlider(track, slider);
                });
    }

    public void dragSlider(String sliderName, WindowInstance window, ElementInstance track) {

        if (settingsMenuBranch.getSession(window) == null)
            return;

        SettingsSlider slider = SettingsSlider.valueOf(sliderName);
        float fraction = (inputManager.getHoverMouseX(window) - track.getComputedLeft()) / track.getComputedW();

        SettingsValueUtility.setSliderFraction(settingsSystem, settings, slider, fraction);
        refreshSlider(track, slider);
    }

    private void refreshSlider(ElementInstance track, SettingsSlider slider) {

        float fraction = SettingsValueUtility.getSliderFraction(settings, slider);

        track.findChildById(RuntimeSetting.ELEMENT_SETTINGS_SLIDER_KNOB).setPositionOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(fraction * RuntimeSetting.SETTINGS_SLIDER_PERCENT_SCALE),
                DimensionValueStruct.ofAbsolute(0f)));
        track.findChildById(RuntimeSetting.ELEMENT_SETTINGS_SLIDER_VALUE)
                .setFontText(SettingsValueUtility.formatSlider(settings, slider));
    }
}
