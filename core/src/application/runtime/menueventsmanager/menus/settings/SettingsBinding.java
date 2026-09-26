package application.runtime.menueventsmanager.menus.settings;

import application.runtime.RuntimeSetting;
import engine.input.Binding;
import engine.settings.KeyBindings;

public enum SettingsBinding {

    /*
     * The game actions the Controls tab lets a player rebind, in the order
     * they are listed and grouped under their section. Primary stays fixed —
     * it is the button that clicks menus, so it can never be rebound away.
     * Editor bindings are not listed; they stay with the editor.
     */

    MOVE_FORWARD(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_MOVE_FORWARD,
            KeyBindings.MOVE_FORWARD),
    MOVE_BACK(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_MOVE_BACK,
            KeyBindings.MOVE_BACK),
    MOVE_LEFT(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_MOVE_LEFT,
            KeyBindings.MOVE_LEFT),
    MOVE_RIGHT(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_MOVE_RIGHT,
            KeyBindings.MOVE_RIGHT),
    JUMP(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_JUMP,
            KeyBindings.JUMP),
    WALK(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_WALK,
            KeyBindings.WALK),
    SPRINT(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_SPRINT,
            KeyBindings.SPRINT),
    USE(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_USE,
            KeyBindings.SECONDARY),
    INVENTORY(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_INVENTORY,
            KeyBindings.INVENTORY),
    ROTATE_ITEM(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_ROTATE_ITEM,
            KeyBindings.ROTATE_ITEM),
    SCREENSHOT(RuntimeSetting.SETTINGS_SECTION_CAPTURE, RuntimeSetting.SETTINGS_BINDING_SCREENSHOT,
            KeyBindings.SCREENSHOT),
    RECORD_VIDEO(RuntimeSetting.SETTINGS_SECTION_CAPTURE, RuntimeSetting.SETTINGS_BINDING_RECORD_VIDEO,
            KeyBindings.RECORD_VIDEO);

    // Values
    public static final SettingsBinding[] VALUES = values();

    // Internal
    private final String section;
    private final String label;
    private final Binding binding;

    // Constructor \\

    SettingsBinding(String section, String label, Binding binding) {
        this.section = section;
        this.label = label;
        this.binding = binding;
    }

    // Accessible \\

    public String getSection() {
        return section;
    }

    public String getLabel() {
        return label;
    }

    public Binding getBinding() {
        return binding;
    }
}
