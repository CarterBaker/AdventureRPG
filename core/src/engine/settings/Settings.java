package engine.settings;

import engine.input.Buttons;
import engine.input.InputCode;
import engine.input.Keys;
import engine.root.EngineSetting;

public class Settings {

    /*
     * User-configurable runtime values. Serialized to and from disk via
     * SettingsUtility. Fields are public and mutable — patched at runtime and
     * flushed on close or settings change. Bindings hold stored input codes,
     * so a binding may be a key or a mouse button.
     */

    // Debug
    public boolean debug = true;

    // Window
    public float FOV = 70;
    public int windowWidth = 1280;
    public int windowHeight = 720;
    public int windowX = EngineSetting.WINDOW_POSITION_UNSET;
    public int windowY = EngineSetting.WINDOW_POSITION_UNSET;
    public boolean windowMaximized;
    public boolean fullscreen;
    public boolean vsync = true;

    // Render
    public int maxRenderDistance = 64;
    public int nearTessellationRadius = 5;

    // Input
    public float mouseSensitivity = 0.15f;

    // Editor — Interface Colors
    public float[] uiColorBackground = { 0.075f, 0.082f, 0.098f, 1.0f };
    public float[] uiColorPanel = { 0.118f, 0.129f, 0.153f, 1.0f };
    public float[] uiColorHeader = { 0.153f, 0.165f, 0.192f, 1.0f };
    public float[] uiColorControl = { 0.204f, 0.220f, 0.259f, 1.0f };
    public float[] uiColorControlHover = { 0.263f, 0.282f, 0.333f, 1.0f };
    public float[] uiColorAccent = { 0.298f, 0.553f, 0.965f, 1.0f };
    public float[] uiColorAccentHover = { 0.420f, 0.643f, 0.984f, 1.0f };
    public float[] uiColorOutline = { 0.231f, 0.251f, 0.294f, 1.0f };
    public float[] uiColorShadow = { 0.0f, 0.0f, 0.0f, 0.55f };
    public float[] uiColorText = { 0.886f, 0.898f, 0.925f, 1.0f };
    public float[] uiColorTextMuted = { 0.576f, 0.604f, 0.659f, 1.0f };
    public float[] uiColorTextOnAccent = { 1.0f, 1.0f, 1.0f, 1.0f };
    public float[] uiColorDanger = { 0.898f, 0.302f, 0.302f, 1.0f };

    // Bindings — Game Movement
    public int[] bindMoveForward = { Keys.W };
    public int[] bindMoveBack = { Keys.S };
    public int[] bindMoveLeft = { Keys.A };
    public int[] bindMoveRight = { Keys.D };
    public int[] bindJump = { Keys.SPACE };
    public int[] bindWalk = { Keys.CONTROL_LEFT };
    public int[] bindSprint = { Keys.SHIFT_LEFT };

    // Bindings — Game Actions
    public int[] bindSecondary = { InputCode.storedMouseCode(Buttons.RIGHT) };

    // Bindings — Screen Capture
    public int[] bindScreenshot = { Keys.F12 };
    public int[] bindRecordVideo = { Keys.F9 };

    // Bindings — Editor Single
    public int[] bindToggleInspector = { Keys.I };
    public int[] bindFocusSelected = { Keys.F };
    public int[] bindDeleteSelected = { Keys.DELETE };

    // Bindings — Editor Combos
    public int[] bindSave = { Keys.CONTROL_LEFT, Keys.S };
    public int[] bindUndo = { Keys.CONTROL_LEFT, Keys.Z };
    public int[] bindRedo = { Keys.CONTROL_LEFT, Keys.Y };
    public int[] bindDuplicate = { Keys.CONTROL_LEFT, Keys.D };
    public int[] bindOpenConsole = { Keys.CONTROL_LEFT, Keys.GRAVE };
}