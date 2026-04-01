package com.internal.core.settings;

import com.internal.core.input.Input;

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

    // Input Bindings — Player Movement
    public int keyForward;
    public int keyBack;
    public int keyLeft;
    public int keyRight;
    public int keyJump;
    public int keyWalk;
    public int keySprint;

    // Input Bindings — Player Actions
    public int keyInventory;

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

        // Input Bindings — Player Movement
        this.keyForward = builder.keyForward;
        this.keyBack = builder.keyBack;
        this.keyLeft = builder.keyLeft;
        this.keyRight = builder.keyRight;
        this.keyJump = builder.keyJump;
        this.keyWalk = builder.keyWalk;
        this.keySprint = builder.keySprint;

        // Input Bindings — Player Actions
        this.keyInventory = builder.keyInventory;
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

        // Input Bindings — Player Movement
        public int keyForward = Input.Keys.W;
        public int keyBack = Input.Keys.S;
        public int keyLeft = Input.Keys.A;
        public int keyRight = Input.Keys.D;
        public int keyJump = Input.Keys.SPACE;
        public int keyWalk = Input.Keys.CONTROL_LEFT;
        public int keySprint = Input.Keys.SHIFT_LEFT;

        // Input Bindings — Player Actions
        public int keyInventory = Input.Keys.I;

        // Window \\

        public Builder FOV(float FOV) {
            this.FOV = FOV;
            return this;
        }

        public Builder windowWidth(int windowWidth) {
            this.windowWidth = windowWidth;
            return this;
        }

        public Builder windowHeight(int windowHeight) {
            this.windowHeight = windowHeight;
            return this;
        }

        public Builder windowX(int windowX) {
            this.windowX = windowX;
            return this;
        }

        public Builder windowY(int windowY) {
            this.windowY = windowY;
            return this;
        }

        public Builder fullscreen(boolean fullscreen) {
            this.fullscreen = fullscreen;
            return this;
        }

        // Render \\

        public Builder maxRenderDistance(int maxRenderDistance) {
            this.maxRenderDistance = maxRenderDistance;
            return this;
        }

        // Input \\

        public Builder mouseSensitivity(float mouseSensitivity) {
            this.mouseSensitivity = mouseSensitivity;
            return this;
        }

        public Builder keyForward(int key) {
            this.keyForward = key;
            return this;
        }

        public Builder keyBack(int key) {
            this.keyBack = key;
            return this;
        }

        public Builder keyLeft(int key) {
            this.keyLeft = key;
            return this;
        }

        public Builder keyRight(int key) {
            this.keyRight = key;
            return this;
        }

        public Builder keyJump(int key) {
            this.keyJump = key;
            return this;
        }

        public Builder keyWalk(int key) {
            this.keyWalk = key;
            return this;
        }

        public Builder keySprint(int key) {
            this.keySprint = key;
            return this;
        }

        public Builder keyInventory(int key) {
            this.keyInventory = key;
            return this;
        }

        // Build \\

        public Settings build() {
            return new Settings(this);
        }
    }
}