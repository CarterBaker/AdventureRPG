package com.AdventureRPG.SettingsSystem;

public class Settings {

    // Debug Settings \\

    public boolean debug = true;

    // Window Settings \\

    public int windowWidth = 1280;
    public int windowHeight = 720;
    public int windowX = -1;
    public int windowY = -1;
    public boolean fullscreen = false;

    // Movement \\

    public float BASE_SPEED = 1.5f; // Average human walking speed in m/s

    // Region Map Settings \\

    public String REGION_IMAGE_PATH = "world/world.png";

    // Chunk Settings \\

    public float BLOCK_SIZE = 1; // 1x1x1 block scale
    public int CHUNK_SIZE = 16; // 16x16x16 blocks per chunk
    public int CHUNKS_PER_PIXEL = 256; // 256x256 chunks per pixel

    // Render Settings \\
    public int MAX_RENDER_DISTANCE = 32; // How many chunks around center
    public int MAX_RENDER_HEIGHT = 16; // How many chunks tall around center

    // LOD Settings \\

    public int LOD_START_DISTANCE = 16; // Chunks beyond this use LOD
    public int MAX_LOD_DISTANCE = 128; // LODs visible up to this range

    // Tick \\
    public float WORLD_TICK = 0.1f;

    // Loader Settings \\

    public int MAX_CHUNK_LOADS_PER_FRAME = 256; // 256 chunks per frame
    public int MAX_CHUNK_LOADS_PER_TICK = 30720; // 120 frames per second

    // Biome Settings \\
    public String BIOME_PATH = "biomes/";

    // World Generation \\

    public int BASE_WORLD_ELEVATION = 512; // Height where elevation starts
    public int MIN_WORLD_ELEVATION = 0; // Lava zone starts here
    public int MAX_WORLD_ELEVATION = 2048; // Above this is vacuum

    public int BASE_ELEVATION_BLENDING = 100; // Elevation gets blended by atleast this much

    public int MIN_CAVE_ELEVATION = 128; // Caves End Here
}
