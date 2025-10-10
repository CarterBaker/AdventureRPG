package com.AdventureRPG.Util;

import com.badlogic.gdx.graphics.Color;

public class GlobalConstant {

    // World Tick Settings
    public static final float WORLD_TICK = 1; // Internal clock used to throttle world load
    public static final int MAX_CHUNK_LOADS_PER_FRAME = 256; // 256 chunks per frame
    public static final int MAX_CHUNK_LOADS_PER_TICK = 16384; // 120 frames per second

    // Path Settings
    public static final String CALENDAR_JSON_PATH = "calendar.json"; // The location of the calendar json file
    public static final String BLOCK_JSON_PATH = "blocks.json"; // The location of the blocks json file
    public static final String BLOCK_TEXTURE_PATH = "textures"; // The location of all block images
    public static final String BIOME_JSON_PATH = "biomes"; // Location of all biome files
    public static final String REGION_IMAGE_PATH = "world/world.png"; // The main image that controls the world
    public static final String SHADER_JSON_PATH = "shaders"; // The location of all shader json files
    public static final String MATERIAL_JSON_PATH = "materials"; // The location of all material json files
    public static final String PASS_JSON_PATH = "renderPasses"; // The location of all pass json files

    // PBR Settings
    public static final Color NORMAL_MAP_DEFAULT = new Color(0.5f, 0.5f, 1f, 1f); // Default flat normal color
    public static final Color HEIGHT_MAP_DEFAULT = new Color(0f, 0f, 0f, 1f); // Default black (0,0,0)
    public static final Color METAL_MAP_DEFAULT = new Color(0f, 0f, 0f, 1f); // Default black (0,0,0)
    public static final Color ROUGHNESS_MAP_DEFAULT = new Color(1f, 1f, 1f, 1f); // Default white (255,255,255)
    public static final Color AO_MAP_DEFAULT = new Color(1f, 1f, 1f, 1f); // Default white (255,255,255)
    public static final Color CUSTOM_MAP_DEFAULT = new Color(0f, 0f, 0f, 1f); // Default black (0,0,0)

    // Atlas Settings
    public static final int BLOCK_TEXTURE_SIZE = 32; // The size of all block images
    public static final int BLOCK_ATLAS_PADDING = 0; // The padding between block faces in the atlas
    public static final int CHUNKS_PER_PIXEL = 32; // How many chunks per pixel in the region map

    // Movement Settings
    public static final float BASE_WALKING_SPEED = 1.5f; // Average human walking speed in m/s

    // Time Settings
    public static final int MINUTES_PER_HOUR = 60; // How many minutes are in an hour
    public static final int HOURS_PER_DAY = 24; // How many hours are in a day
    public static final int DAYS_PER_DAY = 20; // How many in-game days fit inside a real world day
    public static final float MIDDAY_OFFSET = 0.5f; // The offset of days in comparison to real world days

    public static final int STARTING_DAY = 15; // The day the game will start in
    public static final int STARTING_MONTH = 6; // The month the game will start in
    public static final int STARTING_YEAR = 1356; // The year the game will start in
    public static final int STARTING_AGE = 3; // The age the game will start in
    public static final int YEARS_PER_AGE = 1500; // How many years are there in an age

    // Scale Settings
    public static final float BLOCK_SIZE = 1.0f; // 1x1x1 block scale
    public static final int BIOME_SIZE = 4; // The width of the biomes within a chunk
    public static final int CHUNK_SIZE = 16; // The width of a chunk in blocks
    public static final int MEGA_CHUNK_SIZE = 4; // The width that we wiill batch chunks for rendering
    public static final int WORLD_HEIGHT = 64; // The height of the world in blocks

    // Mesh
    public static final int VERT_POS = 3; // X, Y, Z
    public static final int VERT_NOR = 3; // nX, nY, nZ
    public static final int VERT_COL = 1; // Color
    public static final int VERT_UV0 = 2; // UV
    public static final int VERT_STRIDE = VERT_POS + VERT_NOR + VERT_COL + VERT_UV0;
    public static final int CHUNK_VERT_BUFFER = 128;
    public static final int MESH_VERT_LIMIT = 32767;
}
