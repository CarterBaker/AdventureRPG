package engine.settings;

import engine.util.input.Keys;

public class Settings {

    /*
     * Runtime configuration for the engine. Populated at startup via
     * Settings.Builder before the engine initialises. Accessible throughout
     * the engine via internal.settings.
     */

    // Debug
    public final boolean debug = true;

    // Window
    public float FOV;
    public int windowWidth;
    public int windowHeight;
    public int windowX;
    public int windowY;
    public boolean fullscreen;

    // Render
    public int maxRenderDistance;

    // Input
    public float mouseSensitivity;

    // Bindings — Game Movement
    public int[] bindMoveForward;
    public int[] bindMoveBack;
    public int[] bindMoveLeft;
    public int[] bindMoveRight;
    public int[] bindJump;
    public int[] bindWalk;
    public int[] bindSprint;

    // Bindings — Game Actions
    public int[] bindInventory;

    // Bindings — Editor Single
    public int[] bindToggleInspector;
    public int[] bindFocusSelected;
    public int[] bindDeleteSelected;

    // Bindings — Editor Combos
    public int[] bindSave;
    public int[] bindUndo;
    public int[] bindRedo;
    public int[] bindDuplicate;
    public int[] bindOpenConsole;

    // Constructor \\

    public Settings(Builder builder) {

        // Window
        this.FOV = builder.FOV;
        this.windowWidth = builder.windowWidth;
        this.windowHeight = builder.windowHeight;
        this.windowX = builder.windowX;
        this.windowY = builder.windowY;
        this.fullscreen = builder.fullscreen;

        // Render
        this.maxRenderDistance = builder.maxRenderDistance;

        // Input
        this.mouseSensitivity = builder.mouseSensitivity;

        // Bindings — Game Movement
        this.bindMoveForward = builder.bindMoveForward;
        this.bindMoveBack = builder.bindMoveBack;
        this.bindMoveLeft = builder.bindMoveLeft;
        this.bindMoveRight = builder.bindMoveRight;
        this.bindJump = builder.bindJump;
        this.bindWalk = builder.bindWalk;
        this.bindSprint = builder.bindSprint;

        // Bindings — Game Actions
        this.bindInventory = builder.bindInventory;

        // Bindings — Editor Single
        this.bindToggleInspector = builder.bindToggleInspector;
        this.bindFocusSelected = builder.bindFocusSelected;
        this.bindDeleteSelected = builder.bindDeleteSelected;

        // Bindings — Editor Combos
        this.bindSave = builder.bindSave;
        this.bindUndo = builder.bindUndo;
        this.bindRedo = builder.bindRedo;
        this.bindDuplicate = builder.bindDuplicate;
        this.bindOpenConsole = builder.bindOpenConsole;
    }

    // Builder \\

    public static class Builder {

        // Window
        public float FOV = 70;
        public int windowWidth = 1280;
        public int windowHeight = 720;
        public int windowX = -1;
        public int windowY = -1;
        public boolean fullscreen = false;

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

        // Window \\

        public Builder FOV(float FOV) {
            this.FOV = FOV;
            return this;
        }

        public Builder windowWidth(int v) {
            this.windowWidth = v;
            return this;
        }

        public Builder windowHeight(int v) {
            this.windowHeight = v;
            return this;
        }

        public Builder windowX(int v) {
            this.windowX = v;
            return this;
        }

        public Builder windowY(int v) {
            this.windowY = v;
            return this;
        }

        public Builder fullscreen(boolean v) {
            this.fullscreen = v;
            return this;
        }

        // Render \\

        public Builder maxRenderDistance(int v) {
            this.maxRenderDistance = v;
            return this;
        }

        // Input \\

        public Builder mouseSensitivity(float v) {
            this.mouseSensitivity = v;
            return this;
        }

        // Bindings \\

        public Builder bindMoveForward(int... keys) {
            this.bindMoveForward = keys;
            return this;
        }

        public Builder bindMoveBack(int... keys) {
            this.bindMoveBack = keys;
            return this;
        }

        public Builder bindMoveLeft(int... keys) {
            this.bindMoveLeft = keys;
            return this;
        }

        public Builder bindMoveRight(int... keys) {
            this.bindMoveRight = keys;
            return this;
        }

        public Builder bindJump(int... keys) {
            this.bindJump = keys;
            return this;
        }

        public Builder bindWalk(int... keys) {
            this.bindWalk = keys;
            return this;
        }

        public Builder bindSprint(int... keys) {
            this.bindSprint = keys;
            return this;
        }

        public Builder bindInventory(int... keys) {
            this.bindInventory = keys;
            return this;
        }

        public Builder bindToggleInspector(int... keys) {
            this.bindToggleInspector = keys;
            return this;
        }

        public Builder bindFocusSelected(int... keys) {
            this.bindFocusSelected = keys;
            return this;
        }

        public Builder bindDeleteSelected(int... keys) {
            this.bindDeleteSelected = keys;
            return this;
        }

        public Builder bindSave(int... keys) {
            this.bindSave = keys;
            return this;
        }

        public Builder bindUndo(int... keys) {
            this.bindUndo = keys;
            return this;
        }

        public Builder bindRedo(int... keys) {
            this.bindRedo = keys;
            return this;
        }

        public Builder bindDuplicate(int... keys) {
            this.bindDuplicate = keys;
            return this;
        }

        public Builder bindOpenConsole(int... keys) {
            this.bindOpenConsole = keys;
            return this;
        }

        public Settings build() {
            return new Settings(this);
        }
    }
}