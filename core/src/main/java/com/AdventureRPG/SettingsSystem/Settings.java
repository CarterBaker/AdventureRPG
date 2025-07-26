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
    public int MAX_RENDER_DISTANCE = 64; // How many chunks around center
    public int MAX_RENDER_HEIGHT = 16; // How many chunks tall around center

    // World Boundaries \\

    public int MIN_WORLD_HEIGHT = -256; // Lava zone starts here
    public int MAX_WORLD_HEIGHT = 512; // Above this is vacuum

    // LOD Settings \\

    public int LOD_START_DISTANCE = 16; // Chunks beyond this use LOD
    public int MAX_LOD_DISTANCE = 128; // LODs visible up to this range

    // Loader Settings \\

    public int MAX_CHUNK_LOADS_PER_FRAME = 256; // To throttle load speed
    public int MAX_CHUNK_LOADS_PER_TICK = 8192; // To throttle load speed

    // Tick \\
    public float WORLD_TICK = 0.15f;
}
