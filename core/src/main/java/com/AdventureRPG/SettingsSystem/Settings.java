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

    public String REGION_IMAGE_PATH = "world/world.png";

    // Chunk/Grid Settings \\

    public int CHUNK_SIZE = 16; // 16x16x16 blocks per chunk
    public int CHUNKS_PER_PIXEL = 256; // 256x256 chunks per pixel
    public int MAX_RENDER_DISTANCE = 12; // How many chunks around center

    // World Boundaries \\

    public int MIN_WORLD_HEIGHT = -256; // Lava zone starts here
    public int MAX_WORLD_HEIGHT = 512; // Above this is vacuum

    // LOD Settings \\

    public int LOD_START_DISTANCE = 16; // Chunks beyond this use LOD
    public int MAX_LOD_DISTANCE = 128; // LODs visible up to this range

    // Loader Settings \\

    public int MAX_CHUNK_LOADS_PER_FRAME = 8; // To throttle load speed
}
