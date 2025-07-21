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

    // Region Map Settings \\

    public static final String REGION_IMAGE_PATH = "assets/world/world.png";
    public static final String REGION_DATA_PATH = "assets/world/regions.dat";

    // Chunk/Grid Settings \\

    public static final int CHUNK_SIZE = 16;               // 16x16x16 blocks per chunk
    public static final int CHUNKS_PER_PIXEL = 256;          // 256x256x256 chunks per pixel
    public static final int MAX_RENDER_DISTANCE = 12;      // How many chunks around center

    // World Boundaries \\

    public static final int MIN_WORLD_HEIGHT = 0;          // Lava zone starts here
    public static final int MAX_WORLD_HEIGHT = 384;        // Above this is vacuum

    // LOD Settings \\

    public static final int LOD_START_DISTANCE = 16;        // Chunks beyond this use LOD
    public static final int MAX_LOD_DISTANCE = 128;         // LODs visible up to this range

    // Loader Settings \\

    public static final int MAX_CHUNK_LOADS_PER_FRAME = 8; // To throttle load speed
    
}
