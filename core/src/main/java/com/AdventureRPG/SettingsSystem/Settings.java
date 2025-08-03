package com.AdventureRPG.SettingsSystem;

public class Settings {

    // Debug Settings \\

    public final boolean debug = true;

    // Window Settings \\

    public float FOV;
    public int windowWidth;
    public int windowHeight;
    public int windowX;
    public int windowY;
    public boolean fullscreen;

    // Movement \\

    public final float BASE_SPEED; // Average human walking speed in m/s

    // Block Settings \\
    
    public final String BLOCK_TEXTURE_PATH; // The location of all block images
    public final int BLOCK_TEXTURE_SIZE; // The size of all block images
    public final int BLOCK_ATLAS_PADDING; // The padding between block faces in the atlas

    // Region Map Settings \\

    public final String REGION_IMAGE_PATH; // The main image that controls the world

    // Chunk Settings \\

    public final float BLOCK_SIZE; // 1x1x1 block scale
    public final int CHUNK_SIZE; // 16x16x16 blocks per chunk
    public final int CHUNKS_PER_PIXEL; // 256x256 chunks per pixel

    // Render Settings \\

    public final int MAX_RENDER_DISTANCE; // How many chunks around player
    public final int MAX_RENDER_HEIGHT; // How many chunks tall around player

    // LOD Settings \\

    public final int LOD_START_DISTANCE; // Chunks beyond this use LOD
    public final int MAX_LOD_DISTANCE; // LODs visible up to this range

    // Tick \\

    public final float WORLD_TICK; // Internal clock used to throttle world load

    // Loader Settings \\

    public final int MAX_CHUNK_LOADS_PER_FRAME; // 256 chunks per frame
    public final int MAX_CHUNK_LOADS_PER_TICK; // 120 frames per second

    // Biome Settings \\

    public final String BIOME_PATH; // Location of all biome files

    // World Generation \\

    public final int BASE_WORLD_ELEVATION; // Height where elevation starts
    public final int MIN_WORLD_ELEVATION; // Lava zone starts here
    public final int MAX_WORLD_ELEVATION; // Above this is vacuum

    public final int BASE_ELEVATION_BLENDING; // Elevation gets blended by atleast this much

    public final int BASE_OCEAN_LEVEL; // Global water height for ocean biomes
    public final int OCEAN_TIDE_OFFSET; // How High tides will rise from base

    public final int WATER_NOISE_OFFSET; // The offset for water biome noise
    public final int WATER_HEIGHT_OFFSET; // The offset for water height control

    public final int MIN_CAVE_ELEVATION; // Deepest elevation caves can reach

    public final int CAVE_NOISE_OFFSET; // The offset for cave biome noise
    public final int SURFACE_BREAK__OFFSET; // The offset for cave biome noise

    public final int BIOME_BLEND_OFFSET; // Offset for biome blending

    // Constructor

