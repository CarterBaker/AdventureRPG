package application.runtime.menueventsmanager.menus.settings;

import application.runtime.RuntimeSetting;
import engine.input.Binding;
import engine.settings.KeyBindings;

public enum SettingsBinding {

    /*
     * The game actions the Controls tab lets a player rebind, in the order
     * they are listed and grouped under their section. Primary stays fixed —
     * it is the button that clicks menus and attacks, so it can never be
     * rebound away. Block is the Secondary binding, and Activate also sets
     * down whatever is held. The cycle and level bindings step through a
     * list of choices and up and down a stack of levels wherever a menu
     * offers them, and the turn bindings turn what is about to be set down
     * either way, so a table can be worked without a pointer. Each binding
     * names the scopes it is read in: the world's while walking about, the
     * music's while an instrument is played, Play Instrument both, as it
     * enters play mode and leaves it. Only bindings sharing a scope swap keys
     * when rebound, so a note key may be a movement key.
     * Editor bindings are not listed; they stay with the editor.
     */

    MOVE_FORWARD(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_MOVE_FORWARD,
            KeyBindings.MOVE_FORWARD, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    MOVE_BACK(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_MOVE_BACK,
            KeyBindings.MOVE_BACK, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    MOVE_LEFT(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_MOVE_LEFT,
            KeyBindings.MOVE_LEFT, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    MOVE_RIGHT(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_MOVE_RIGHT,
            KeyBindings.MOVE_RIGHT, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    JUMP(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_JUMP,
            KeyBindings.JUMP, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    WALK(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_WALK,
            KeyBindings.WALK, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    SPRINT(RuntimeSetting.SETTINGS_SECTION_MOVEMENT, RuntimeSetting.SETTINGS_BINDING_SPRINT,
            KeyBindings.SPRINT, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    BLOCK(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_BLOCK,
            KeyBindings.SECONDARY, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    ACTIVATE(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_ACTIVATE,
            KeyBindings.ACTIVATE, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    INVENTORY(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_INVENTORY,
            KeyBindings.INVENTORY, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    ROTATE_ITEM(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_ROTATE_ITEM,
            KeyBindings.ROTATE_ITEM, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    TURN_LEFT(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_TURN_LEFT,
            KeyBindings.TURN_LEFT, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    TURN_RIGHT(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_TURN_RIGHT,
            KeyBindings.TURN_RIGHT, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    CYCLE_PREVIOUS(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_CYCLE_PREVIOUS,
            KeyBindings.CYCLE_PREVIOUS, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    CYCLE_NEXT(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_CYCLE_NEXT,
            KeyBindings.CYCLE_NEXT, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    LEVEL_UP(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_LEVEL_UP,
            KeyBindings.LEVEL_UP, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    LEVEL_DOWN(RuntimeSetting.SETTINGS_SECTION_ACTIONS, RuntimeSetting.SETTINGS_BINDING_LEVEL_DOWN,
            KeyBindings.LEVEL_DOWN, RuntimeSetting.SETTINGS_SCOPE_WORLD),
    PLAY_INSTRUMENT(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_PLAY_INSTRUMENT,
            KeyBindings.PLAY_INSTRUMENT, RuntimeSetting.SETTINGS_SCOPE_WORLD | RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    SONGBOOK(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_SONGBOOK,
            KeyBindings.SONGBOOK, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    LEAVE_INSTRUMENT(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_LEAVE_INSTRUMENT,
            KeyBindings.LEAVE_INSTRUMENT, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_C(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_C,
            KeyBindings.NOTE_C, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_C_SHARP(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_C_SHARP,
            KeyBindings.NOTE_C_SHARP, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_D(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_D,
            KeyBindings.NOTE_D, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_D_SHARP(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_D_SHARP,
            KeyBindings.NOTE_D_SHARP, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_E(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_E,
            KeyBindings.NOTE_E, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_F(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_F,
            KeyBindings.NOTE_F, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_F_SHARP(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_F_SHARP,
            KeyBindings.NOTE_F_SHARP, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_G(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_G,
            KeyBindings.NOTE_G, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_G_SHARP(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_G_SHARP,
            KeyBindings.NOTE_G_SHARP, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_A(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_A,
            KeyBindings.NOTE_A, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_A_SHARP(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_A_SHARP,
            KeyBindings.NOTE_A_SHARP, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_B(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_B,
            KeyBindings.NOTE_B, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    NOTE_HIGH_C(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_NOTE_HIGH_C,
            KeyBindings.NOTE_HIGH_C, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    OCTAVE_DOWN(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_OCTAVE_DOWN,
            KeyBindings.OCTAVE_DOWN, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    OCTAVE_UP(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_OCTAVE_UP,
            KeyBindings.OCTAVE_UP, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    SOFTER(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_SOFTER,
            KeyBindings.SOFTER, RuntimeSetting.SETTINGS_SCOPE_MUSIC),
    LOUDER(RuntimeSetting.SETTINGS_SECTION_MUSIC, RuntimeSetting.SETTINGS_BINDING_LOUDER,
            KeyBindings.LOUDER, RuntimeSetting.SETTINGS_SCOPE_MUSIC);

    // Values
    public static final SettingsBinding[] VALUES = values();

    // Internal
    private final String section;
    private final String label;
    private final Binding binding;
    private final int scopes;

    // Constructor \\

    SettingsBinding(String section, String label, Binding binding, int scopes) {
        this.section = section;
        this.label = label;
        this.binding = binding;
        this.scopes = scopes;
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

    // Whether the two are read in a scope they share, so one key cannot serve both
    public boolean sharesScope(SettingsBinding settingsBinding) {
        return (scopes & settingsBinding.scopes) != 0;
    }
}
