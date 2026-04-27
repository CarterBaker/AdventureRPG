package engine.settings;

import engine.input.Keys;

public class Settings {

    /*
     * User-configurable runtime values. Serialized to and from disk via
     * LoadUtility. Fields are public and mutable — patched at runtime and
     * flushed on close or settings change.
     */

    // Debug
    public boolean debug = true;

    // Window
    public float FOV = 70;
    public int windowWidth = 1280;
    public int windowHeight = 720;
    public int windowX = -1;
    public int windowY = -1;
    public boolean fullscreen;

    // Render
    public int maxRenderDistance = 64;

    // Input
    public float mouseSensitivity = 0.15f;

    // Bindings — Game Movement
    public int[] bindMoveForward = { Keys.W };
    public int[] bindMoveBack = { Keys.S };
    public int[] bindMoveLeft = { Keys.A };
    public int[] bindMoveRight = { Keys.D };
    public int[] bindJump = { Keys.SPACE };
    public int[] bindWalk = { Keys.CONTROL_LEFT };
    public int[] bindSprint = { Keys.SHIFT_LEFT };

    // Bindings — Game Actions
    public int[] bindInventory = { Keys.I };

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