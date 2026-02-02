package com.internal.core.engine.settings;

public class Settings {

    // Debug Settings
    public final boolean debug = true; // Not accessible through json

    // Runtime Settings \\

    // Window Settings
    public float FOV;
    public int windowWidth;
    public int windowHeight;
    public int windowX;
    public int windowY;
    public boolean fullscreen;

    // Render Settings
    public int maxRenderDistance; // How many chunks around player

    // Constant Settings \\

    // Phsyics Settings
    public final float FIXED_TIME_STEP;

    // Base \\

    // TODO: I will need to add system wide safety rails here
    public Settings(Builder builder) {

        // Runtime Settings \\

        // Window Settings
        this.FOV = builder.FOV;
        this.windowWidth = builder.windowWidth;
        this.windowHeight = builder.windowHeight;
        this.windowX = builder.windowX;
        this.windowY = builder.windowY;
        this.fullscreen = builder.fullscreen;

        // Render Settings
        this.maxRenderDistance = builder.maxRenderDistance;

        // Constant Settings \\

        // Phsyics Settings
        this.FIXED_TIME_STEP = builder.FIXED_TIME_STEP;
    }

    // Builder \\

    public static class Builder {

        // Runtime Settings \\

        // Window Settings
        public float FOV = 70;
        public int windowWidth = 1280;
        public int windowHeight = 720;
        public int windowX = -1;
        public int windowY = -1;
        public boolean fullscreen = false;

        // Render Settings
        public int maxRenderDistance = 128;

        // Constant Settings \\

        // Physics Settings
        private float FIXED_TIME_STEP = 0.02f;

        // Base \\

        // Runtime Settings \\

        // Window Settings \\

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

        // Render Settings \\

        public Builder maxRenderDistance(int maxRenderDistance) {
            this.maxRenderDistance = maxRenderDistance;
            return this;
        }

        // Constant Settings \\

        // Physics Settings \\

        public Builder FIXED_TIME_STEP(float FIXED_TIME_STEP) {
            this.FIXED_TIME_STEP = FIXED_TIME_STEP;
            return this;
        }

        // Builder \\

        public Settings build() {
            return new Settings(this);
        }
    }
}