    public Settings(Builder builder) {

        this.FOV = builder.FOV;
        this.windowWidth = builder.windowWidth;
        this.windowHeight = builder.windowHeight;
        this.windowX = builder.windowX;
        this.windowY = builder.windowY;
        this.fullscreen = builder.fullscreen;

        this.BASE_SPEED = builder.BASE_SPEED;

        this.BLOCK_TEXTURE_PATH = builder.BLOCK_TEXTURE_PATH;
        this.BLOCK_TEXTURE_SIZE = builder.BLOCK_TEXTURE_SIZE;
        this.BLOCK_ATLAS_PADDING = builder.BLOCK_ATLAS_PADDING;

        this.REGION_IMAGE_PATH = builder.REGION_IMAGE_PATH;

        this.BLOCK_SIZE = builder.BLOCK_SIZE;
        this.CHUNK_SIZE = builder.CHUNK_SIZE;
        this.CHUNKS_PER_PIXEL = builder.CHUNKS_PER_PIXEL;

        this.MAX_RENDER_DISTANCE = builder.MAX_RENDER_DISTANCE;
        this.MAX_RENDER_HEIGHT = builder.MAX_RENDER_HEIGHT;

        this.LOD_START_DISTANCE = builder.LOD_START_DISTANCE;
        this.MAX_LOD_DISTANCE = builder.MAX_LOD_DISTANCE;

        this.WORLD_TICK = builder.WORLD_TICK;

        this.MAX_CHUNK_LOADS_PER_FRAME = builder.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = builder.MAX_CHUNK_LOADS_PER_TICK;

        this.BIOME_PATH = builder.BIOME_PATH;

        this.BASE_WORLD_ELEVATION = builder.BASE_WORLD_ELEVATION;
        this.MIN_WORLD_ELEVATION = builder.MIN_WORLD_ELEVATION;
        this.MAX_WORLD_ELEVATION = builder.MAX_WORLD_ELEVATION;

        this.BASE_ELEVATION_BLENDING = builder.BASE_ELEVATION_BLENDING;

        this.BASE_OCEAN_LEVEL = builder.BASE_OCEAN_LEVEL;
        this.OCEAN_TIDE_OFFSET = builder.OCEAN_TIDE_OFFSET;

        this.WATER_NOISE_OFFSET = builder.WATER_NOISE_OFFSET;
        this.WATER_HEIGHT_OFFSET = builder.WATER_HEIGHT_OFFSET;

        this.MIN_CAVE_ELEVATION = builder.MIN_CAVE_ELEVATION;

        this.CAVE_NOISE_OFFSET = builder.CAVE_NOISE_OFFSET;
        this.SURFACE_BREAK__OFFSET = builder.SURFACE_BREAK__OFFSET;

        this.BIOME_BLEND_OFFSET = builder.BIOME_BLEND_OFFSET;
    }

    // Builder

    public static class Builder {

        public float FOV = 70;
        public int windowWidth = 1280;
        public int windowHeight = 720;
        public int windowX = -1;
        public int windowY = -1;
        public boolean fullscreen = false;

        private float BASE_SPEED = 1.5f;

        private String BLOCK_TEXTURE_PATH = "textures";
        private int BLOCK_TEXTURE_SIZE = 32;
        private int BLOCK_ATLAS_PADDING = 0;

        private String REGION_IMAGE_PATH = "world/world.png";

        private float BLOCK_SIZE = 1;
        private int CHUNK_SIZE = 16;
        private int CHUNKS_PER_PIXEL = 256;

        private int MAX_RENDER_DISTANCE = 32;
        private int MAX_RENDER_HEIGHT = 16;

        private int LOD_START_DISTANCE = 16;
        private int MAX_LOD_DISTANCE = 128;

        private float WORLD_TICK = 0.1f;

        private int MAX_CHUNK_LOADS_PER_FRAME = 256;
        private int MAX_CHUNK_LOADS_PER_TICK = 30720;

        private String BIOME_PATH = "biomes/";

        private int BASE_WORLD_ELEVATION = 512;
        private int MIN_WORLD_ELEVATION = 0;
        private int MAX_WORLD_ELEVATION = 2048;

        private int BASE_ELEVATION_BLENDING = 100;

        private int BASE_OCEAN_LEVEL = 256;
        private int OCEAN_TIDE_OFFSET = 8;

        private int WATER_NOISE_OFFSET = 12345;
        private int WATER_HEIGHT_OFFSET = 8888;

        private int MIN_CAVE_ELEVATION = 128;

        private int CAVE_NOISE_OFFSET = 9999;
        private int SURFACE_BREAK__OFFSET = 2222;

        private int BIOME_BLEND_OFFSET = 4321;

        // Build

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

        public Builder BASE_SPEED(float BASE_SPEED) {
            this.BASE_SPEED = BASE_SPEED;
            return this;
        }

        public Builder BLOCK_TEXTURE_PATH(String BLOCK_TEXTURE_PATH) {
            this.BLOCK_TEXTURE_PATH = BLOCK_TEXTURE_PATH;
            return this;
        }

        public Builder BLOCK_TEXTURE_SIZE(int BLOCK_TEXTURE_SIZE) {
            this.BLOCK_TEXTURE_SIZE = BLOCK_TEXTURE_SIZE;
            return this;
        }

        public Builder BLOCK_ATLAS_PADDING(int BLOCK_ATLAS_PADDING) {
            this.BLOCK_ATLAS_PADDING = BLOCK_ATLAS_PADDING;
            return this;
        }

