package com.internal.core.engine.settings;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class EngineSetting {

        // Internal Settings
        public static final String THREAD_DEFINITIONS = "internal/threads"; // Location of all thread pool definitions

        // Path Settings
        public static final String BLOCK_TEXTURE_PATH = "textures"; // Location of all block images
        public static final String BLOCK_TEXTURE_ALIAS_PATH = "textureAliases"; // Location of alias definitions
        public static final String SHADER_PATH = "shaders"; // Location of all shader files
        public static final String MATERIAL_JSON_PATH = "materials"; // Location of all material json files
        public static final String PASS_JSON_PATH = "processingPasses"; // Location of all pass json files
        public static final String MESH_JSON_PATH = "mesh"; // Location of all material json files
        public static final String WORLD_TEXTURE_PATH = "worlds"; // The location of all world images
        public static final String CALENDAR_JSON_PATH = "calendar/Calendar.json"; // Location of the calendar json file
        public static final String BLOCK_JSON_PATH = "blocks"; // Location of all block json files
        public static final String BIOME_JSON_PATH = "biomes"; // Location of all biome files
        public static final String ENTITY_JSON_PATH = "entities"; // Location of all entity files

        // File Extension Settingsa

        // JSON extensions
        private static final String[] JSON_FILE_EXTENSIONS_INTERNAL = {
                        "json"
        };
        public static final ObjectArraySet<String> JSON_FILE_EXTENSIONS = new ObjectArraySet<>(
                        JSON_FILE_EXTENSIONS_INTERNAL);

        // Image extensions
        private static final String[] TEXTURE_FILE_EXTENSIONS_INTERNAL = {
                        "png", "jpg", "jpeg", "tga", "bmp"
        };
        public static final ObjectArraySet<String> TEXTURE_FILE_EXTENSIONS = new ObjectArraySet<>(
                        TEXTURE_FILE_EXTENSIONS_INTERNAL);

        // Vert shader extensions
        private static final String[] VERT_FILE_EXTENSIONS_INTERNAL = {
                        "vsh", "vert", "vs", "vertex"
        };
        public static final ObjectArraySet<String> VERT_FILE_EXTENSIONS = new ObjectArraySet<>(
                        VERT_FILE_EXTENSIONS_INTERNAL);

        // frag shader extensions
        private static final String[] FRAG_FILE_EXTENSIONS_INTERNAL = {
                        "fsh", "frag", "fs", "fragment", "pixel"
        };
        public static final ObjectArraySet<String> FRAG_FILE_EXTENSIONS = new ObjectArraySet<>(
                        FRAG_FILE_EXTENSIONS_INTERNAL);

        // Include shader Extensions
        private static final String[] INCLUDE_FILE_EXTENSIONS_INTERNAL = {
                        "glsl", "inc", "glslinc"
        };
        public static final ObjectArraySet<String> INCLUDE_FILE_EXTENSIONS = new ObjectArraySet<>(
                        INCLUDE_FILE_EXTENSIONS_INTERNAL);

        // Atlas Settings
        public static final int BLOCK_TEXTURE_SIZE = 32; // The size of all block images
        public static final int CHUNKS_PER_PIXEL = 32; // How many chunks per pixel in the region map

        // Movement Settings
        public static final float BASE_WALKING_SPEED = 1.5f; // Average human walking speed in m/s

        // Time Settings
        public static final int MINUTES_PER_HOUR = 60; // How many minutes are in an hour
        public static final int HOURS_PER_DAY = 24; // How many hours are in a day
        public static final float MIDDAY_OFFSET = 0.5f; // The offset of days in comparison to real world days
        public static final int DAYS_PER_DAY = 20; // How many in-game days fit inside a real world day
        public static final int YEARS_PER_AGE = 1500; // How many game years are there in an in-game age

        public static final int STARTING_MINUTE = 30; // The minutes the game will start in
        public static final int STARTING_HOUR = 12; // The hour the game will start in
        public static final int STARTING_MONTH = 6; // The month the game will start in
        public static final int STARTING_DAY_OF_MONTH = 15; // The day of the month the game will start in
        public static final int STARTING_YEAR = 1356; // The year the game will start in
        public static final int STARTING_AGE = 3; // The age the game will start in

        // Scale Settings
        public static final float BLOCK_SIZE = 1.0f; // 1x1x1 block scale
        public static final int BLOCK_PALETTE_THRESHOLD = 512; // The max amount of blocks a palette can compress
        public static final int BIOME_SIZE = 4; // The width of the biomes within a chunk
        public static final int CHUNK_SIZE = 16; // The width of a chunk in blocks
        public static final int MEGA_CHUNK_SIZE = 4; // The width that we will batch chunks for rendering
        public static final int WORLD_HEIGHT = 64; // The height of the world in blocks

        // Mesh
        public static final int VERT_POS = 3; // X, Y, Z
        public static final int VERT_NOR = 3; // nX, nY, nZ
        public static final int VERT_COL = 1; // Color
        public static final int VERT_UV0 = 2; // UV
        public static final int CHUNK_VERT_STRIDE = VERT_POS + VERT_NOR + VERT_COL + VERT_UV0; // Always 9
        public static final int CHUNK_VERT_BUFFER = 128;
        public static final int MESH_VERT_LIMIT = 32767;

        // World
        public static final String CHUNK_VAO = "util/vao/Stride9"; // The vao that all chunks will use
        public static final String STARTING_WORLD = "TerraArcana"; // The world the player starts in
        public static final String GRID_COORDINATE_UBO = "GridCoordinateData"; // The grid slot specific UBO
        public static final int MAX_CHUNK_STREAM_PER_FRAME = 128; // Chunks to load per frame
        public static final int MAX_CHUNK_STREAM_PER_BATCH = 32; // Chunks to load per batch
        public static final float GRID_FULL_DATA_THRESHOLD = 0.35f; // Inner 35% holds FULL block data
        public static final float GRID_ESSENTIAL_DATA_THRESHOLD = 0.70f; // Middle 70% holds ESSENTIAL data
        public static final float GRID_INDIVIDUAL_RENDER_THRESHOLD = 0.25f; // Inner 50% renders Individually

        // Player
        public static final String DEFAULT_PLAYER_RACE = "humanoid"; // The starting race for all players
}
