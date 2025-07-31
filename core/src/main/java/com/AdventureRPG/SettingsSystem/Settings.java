package com.AdventureRPG.SettingsSystem;

public class Settings {

    // Debug Settings \\

    public final boolean debug = true;

    // Window Settings \\

    public int windowWidth = 1280;
    public int windowHeight = 720;
    public int windowX = -1;
    public int windowY = -1;
    public boolean fullscreen = false;

    // Movement \\

    public final float BASE_SPEED; // Average human walking speed in m/s

    // Region Map Settings \\

    public final String REGION_IMAGE_PATH;

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
    public final float WORLD_TICK;

    // Loader Settings \\

    public final int MAX_CHUNK_LOADS_PER_FRAME; // 256 chunks per frame
    public final int MAX_CHUNK_LOADS_PER_TICK; // 120 frames per second

    // Biome Settings \\

    public final String BIOME_PATH;

    // World Generation \\

    public final int BASE_WORLD_ELEVATION; // Height where elevation starts
    public final int MIN_WORLD_ELEVATION; // Lava zone starts here
    public final int MAX_WORLD_ELEVATION; // Above this is vacuum

    public final int BASE_ELEVATION_BLENDING; // Elevation gets blended by atleast this much

    public final int BASE_OCEAN_LEVEL; // Global water height for ocean biomes
    public final int OCEAN_TIDE_OFFSET; // How High tides will rise from base

    public final int WATER_NOISE_OFFSET; // The offset for water biome noise

    public final int MIN_CAVE_ELEVATION; // Caves End Here

    // Constructor

    public Settings(Builder builder) {
        this.BASE_SPEED = builder.BASE_SPEED;
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

        this.MIN_CAVE_ELEVATION = builder.MIN_CAVE_ELEVATION;
    }

    // Builder

    public static class Builder {

        public float BASE_SPEED = 1.5f;

        public String REGION_IMAGE_PATH = "world/world.png";

        public float BLOCK_SIZE = 1;
        public int CHUNK_SIZE = 16;
        public int CHUNKS_PER_PIXEL = 256;

        public int MAX_RENDER_DISTANCE = 32;
        public int MAX_RENDER_HEIGHT = 16;

        public int LOD_START_DISTANCE = 16;
        public int MAX_LOD_DISTANCE = 128;

        public float WORLD_TICK = 0.1f;

        public int MAX_CHUNK_LOADS_PER_FRAME = 256;
        public int MAX_CHUNK_LOADS_PER_TICK = 30720;

        public String BIOME_PATH = "biomes/";

        public int BASE_WORLD_ELEVATION = 512;
        public int MIN_WORLD_ELEVATION = 0;
        public int MAX_WORLD_ELEVATION = 2048;

        public int BASE_ELEVATION_BLENDING = 100;

        public int BASE_OCEAN_LEVEL = 256;
        public int OCEAN_TIDE_OFFSET = 8;

        public int WATER_NOISE_OFFSET = 12345;

        public int MIN_CAVE_ELEVATION = 128;

        // Build

        public Builder setBaseSpeed(float speed) {
            this.BASE_SPEED = speed;
            return this;
        }

        public Builder setRegionImagePath(String path) {
            this.REGION_IMAGE_PATH = path;
            return this;
        }

        public Builder setChunkSettings(float blockSize, int chunkSize, int chunksPerPixel) {
            this.BLOCK_SIZE = blockSize;
            this.CHUNK_SIZE = chunkSize;
            this.CHUNKS_PER_PIXEL = chunksPerPixel;
            return this;
        }

        public Builder setRenderSettings(int renderDistance, int renderHeight) {
            this.MAX_RENDER_DISTANCE = renderDistance;
            this.MAX_RENDER_HEIGHT = renderHeight;
            return this;
        }

        public Builder setLOD(int lodStart, int maxLod) {
            this.LOD_START_DISTANCE = lodStart;
            this.MAX_LOD_DISTANCE = maxLod;
            return this;
        }

        public Builder setWorldTick(float tick) {
            this.WORLD_TICK = tick;
            return this;
        }

        public Builder setLoaderLimits(int perFrame, int perTick) {
            this.MAX_CHUNK_LOADS_PER_FRAME = perFrame;
            this.MAX_CHUNK_LOADS_PER_TICK = perTick;
            return this;
        }

        public Builder setBiomePath(String path) {
            this.BIOME_PATH = path;
            return this;
        }

        public Builder setWorldGen(int baseElevation, int minElevation, int maxElevation) {
            this.BASE_WORLD_ELEVATION = baseElevation;
            this.MIN_WORLD_ELEVATION = minElevation;
            this.MAX_WORLD_ELEVATION = maxElevation;
            return this;
        }

        public Builder setElevationBlending(int blend) {
            this.BASE_ELEVATION_BLENDING = blend;
            return this;
        }

        public Builder setOceanSettings(int level, int tideOffset) {
            this.BASE_OCEAN_LEVEL = level;
            this.OCEAN_TIDE_OFFSET = tideOffset;
            return this;
        }

        public Builder setWaterOffset(int noiseOffset) {
            this.WATER_NOISE_OFFSET = noiseOffset;
            return this;
        }

        public Builder setCaveSettings(int minCaveElevation) {
            this.MIN_CAVE_ELEVATION = minCaveElevation;
            return this;
        }

        public Settings build() {
            return new Settings(this);
        }
    }
}
