package com.AdventureRPG.SettingsSystem;

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

    // Thread Settings
    public final int AI_AVAILABLE_THREADS; // Maximum available threads to pool off
    public final int GENERATION_AVAILABLE_THREADS; // Maximum available threads to pool off
    public final int GENERAL_AVAILABLE_THREADS; // Maximum available threads to pool off

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

        // Thread Settings
        this.AI_AVAILABLE_THREADS = builder.AI_AVAILABLE_THREADS;
        this.GENERATION_AVAILABLE_THREADS = builder.GENERATION_AVAILABLE_THREADS;
        this.GENERAL_AVAILABLE_THREADS = builder.GENERAL_AVAILABLE_THREADS;
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
        public int maxRenderDistance = 64;

        // Constant Settings \\

        // Thread Settings
        private int AI_AVAILABLE_THREADS = 2;
        private int GENERATION_AVAILABLE_THREADS = 2;
        private int GENERAL_AVAILABLE_THREADS = 2;

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

        // Thread Settings \\

        public Builder AI_AVAILABLE_THREADS(int AI_AVAILABLE_THREADS) {
            this.AI_AVAILABLE_THREADS = AI_AVAILABLE_THREADS;
            return this;
        }

        public Builder GENERATION_AVAILABLE_THREADS(int GENERATION_AVAILABLE_THREADS) {
            this.GENERATION_AVAILABLE_THREADS = GENERATION_AVAILABLE_THREADS;
            return this;
        }

        public Builder GENERAL_AVAILABLE_THREADS(int GENERAL_AVAILABLE_THREADS) {
            this.GENERAL_AVAILABLE_THREADS = GENERAL_AVAILABLE_THREADS;
            return this;
        }

        // Builder \\

        public Settings build() {
            return new Settings(this);
        }
    }
}
