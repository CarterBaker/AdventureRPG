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
import engine.root.EngineSetting;

public class SettingsOptionBranch extends BranchPackage {

    /*
     * Fills the Display and Graphics tabs and the Controls tab's mouse
     * section, and writes each change into Settings. Display and field of
     * view take effect at once through SettingsSystem, and sensitivity is
     * read live by InputManager. Render distance and terrain detail reshape
     * how the world streams and shades — a distance change reloads every
     * chunk — so they only mark the session, and SettingsMenuBranch applies
     * them once when the menu closes.
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
        settingsMenuBranch.injectNote(session, RuntimeSetting.SETTINGS_NOTE_WORLD_APPLY);
    }

    void populateMouse(SettingsSessionStruct session) {
        settingsMenuBranch.injectSectionHeader(session, RuntimeSetting.SETTINGS_SECTION_MOUSE);
        injectSlider(session, SettingsSlider.MOUSE_SENSITIVITY);
    }

    // Options \\

    private void injectOptionRow(SettingsSessionStruct session, SettingsOption option) {
        menuManager.inject(
                session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_OPTIONS,
                RuntimeSetting.MENU_SETTINGS_OPTION_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(option.getLabel());
                    row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ROW_VALUE).setFontText(formatOption(option));
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

        applyStep(session, option, step);
        settingsMenuBranch.refreshOptions(session);
    }

    private void applyStep(SettingsSessionStruct session, SettingsOption option, int step) {
        switch (option) {
            case FULLSCREEN -> {
                settings.fullscreen = !settings.fullscreen;
                settingsSystem.applyFullscreen();
            }
            case VSYNC -> {
                settings.vsync = !settings.vsync;
                settingsSystem.applyVsync();
            }
            case FIELD_OF_VIEW -> {
                settings.FOV = Math.clamp(
                        settings.FOV + step * RuntimeSetting.SETTINGS_FIELD_OF_VIEW_STEP,
                        EngineSetting.FIELD_OF_VIEW_MIN,
                        EngineSetting.FIELD_OF_VIEW_MAX);
                settingsSystem.applyFieldOfView();
            }
            case RENDER_DISTANCE -> {
                settings.maxRenderDistance = Math.clamp(
                        settings.maxRenderDistance + step * RuntimeSetting.SETTINGS_RENDER_DISTANCE_STEP,
                        EngineSetting.RENDER_DISTANCE_MIN,
                        EngineSetting.RENDER_DISTANCE_MAX);
                session.markRenderSettingsChanged();
            }
            case TERRAIN_DETAIL -> {
                settings.nearTessellationRadius = Math.clamp(
                        settings.nearTessellationRadius + step * RuntimeSetting.SETTINGS_TERRAIN_DETAIL_STEP,
                        EngineSetting.NEAR_TESSELLATION_RADIUS_MIN,
                        EngineSetting.NEAR_TESSELLATION_RADIUS_MAX);
                session.markRenderSettingsChanged();
            }
        }
    }

    private String formatOption(SettingsOption option) {
        return switch (option) {
            case FULLSCREEN -> formatToggle(settings.fullscreen);
            case VSYNC -> formatToggle(settings.vsync);
            case FIELD_OF_VIEW -> String.format(RuntimeSetting.SETTINGS_FORMAT_FIELD_OF_VIEW, settings.FOV);
            case RENDER_DISTANCE -> String.format(
                    RuntimeSetting.SETTINGS_FORMAT_CHUNKS,
                    settings.maxRenderDistance / RuntimeSetting.SETTINGS_RENDER_DISTANCE_PER_RADIUS);
            case TERRAIN_DETAIL -> String.format(
                    RuntimeSetting.SETTINGS_FORMAT_CHUNKS,
                    settings.nearTessellationRadius);
        };
    }

    private String formatToggle(boolean enabled) {
        return enabled ? RuntimeSetting.SETTINGS_VALUE_ON : RuntimeSetting.SETTINGS_VALUE_OFF;
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

        applyFraction(slider, Math.clamp(fraction, 0f, 1f));
        refreshSlider(track, slider);
    }

    private void refreshSlider(ElementInstance track, SettingsSlider slider) {

        float min = resolveMin(slider);
        float range = resolveMax(slider) - min;
        float fraction = range > 0f ? (resolveValue(slider) - min) / range : 0f;

        track.findChildById(RuntimeSetting.ELEMENT_SETTINGS_SLIDER_KNOB).setPositionOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(fraction * RuntimeSetting.SETTINGS_SLIDER_PERCENT_SCALE),
                DimensionValueStruct.ofAbsolute(0f)));
        track.findChildById(RuntimeSetting.ELEMENT_SETTINGS_SLIDER_VALUE).setFontText(formatSlider(slider));
    }

    private void applyFraction(SettingsSlider slider, float fraction) {

        float min = resolveMin(slider);
        float value = min + (resolveMax(slider) - min) * fraction;

        switch (slider) {
            case MOUSE_SENSITIVITY -> settings.mouseSensitivity = value;
        }
    }

    private float resolveValue(SettingsSlider slider) {
        return switch (slider) {
            case MOUSE_SENSITIVITY -> settings.mouseSensitivity;
        };
    }

    private float resolveMin(SettingsSlider slider) {
        return switch (slider) {
            case MOUSE_SENSITIVITY -> EngineSetting.MOUSE_SENSITIVITY_MIN;
        };
    }

    private float resolveMax(SettingsSlider slider) {
        return switch (slider) {
            case MOUSE_SENSITIVITY -> EngineSetting.MOUSE_SENSITIVITY_MAX;
        };
    }

    private String formatSlider(SettingsSlider slider) {
        return switch (slider) {
            case MOUSE_SENSITIVITY -> String.format(RuntimeSetting.SETTINGS_FORMAT_SENSITIVITY, resolveValue(slider));
        };
    }
}
