package editor.runtime.menueventsmanager.menus.settings;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.menus.settings.SettingsBinding;
import application.runtime.menueventsmanager.menus.settings.SettingsOption;
import application.runtime.menueventsmanager.menus.settings.SettingsSlider;
import application.runtime.menueventsmanager.menus.settings.SettingsValueUtility;
import editor.runtime.EditorSetting;
import engine.input.Input;
import engine.input.InputCode;
import engine.input.InputNameUtility;
import engine.root.BranchPackage;

public class EditorSettingsOptionBranch extends BranchPackage {

    /*
     * Fills the settings dialog's General page and its Dev Mode pages, and
     * writes each change into Settings through SettingsValueUtility, the
     * model the game's own settings menu shares, so a setting behaves the
     * same wherever it is changed. General sets the editor's own window; the
     * Dev Mode pages set what every Dev Mode and Preview tab runs with. A
     * world setting only marks the dialog, which applies it once on closing.
     * Clicking a control row waits for the next key or mouse button pressed;
     * Escape cancels the wait, and an action given a key another holds swaps
     * keys with it.
     */

    // Internal
    private SettingsSystem settingsSystem;
    private EditorSettingsBranch editorSettingsBranch;

    // Colors
    private MenuColorStruct captureColor;

    // Capture
    private SettingsBinding capturingBinding;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.captureColor = new MenuColorStruct(ThemeColor.ACCENT, EditorSetting.SETTINGS_CAPTURE_ALPHA);
    }

    @Override
    protected void get() {
        this.settingsSystem = get(SettingsSystem.class);
        this.editorSettingsBranch = get(EditorSettingsBranch.class);
    }

    // Populate \\

    void populateGeneral() {
        editorSettingsBranch.injectSectionHeader(RuntimeSetting.SETTINGS_SECTION_WINDOW);
        injectOptionRow(SettingsOption.FULLSCREEN);
        injectOptionRow(SettingsOption.VSYNC);
        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_WINDOW);
        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_SAVED);
    }

    void populateGraphics() {

        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_DEV_MODE);

        editorSettingsBranch.injectSectionHeader(RuntimeSetting.SETTINGS_SECTION_VIEW);
        injectOptionRow(SettingsOption.FIELD_OF_VIEW);

        editorSettingsBranch.injectSectionHeader(RuntimeSetting.SETTINGS_SECTION_WORLD);
        injectOptionRow(SettingsOption.RENDER_DISTANCE);
        injectOptionRow(SettingsOption.TERRAIN_DETAIL);
        injectOptionRow(SettingsOption.TREE_DETAIL);
        injectOptionRow(SettingsOption.MACRO_TERRAIN);
        editorSettingsBranch.injectNote(RuntimeSetting.SETTINGS_NOTE_MACRO_TERRAIN);
        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_WORLD_APPLY);

        editorSettingsBranch.injectSectionHeader(EditorSetting.SETTINGS_SECTION_PREVIEW_WORLD);
        injectOptionRow(SettingsOption.MENU_CHUNK_STREAMING);
        injectOptionRow(SettingsOption.MENU_RENDER_DISTANCE);
        injectOptionRow(SettingsOption.MENU_TREE_DETAIL);
        injectOptionRow(SettingsOption.MENU_MACRO_TERRAIN);
        editorSettingsBranch.injectNote(RuntimeSetting.SETTINGS_NOTE_MENU_WORLD);
        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_WORLD_APPLY);
    }

    void populateEffects() {

        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_DEV_MODE);

        editorSettingsBranch.injectSectionHeader(RuntimeSetting.SETTINGS_SECTION_OUTLINES);
        injectOptionRow(SettingsOption.OUTLINES);
        injectOptionRow(SettingsOption.OUTLINE_THICKNESS);
        injectSlider(SettingsSlider.OUTLINE_STRENGTH);

        editorSettingsBranch.injectSectionHeader(RuntimeSetting.SETTINGS_SECTION_DEPTH_OF_FIELD);
        injectOptionRow(SettingsOption.DEPTH_OF_FIELD);
        injectSlider(SettingsSlider.DEPTH_OF_FIELD_STRENGTH);
        injectSlider(SettingsSlider.DEPTH_OF_FIELD_BLUR);
        editorSettingsBranch.injectNote(RuntimeSetting.SETTINGS_NOTE_AUTO_FOCUS);
        injectOptionRow(SettingsOption.DEPTH_OF_FIELD_THIRD_PERSON);
        injectSlider(SettingsSlider.DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH);
        editorSettingsBranch.injectNote(RuntimeSetting.SETTINGS_NOTE_THIRD_PERSON_FOCUS);

        editorSettingsBranch.injectSectionHeader(RuntimeSetting.SETTINGS_SECTION_IMAGE);
        injectOptionRow(SettingsOption.ANTI_ALIASING);
        injectOptionRow(SettingsOption.BLOOM);
        injectSlider(SettingsSlider.BLOOM_INTENSITY);

        editorSettingsBranch.injectSectionHeader(RuntimeSetting.SETTINGS_SECTION_COLOR);
        injectSlider(SettingsSlider.BRIGHTNESS);
        injectSlider(SettingsSlider.CONTRAST);
        injectSlider(SettingsSlider.SATURATION);

        editorSettingsBranch.injectSectionHeader(RuntimeSetting.SETTINGS_SECTION_LENS);
        injectSlider(SettingsSlider.VIGNETTE);
        injectSlider(SettingsSlider.CHROMATIC_ABERRATION);
        injectSlider(SettingsSlider.FILM_GRAIN);
    }

    void populateGameplay() {

        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_DEV_MODE);

        editorSettingsBranch.injectSectionHeader(RuntimeSetting.SETTINGS_SECTION_CAMERA);
        injectOptionRow(SettingsOption.CAMERA_SHAKE);
        injectSlider(SettingsSlider.CAMERA_SHAKE_INTENSITY);
        injectOptionRow(SettingsOption.HEAD_BOB);
        editorSettingsBranch.injectNote(RuntimeSetting.SETTINGS_NOTE_HEAD_BOB);

        editorSettingsBranch.injectSectionHeader(RuntimeSetting.SETTINGS_SECTION_MOUSE);
        injectSlider(SettingsSlider.MOUSE_SENSITIVITY);
    }

    void populateControls() {

        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_DEV_MODE);

        String section = null;

        for (SettingsBinding settingsBinding : SettingsBinding.VALUES) {

            if (!settingsBinding.getSection().equals(section)) {
                section = settingsBinding.getSection();
                editorSettingsBranch.injectSectionHeader(section);
            }

            injectBindingRow(settingsBinding);
        }

        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_CONTROLS);
        editorSettingsBranch.injectActionRow(
                EditorSettingsAction.RESET_CONTROLS,
                RuntimeSetting.SETTINGS_ACTION_RESET_BINDINGS);
    }

    // Options \\

    private void injectOptionRow(SettingsOption option) {
        editorSettingsBranch.injectRow(
                EditorSetting.MENU_EDITOR_SETTINGS_OPTION_ROW,
                row -> {
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(option.getLabel());
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_VALUE)
                            .setFontText(SettingsValueUtility.formatOption(settings, option));
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_PREVIOUS).setActionArgOverride(option.name());
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_NEXT).setActionArgOverride(option.name());
                });
    }

    public void previousOption(String optionName) {
        stepOption(SettingsOption.valueOf(optionName), -1);
    }

    public void nextOption(String optionName) {
        stepOption(SettingsOption.valueOf(optionName), 1);
    }

    private void stepOption(SettingsOption option, int step) {

        if (!editorSettingsBranch.isOpen())
            return;

        SettingsValueUtility.stepOption(settingsSystem, settings, option, step);

        if (option.isWorldSetting())
            editorSettingsBranch.markWorldSettingsChanged();

        editorSettingsBranch.refreshOptions();
    }

    // Sliders \\

    private void injectSlider(SettingsSlider slider) {
        editorSettingsBranch.injectRow(
                EditorSetting.MENU_EDITOR_SETTINGS_SLIDER_ROW,
                row -> {
                    ElementInstance track = row.findChildById(EditorSetting.ELEMENT_SETTINGS_SLIDER_TRACK);
                    track.setOnDragArgOverride(slider.name());
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(slider.getLabel());
                    refreshSlider(track, slider);
                });
    }

    public void dragSlider(String sliderName, WindowInstance window, ElementInstance track) {

        if (!editorSettingsBranch.isOpen())
            return;

        SettingsSlider slider = SettingsSlider.valueOf(sliderName);

        SettingsValueUtility.setSliderFraction(
                settingsSystem,
                settings,
                slider,
                editorSettingsBranch.resolveTrackFraction(window, track));

        if (editorSettingsBranch.placeKnob(track, SettingsValueUtility.getSliderFraction(settings, slider)))
            track.findChildById(EditorSetting.ELEMENT_SETTINGS_SLIDER_VALUE)
                    .setFontText(SettingsValueUtility.formatSlider(settings, slider));
    }

    private void refreshSlider(ElementInstance track, SettingsSlider slider) {
        editorSettingsBranch.placeKnob(track, SettingsValueUtility.getSliderFraction(settings, slider));
        track.findChildById(EditorSetting.ELEMENT_SETTINGS_SLIDER_VALUE)
                .setFontText(SettingsValueUtility.formatSlider(settings, slider));
    }

    // Bindings \\

    private void injectBindingRow(SettingsBinding settingsBinding) {

        boolean capturing = capturingBinding == settingsBinding;

        editorSettingsBranch.injectRow(
                EditorSetting.MENU_EDITOR_SETTINGS_BINDING_ROW,
                row -> {
                    row.setActionArgOverride(settingsBinding.name());
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(settingsBinding.getLabel());
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_VALUE).setFontText(capturing
                            ? RuntimeSetting.SETTINGS_BINDING_CAPTURE_PROMPT
                            : InputNameUtility.getName(settingsBinding.getBinding()));

                    if (capturing)
                        row.setColorOverride(captureColor);
                });
    }

    public void selectBinding(String bindingName) {

        if (!editorSettingsBranch.isOpen())
            return;

        this.capturingBinding = SettingsBinding.valueOf(bindingName);
        editorSettingsBranch.setStatus(EditorSetting.SETTINGS_STATUS_CAPTURE_BINDING);
        editorSettingsBranch.refreshOptions();
    }

    void resetBindings() {

        settingsSystem.resetBindings();
        this.capturingBinding = null;

        editorSettingsBranch.setStatus(EditorSetting.SETTINGS_STATUS_CONTROLS_RESET);
        editorSettingsBranch.refreshOptions();
    }

    // Capture \\

    // Takes the key or button pressed for the binding waiting on one; false when none is waiting
    boolean updateCapture(Input rawInput) {

        if (capturingBinding == null)
            return false;

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)) {
            cancelCapture();
            editorSettingsBranch.setStatus(EditorSetting.SETTINGS_STATUS_NONE);
            editorSettingsBranch.refreshOptions();
            return true;
        }

        InputCode pressed = SettingsValueUtility.resolvePressedCode(rawInput);

        if (pressed == null)
            return true;

        SettingsValueUtility.rebind(capturingBinding, pressed);
        cancelCapture();

        editorSettingsBranch.setStatus(EditorSetting.SETTINGS_STATUS_NONE);
        editorSettingsBranch.refreshOptions();
        return true;
    }

    void cancelCapture() {
        this.capturingBinding = null;
    }
}
