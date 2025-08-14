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
    public final int AVAILABLE_THREADS; // Maximum available threads to pool off

    // Movement Settings
    public final float BASE_WALKING_SPEED; // Average human walking speed in m/s

    // Path Settings
    public final String BLOCK_TEXTURE_PATH; // The location of all block images
    public final String BIOME_JSON_PATH; // Location of all biome files
    public final String REGION_IMAGE_PATH; // The main image that controls the world

    // Atlas Settings
    public final int BLOCK_TEXTURE_SIZE; // The size of all block images
    public final int BLOCK_ATLAS_PADDING; // The padding between block faces in the atlas
    public final int CHUNKS_PER_PIXEL; // How many chunks per pixel in the region map

    // Scale Settings
    public final float BLOCK_SIZE; // 1x1x1 block scale
    public final int CHUNK_SIZE; // The width of a chunk in blocks
    public final int WORLD_HEIGHT; // The height of the world in blocks

    // World Tick Settings
    public final float WORLD_TICK; // Internal clock used to throttle world load
    public final int MAX_CHUNK_LOADS_PER_FRAME; // 256 chunks per frame
    public final int MAX_CHUNK_LOADS_PER_TICK; // 120 frames per second

    // Base \\

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
        this.AVAILABLE_THREADS = builder.AVAILABLE_THREADS;

        // Movement Settings
        this.BASE_WALKING_SPEED = builder.BASE_WALKING_SPEED;

        // Path Settings
        this.BLOCK_TEXTURE_PATH = builder.BLOCK_TEXTURE_PATH;
        this.BIOME_JSON_PATH = builder.BIOME_JSON_PATH;
        this.REGION_IMAGE_PATH = builder.REGION_IMAGE_PATH;

        // Atlas Settings
        this.BLOCK_TEXTURE_SIZE = builder.BLOCK_TEXTURE_SIZE;
        this.BLOCK_ATLAS_PADDING = builder.BLOCK_ATLAS_PADDING;
        this.CHUNKS_PER_PIXEL = builder.CHUNKS_PER_PIXEL;

        // Scale Settings
        this.BLOCK_SIZE = builder.BLOCK_SIZE;
        this.CHUNK_SIZE = builder.CHUNK_SIZE;
        this.WORLD_HEIGHT = builder.WORLD_HEIGHT;

        // World Tick Settings
        this.WORLD_TICK = builder.WORLD_TICK;
        this.MAX_CHUNK_LOADS_PER_FRAME = builder.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = builder.MAX_CHUNK_LOADS_PER_TICK;
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
        public int maxRenderDistance = 48;

        // Constant Settings \\

        // Thread Settings
        private int AVAILABLE_THREADS = 3;

        // Movement Settings
        private float BASE_WALKING_SPEED = 1.5f;

        // Path Settings
        private String BLOCK_TEXTURE_PATH = "textures";
        private String BIOME_JSON_PATH = "biomes/";
        private String REGION_IMAGE_PATH = "world/world.png";

        // Atlas Settings
        private int BLOCK_TEXTURE_SIZE = 32;
        private int BLOCK_ATLAS_PADDING = 0;
        private int CHUNKS_PER_PIXEL = 16;

        // Scale Settings
        public float BLOCK_SIZE = 1;
        public int CHUNK_SIZE = 16;
        public int WORLD_HEIGHT = 1024;

        // World Tick Settings
        public float WORLD_TICK = 1;
        public int MAX_CHUNK_LOADS_PER_FRAME = 256;
        public int MAX_CHUNK_LOADS_PER_TICK = 16384;

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

        public Builder AVAILABLE_THREADS(int AVAILABLE_THREADS) {
            this.AVAILABLE_THREADS = AVAILABLE_THREADS;
            return this;
        }

        // Movement Settings \\

        public Builder BASE_WALKING_SPEED(float BASE_WALKING_SPEED) {
            this.BASE_WALKING_SPEED = BASE_WALKING_SPEED;
            return this;
        }

        // Paths Settings \\

        public Builder BLOCK_TEXTURE_PATH(String BLOCK_TEXTURE_PATH) {
            this.BLOCK_TEXTURE_PATH = BLOCK_TEXTURE_PATH;
            return this;
        }

        public Builder BIOME_JSON_PATH(String BIOME_JSON_PATH) {
            this.BIOME_JSON_PATH = BIOME_JSON_PATH;
            return this;
        }

        public Builder REGION_IMAGE_PATH(String REGION_IMAGE_PATH) {
            this.REGION_IMAGE_PATH = REGION_IMAGE_PATH;
            return this;
        }

        // Atlas Settings \\

        public Builder BLOCK_TEXTURE_SIZE(int BLOCK_TEXTURE_SIZE) {
            this.BLOCK_TEXTURE_SIZE = BLOCK_TEXTURE_SIZE;
            return this;
        }

        public Builder BLOCK_ATLAS_PADDING(int BLOCK_ATLAS_PADDING) {
            this.BLOCK_ATLAS_PADDING = BLOCK_ATLAS_PADDING;
            return this;
        }

        public Builder CHUNKS_PER_PIXEL(int CHUNKS_PER_PIXEL) {
            this.CHUNKS_PER_PIXEL = CHUNKS_PER_PIXEL;
            return this;
        }

        // Scale Settings \\

        public Builder BLOCK_SIZE(float BLOCK_SIZE) {
            this.BLOCK_SIZE = BLOCK_SIZE;
            return this;
        }

        public Builder CHUNK_SIZE(int CHUNK_SIZE) {
            this.CHUNK_SIZE = CHUNK_SIZE;
            return this;
        }

        public Builder WORLD_HEIGHT(int WORLD_HEIGHT) {
            this.WORLD_HEIGHT = WORLD_HEIGHT;
            return this;
        }

        // World Tick Settings \\

        public Builder WORLD_TICK(float WORLD_TICK) {
            this.WORLD_TICK = WORLD_TICK;
            return this;
        }

        public Builder MAX_CHUNK_LOADS_PER_FRAME(int MAX_CHUNK_LOADS_PER_FRAME) {
            this.MAX_CHUNK_LOADS_PER_FRAME = MAX_CHUNK_LOADS_PER_FRAME;
            return this;
        }

        public Builder MAX_CHUNK_LOADS_PER_TICK(int MAX_CHUNK_LOADS_PER_TICK) {
            this.MAX_CHUNK_LOADS_PER_TICK = MAX_CHUNK_LOADS_PER_TICK;
            return this;
        }

        // Builder \\

        public Settings build() {
            return new Settings(this);
        }
    }
}
