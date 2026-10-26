package application.runtime.menueventsmanager.menus.settings;

import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.runtime.RuntimeSetting;
import engine.input.Binding;
import engine.input.BindingType;
import engine.input.Input;
import engine.input.InputCode;
import engine.input.InputNameUtility;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.settings.KeyBindings;
import engine.settings.Settings;

public class SettingsValueUtility extends EngineUtility {

    /*
     * The one model behind every settings menu: how each option steps and
     * reads out, how each slider maps a share of its track to its value, and
     * how a binding takes the key or button pressed for it. The game's
     * settings menu and the editor's settings dialog both route through it,
     * so a setting behaves the same wherever it is changed. A change applies
     * through SettingsSystem at once, except a world setting, which its menu
     * applies once when it closes.
     */

    // Options \\

    public static void stepOption(SettingsSystem settingsSystem, Settings settings, SettingsOption option, int step) {
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
            case RENDER_DISTANCE -> settings.maxRenderDistance = stepRenderDistance(settings.maxRenderDistance, step);
            case TERRAIN_DETAIL -> settings.detailRadius = Math.clamp(
                    settings.detailRadius + step * RuntimeSetting.SETTINGS_TERRAIN_DETAIL_STEP,
                    EngineSetting.DETAIL_RADIUS_MIN,
                    EngineSetting.DETAIL_RADIUS_MAX);
            case TREE_DETAIL -> {
                settings.treeDetailDistance = stepTreeDetail(settings.treeDetailDistance, step);
                settingsSystem.applyTreeDetail();
            }
            case MACRO_TERRAIN -> settings.macroTerrain = !settings.macroTerrain;
            case MENU_CHUNK_STREAMING -> settings.menuChunkStreaming = !settings.menuChunkStreaming;
            case MENU_RENDER_DISTANCE -> settings.menuRenderDistance = stepRenderDistance(
                    settings.menuRenderDistance,
                    step);
            case MENU_TREE_DETAIL -> {
                settings.menuTreeDetailDistance = stepTreeDetail(settings.menuTreeDetailDistance, step);
                settingsSystem.applyTreeDetail();
            }
            case MENU_MACRO_TERRAIN -> settings.menuMacroTerrain = !settings.menuMacroTerrain;
            case OUTLINES -> {
                settings.outlines = !settings.outlines;
                settingsSystem.applyPostProcessing();
            }
            case OUTLINE_THICKNESS -> {
                settings.outlineThickness = Math.clamp(
                        settings.outlineThickness + step * RuntimeSetting.SETTINGS_OUTLINE_THICKNESS_STEP,
                        EngineSetting.OUTLINE_THICKNESS_MIN,
                        EngineSetting.OUTLINE_THICKNESS_MAX);
                settingsSystem.applyPostProcessing();
            }
            case DEPTH_OF_FIELD -> {
                settings.depthOfField = !settings.depthOfField;
                settingsSystem.applyPostProcessing();
            }
            case DEPTH_OF_FIELD_THIRD_PERSON -> settings.depthOfFieldThirdPerson = !settings.depthOfFieldThirdPerson;
            case ANTI_ALIASING -> {
                settings.antiAliasing = !settings.antiAliasing;
                settingsSystem.applyPostProcessing();
            }
            case BLOOM -> {
                settings.bloom = !settings.bloom;
                settingsSystem.applyPostProcessing();
            }
            case CAMERA_SHAKE -> settings.cameraShake = !settings.cameraShake;
            case HEAD_BOB -> settings.headBob = !settings.headBob;
        }
    }

    private static int stepRenderDistance(int renderDistance, int step) {
        return Math.clamp(
                renderDistance + step * RuntimeSetting.SETTINGS_RENDER_DISTANCE_STEP,
                EngineSetting.RENDER_DISTANCE_MIN,
                EngineSetting.RENDER_DISTANCE_MAX);
    }

    private static int stepTreeDetail(int treeDetailDistance, int step) {
        return Math.clamp(
                treeDetailDistance + step * RuntimeSetting.SETTINGS_TREE_DETAIL_STEP,
                EngineSetting.TREE_DETAIL_DISTANCE_MIN,
                EngineSetting.TREE_DETAIL_DISTANCE_MAX);
    }

    public static String formatOption(Settings settings, SettingsOption option) {
        return switch (option) {
            case FULLSCREEN -> formatToggle(settings.fullscreen);
            case VSYNC -> formatToggle(settings.vsync);
            case FIELD_OF_VIEW -> String.format(RuntimeSetting.SETTINGS_FORMAT_FIELD_OF_VIEW, settings.FOV);
            case RENDER_DISTANCE -> formatRenderDistance(settings.maxRenderDistance);
            case TERRAIN_DETAIL -> String.format(
                    RuntimeSetting.SETTINGS_FORMAT_BLOCKS,
                    settings.detailRadius);
            case TREE_DETAIL -> String.format(RuntimeSetting.SETTINGS_FORMAT_CHUNKS, settings.treeDetailDistance);
            case MACRO_TERRAIN -> formatToggle(settings.macroTerrain);
            case MENU_CHUNK_STREAMING -> formatToggle(settings.menuChunkStreaming);
            case MENU_RENDER_DISTANCE -> formatRenderDistance(settings.menuRenderDistance);
            case MENU_TREE_DETAIL -> String.format(
                    RuntimeSetting.SETTINGS_FORMAT_CHUNKS,
                    settings.menuTreeDetailDistance);
            case MENU_MACRO_TERRAIN -> formatToggle(settings.menuMacroTerrain);
            case OUTLINES -> formatToggle(settings.outlines);
            case OUTLINE_THICKNESS -> String.format(RuntimeSetting.SETTINGS_FORMAT_PIXELS, settings.outlineThickness);
            case DEPTH_OF_FIELD -> formatToggle(settings.depthOfField);
            case DEPTH_OF_FIELD_THIRD_PERSON -> formatToggle(settings.depthOfFieldThirdPerson);
            case ANTI_ALIASING -> formatToggle(settings.antiAliasing);
            case BLOOM -> formatToggle(settings.bloom);
            case CAMERA_SHAKE -> formatToggle(settings.cameraShake);
            case HEAD_BOB -> formatToggle(settings.headBob);
        };
    }

    private static String formatRenderDistance(int renderDistance) {
        return String.format(
                RuntimeSetting.SETTINGS_FORMAT_CHUNKS,
                renderDistance / RuntimeSetting.SETTINGS_RENDER_DISTANCE_PER_RADIUS);
    }

    private static String formatToggle(boolean enabled) {
        return enabled ? RuntimeSetting.SETTINGS_VALUE_ON : RuntimeSetting.SETTINGS_VALUE_OFF;
    }

    // Sliders \\

    // Sets the slider to a share of its range, pushing a post-processing value to the GPU at once
    public static void setSliderFraction(
            SettingsSystem settingsSystem,
            Settings settings,
            SettingsSlider slider,
            float fraction) {

        float min = resolveMin(slider);
        float value = min + (resolveMax(slider) - min) * Math.clamp(fraction, 0f, 1f);

        switch (slider) {
            case MOUSE_SENSITIVITY -> settings.mouseSensitivity = value;
            case OUTLINE_STRENGTH -> settings.outlineStrength = value;
            case DEPTH_OF_FIELD_STRENGTH -> settings.depthOfFieldStrength = value;
            case DEPTH_OF_FIELD_BLUR -> settings.depthOfFieldBlur = value;
            case DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH -> settings.depthOfFieldThirdPersonStrength = value;
            case BLOOM_INTENSITY -> settings.bloomIntensity = value;
            case BRIGHTNESS -> settings.brightness = value;
            case CONTRAST -> settings.contrast = value;
            case SATURATION -> settings.saturation = value;
            case VIGNETTE -> settings.vignette = value;
            case CHROMATIC_ABERRATION -> settings.chromaticAberration = value;
            case FILM_GRAIN -> settings.filmGrain = value;
            case CAMERA_SHAKE_INTENSITY -> settings.cameraShakeIntensity = value;
        }

        if (slider.isPostProcessing())
            settingsSystem.applyPostProcessing();
    }

    // The share of its range the slider's value sits at
    public static float getSliderFraction(Settings settings, SettingsSlider slider) {

        float min = resolveMin(slider);
        float range = resolveMax(slider) - min;

        return range > 0f ? (resolveValue(settings, slider) - min) / range : 0f;
    }

    public static String formatSlider(Settings settings, SettingsSlider slider) {
        return switch (slider) {
            case MOUSE_SENSITIVITY -> String.format(
                    RuntimeSetting.SETTINGS_FORMAT_SENSITIVITY,
                    resolveValue(settings, slider));
            case OUTLINE_STRENGTH, DEPTH_OF_FIELD_STRENGTH, DEPTH_OF_FIELD_BLUR, DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH,
                    BLOOM_INTENSITY, BRIGHTNESS, CONTRAST, SATURATION, VIGNETTE, CHROMATIC_ABERRATION, FILM_GRAIN,
                    CAMERA_SHAKE_INTENSITY -> String.format(
                            RuntimeSetting.SETTINGS_FORMAT_PERCENT,
                            resolveValue(settings, slider) * RuntimeSetting.SETTINGS_SLIDER_PERCENT_SCALE);
        };
    }

    private static float resolveValue(Settings settings, SettingsSlider slider) {
        return switch (slider) {
            case MOUSE_SENSITIVITY -> settings.mouseSensitivity;
            case OUTLINE_STRENGTH -> settings.outlineStrength;
            case DEPTH_OF_FIELD_STRENGTH -> settings.depthOfFieldStrength;
            case DEPTH_OF_FIELD_BLUR -> settings.depthOfFieldBlur;
            case DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH -> settings.depthOfFieldThirdPersonStrength;
            case BLOOM_INTENSITY -> settings.bloomIntensity;
            case BRIGHTNESS -> settings.brightness;
            case CONTRAST -> settings.contrast;
            case SATURATION -> settings.saturation;
            case VIGNETTE -> settings.vignette;
            case CHROMATIC_ABERRATION -> settings.chromaticAberration;
            case FILM_GRAIN -> settings.filmGrain;
            case CAMERA_SHAKE_INTENSITY -> settings.cameraShakeIntensity;
        };
    }

    private static float resolveMin(SettingsSlider slider) {
        return switch (slider) {
            case MOUSE_SENSITIVITY -> EngineSetting.MOUSE_SENSITIVITY_MIN;
            case OUTLINE_STRENGTH -> EngineSetting.OUTLINE_STRENGTH_MIN;
            case DEPTH_OF_FIELD_STRENGTH -> EngineSetting.DEPTH_OF_FIELD_STRENGTH_MIN;
            case DEPTH_OF_FIELD_BLUR -> EngineSetting.DEPTH_OF_FIELD_BLUR_MIN;
            case DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH -> EngineSetting.DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH_MIN;
            case BLOOM_INTENSITY -> EngineSetting.BLOOM_INTENSITY_MIN;
            case BRIGHTNESS -> EngineSetting.BRIGHTNESS_MIN;
            case CONTRAST -> EngineSetting.CONTRAST_MIN;
            case SATURATION -> EngineSetting.SATURATION_MIN;
            case VIGNETTE -> EngineSetting.VIGNETTE_MIN;
            case CHROMATIC_ABERRATION -> EngineSetting.CHROMATIC_ABERRATION_MIN;
            case FILM_GRAIN -> EngineSetting.FILM_GRAIN_MIN;
            case CAMERA_SHAKE_INTENSITY -> EngineSetting.CAMERA_SHAKE_INTENSITY_MIN;
        };
    }

    private static float resolveMax(SettingsSlider slider) {
        return switch (slider) {
            case MOUSE_SENSITIVITY -> EngineSetting.MOUSE_SENSITIVITY_MAX;
            case OUTLINE_STRENGTH -> EngineSetting.OUTLINE_STRENGTH_MAX;
            case DEPTH_OF_FIELD_STRENGTH -> EngineSetting.DEPTH_OF_FIELD_STRENGTH_MAX;
            case DEPTH_OF_FIELD_BLUR -> EngineSetting.DEPTH_OF_FIELD_BLUR_MAX;
            case DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH -> EngineSetting.DEPTH_OF_FIELD_THIRD_PERSON_STRENGTH_MAX;
            case BLOOM_INTENSITY -> EngineSetting.BLOOM_INTENSITY_MAX;
            case BRIGHTNESS -> EngineSetting.BRIGHTNESS_MAX;
            case CONTRAST -> EngineSetting.CONTRAST_MAX;
            case SATURATION -> EngineSetting.SATURATION_MAX;
            case VIGNETTE -> EngineSetting.VIGNETTE_MAX;
            case CHROMATIC_ABERRATION -> EngineSetting.CHROMATIC_ABERRATION_MAX;
            case FILM_GRAIN -> EngineSetting.FILM_GRAIN_MAX;
            case CAMERA_SHAKE_INTENSITY -> EngineSetting.CAMERA_SHAKE_INTENSITY_MAX;
        };
    }

    // Bindings \\

    // The first named key, or mouse button other than the one that clicks menus, pressed this frame; null for none
    public static InputCode resolvePressedCode(Input rawInput) {

        for (int key = 0; key < EngineSetting.INPUT_KEY_CODE_COUNT; key++) {

            if (!rawInput.isKeyClicked(key))
                continue;

            InputCode inputCode = InputCode.key(key);

            if (InputNameUtility.hasName(inputCode))
                return inputCode;
        }

        for (int button = 0; button < EngineSetting.INPUT_MOUSE_BUTTON_COUNT; button++) {

            if (!rawInput.isMouseClicked(button) || isMenuButton(button))
                continue;

            InputCode inputCode = InputCode.mouse(button);

            if (InputNameUtility.hasName(inputCode))
                return inputCode;
        }

        return null;
    }

    private static boolean isMenuButton(int button) {

        for (InputCode inputCode : KeyBindings.PRIMARY.getCodes())
            if (inputCode.type == BindingType.BUTTON && inputCode.code == button)
                return true;

        return false;
    }

    // Binds the action to the code; another listed action holding it takes the action's old codes, so none is unbound
    public static void rebind(SettingsBinding target, InputCode inputCode) {

        InputCode[] previousCodes = target.getBinding().getCodes();

        for (SettingsBinding settingsBinding : SettingsBinding.VALUES)
            if (settingsBinding != target && isBoundTo(settingsBinding.getBinding(), inputCode))
                settingsBinding.getBinding().set(previousCodes);

        target.getBinding().set(inputCode);
    }

    private static boolean isBoundTo(Binding binding, InputCode inputCode) {

        InputCode[] codes = binding.getCodes();

        return codes.length == 1 && codes[0].matches(inputCode);
    }
}