        public Builder REGION_IMAGE_PATH(String REGION_IMAGE_PATH) {
            this.REGION_IMAGE_PATH = REGION_IMAGE_PATH;
            return this;
        }

        public Builder BLOCK_SIZE(float BLOCK_SIZE) {
            this.BLOCK_SIZE = BLOCK_SIZE;
            return this;
        }

        public Builder CHUNK_SIZE(int CHUNK_SIZE) {
            this.CHUNK_SIZE = CHUNK_SIZE;
            return this;
        }

        public Builder CHUNKS_PER_PIXEL(int CHUNKS_PER_PIXEL) {
            this.CHUNKS_PER_PIXEL = CHUNKS_PER_PIXEL;
            return this;
        }

        public Builder MAX_RENDER_DISTANCE(int MAX_RENDER_DISTANCE) {
            this.MAX_RENDER_DISTANCE = MAX_RENDER_DISTANCE;
            return this;
        }

        public Builder MAX_RENDER_HEIGHT(int MAX_RENDER_HEIGHT) {
            this.MAX_RENDER_HEIGHT = MAX_RENDER_HEIGHT;
            return this;
        }

        public Builder LOD_START_DISTANCE(int LOD_START_DISTANCE) {
            this.LOD_START_DISTANCE = LOD_START_DISTANCE;
            return this;
        }

        public Builder MAX_LOD_DISTANCE(int MAX_LOD_DISTANCE) {
            this.MAX_LOD_DISTANCE = MAX_LOD_DISTANCE;
            return this;
        }

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

        public Builder BIOME_PATH(String BIOME_PATH) {
            this.BIOME_PATH = BIOME_PATH;
            return this;
        }

        public Builder BASE_WORLD_ELEVATION(int BASE_WORLD_ELEVATION) {
            this.BASE_WORLD_ELEVATION = BASE_WORLD_ELEVATION;
            return this;
        }

        public Builder MIN_WORLD_ELEVATION(int MIN_WORLD_ELEVATION) {
            this.MIN_WORLD_ELEVATION = MIN_WORLD_ELEVATION;
            return this;
        }

        public Builder MAX_WORLD_ELEVATION(int MAX_WORLD_ELEVATION) {
            this.MAX_WORLD_ELEVATION = MAX_WORLD_ELEVATION;
            return this;
        }

        public Builder BASE_ELEVATION_BLENDING(int BASE_ELEVATION_BLENDING) {
            this.BASE_ELEVATION_BLENDING = BASE_ELEVATION_BLENDING;
            return this;
        }

        public Builder BASE_OCEAN_LEVEL(int BASE_OCEAN_LEVEL) {
            this.BASE_OCEAN_LEVEL = BASE_OCEAN_LEVEL;
            return this;
        }

        public Builder OCEAN_TIDE_OFFSET(int OCEAN_TIDE_OFFSET) {
            this.OCEAN_TIDE_OFFSET = OCEAN_TIDE_OFFSET;
            return this;
        }

        public Builder WATER_NOISE_OFFSET(int WATER_NOISE_OFFSET) {
            this.WATER_NOISE_OFFSET = WATER_NOISE_OFFSET;
            return this;
        }

        public Builder WATER_HEIGHT_OFFSET(int WATER_HEIGHT_OFFSET) {
            this.WATER_HEIGHT_OFFSET = WATER_HEIGHT_OFFSET;
            return this;
        }

        public Builder MIN_CAVE_ELEVATION(int MIN_CAVE_ELEVATION) {
            this.MIN_CAVE_ELEVATION = MIN_CAVE_ELEVATION;
            return this;
        }

        public Builder CAVE_NOISE_OFFSET(int CAVE_NOISE_OFFSET) {
            this.CAVE_NOISE_OFFSET = CAVE_NOISE_OFFSET;
            return this;
        }

        public Builder SURFACE_BREAK__OFFSET(int SURFACE_BREAK__OFFSET) {
            this.SURFACE_BREAK__OFFSET = SURFACE_BREAK__OFFSET;
            return this;
        }

        public Builder BIOME_BLEND_OFFSET(int BIOME_BLEND_OFFSET) {
            this.BIOME_BLEND_OFFSET = BIOME_BLEND_OFFSET;
            return this;
        }

        public Settings build() {
            return new Settings(this);
        }
    }
}
