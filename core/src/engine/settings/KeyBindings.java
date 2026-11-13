package engine.settings;

import engine.input.Binding;
import engine.input.Buttons;
import engine.input.InputCode;
import engine.input.Keys;

public final class KeyBindings {

    /*
     * Runtime key binding state. Holds all active Binding objects for the
     * session. Initialized with engine defaults at startup — overwritten by
     * SettingsUtility.applyBindings() once Settings are loaded. Primary and
     * Pause are never loaded or rebound — they are how a player reaches and
     * clicks the menus, so they can never be bound away. The music bindings
     * are read only while an instrument is played, when nothing else reads
     * the keys, so their keys may be the ones movement uses.
     */

    // Movement
    public static Binding MOVE_FORWARD = new Binding(InputCode.key(Keys.W));
    public static Binding MOVE_BACK = new Binding(InputCode.key(Keys.S));
    public static Binding MOVE_LEFT = new Binding(InputCode.key(Keys.A));
    public static Binding MOVE_RIGHT = new Binding(InputCode.key(Keys.D));
    public static Binding JUMP = new Binding(InputCode.key(Keys.SPACE));
    public static Binding WALK = new Binding(InputCode.key(Keys.CONTROL_LEFT));
    public static Binding SPRINT = new Binding(InputCode.key(Keys.SHIFT_LEFT));

    // Mouse — Primary attacks, Secondary raises a guard
    public static Binding PRIMARY = new Binding(InputCode.mouse(Buttons.LEFT));
    public static Binding SECONDARY = new Binding(InputCode.mouse(Buttons.RIGHT));
    public static Binding LOOK = new Binding(InputCode.mouse(Buttons.RIGHT));

    // Interaction
    public static Binding ACTIVATE = new Binding(InputCode.key(Keys.E));

    // Menu
    public static Binding PAUSE = new Binding(InputCode.key(Keys.ESCAPE));

    // Inventory
    public static Binding INVENTORY = new Binding(InputCode.key(Keys.I));
    public static Binding ROTATE_ITEM = new Binding(InputCode.key(Keys.R));
    public static Binding TURN_LEFT = new Binding(InputCode.key(Keys.COMMA));
    public static Binding TURN_RIGHT = new Binding(InputCode.key(Keys.PERIOD));

    // Cycling — step through a list of choices, or up and down a stack of levels
    public static Binding CYCLE_PREVIOUS = new Binding(InputCode.key(Keys.Q));
    public static Binding CYCLE_NEXT = new Binding(InputCode.key(Keys.F));
    public static Binding LEVEL_UP = new Binding(InputCode.key(Keys.PAGE_UP));
    public static Binding LEVEL_DOWN = new Binding(InputCode.key(Keys.PAGE_DOWN));

    // Music — play mode, opened with an instrument held: the Songbook, and leaving
    public static Binding PLAY_INSTRUMENT = new Binding(InputCode.key(Keys.B));
    public static Binding SONGBOOK = new Binding(InputCode.key(Keys.N));
    public static Binding LEAVE_INSTRUMENT = new Binding(InputCode.key(Keys.ESCAPE));

    // Music — the thirteen note keys from C to the next C, laid out as Musical Typing, then octave and loudness
    public static Binding NOTE_C = new Binding(InputCode.key(Keys.A));
    public static Binding NOTE_C_SHARP = new Binding(InputCode.key(Keys.W));
    public static Binding NOTE_D = new Binding(InputCode.key(Keys.S));
    public static Binding NOTE_D_SHARP = new Binding(InputCode.key(Keys.E));
    public static Binding NOTE_E = new Binding(InputCode.key(Keys.D));
    public static Binding NOTE_F = new Binding(InputCode.key(Keys.F));
    public static Binding NOTE_F_SHARP = new Binding(InputCode.key(Keys.T));
    public static Binding NOTE_G = new Binding(InputCode.key(Keys.G));
    public static Binding NOTE_G_SHARP = new Binding(InputCode.key(Keys.Y));
    public static Binding NOTE_A = new Binding(InputCode.key(Keys.H));
    public static Binding NOTE_A_SHARP = new Binding(InputCode.key(Keys.U));
    public static Binding NOTE_B = new Binding(InputCode.key(Keys.J));
    public static Binding NOTE_HIGH_C = new Binding(InputCode.key(Keys.K));
    public static Binding OCTAVE_DOWN = new Binding(InputCode.key(Keys.Z));
    public static Binding OCTAVE_UP = new Binding(InputCode.key(Keys.X));
    public static Binding SOFTER = new Binding(InputCode.key(Keys.C));
    public static Binding LOUDER = new Binding(InputCode.key(Keys.V));

    // Music — the note keys in order, each the key one semitone above the last
    public static final Binding[] NOTES = {
            NOTE_C, NOTE_C_SHARP, NOTE_D, NOTE_D_SHARP, NOTE_E, NOTE_F, NOTE_F_SHARP,
            NOTE_G, NOTE_G_SHARP, NOTE_A, NOTE_A_SHARP, NOTE_B, NOTE_HIGH_C };

    // Editor — Single
    public static Binding TOGGLE_INSPECTOR = new Binding(InputCode.key(Keys.I));
    public static Binding FOCUS_SELECTED = new Binding(InputCode.key(Keys.F));
    public static Binding DELETE_SELECTED = new Binding(InputCode.key(Keys.DELETE));

    // Editor — Combos
    public static Binding SAVE = new Binding(InputCode.key(Keys.CONTROL_LEFT), InputCode.key(Keys.S));
    public static Binding UNDO = new Binding(InputCode.key(Keys.CONTROL_LEFT), InputCode.key(Keys.Z));
    public static Binding REDO = new Binding(InputCode.key(Keys.CONTROL_LEFT), InputCode.key(Keys.Y));
    public static Binding DUPLICATE = new Binding(InputCode.key(Keys.CONTROL_LEFT), InputCode.key(Keys.D));
    public static Binding OPEN_CONSOLE = new Binding(InputCode.key(Keys.CONTROL_LEFT), InputCode.key(Keys.GRAVE));
    public static Binding OPEN_PREVIEW = new Binding(InputCode.key(Keys.NUM_1));
}