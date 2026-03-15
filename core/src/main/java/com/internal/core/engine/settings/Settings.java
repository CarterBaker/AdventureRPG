package com.internal.core.engine.settings;

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

        // Build \\

        public Settings build() {
            return new Settings(this);
        }
    }
}