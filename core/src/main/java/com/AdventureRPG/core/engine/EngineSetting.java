package com.AdventureRPG.core.engine;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class EngineSetting {

        // World Tick Settings
        public static final float WORLD_TICK = 1; // Internal clock used to throttle world load
        public static final int MAX_CHUNK_LOADS_PER_FRAME = 256; // 256 chunks per frame
        public static final int MAX_CHUNK_LOADS_PER_TICK = 16384; // 120 frames per second

        // Path Settings
        public static final String CALENDAR_JSON_PATH = "calendar.json"; // The location of the calendar json file
        public static final String BLOCK_JSON_PATH = "blocks.json"; // The location of the blocks json file
        public static final String BLOCK_TEXTURE_PATH = "textures"; // The location of all block images
        public static final String BLOCK_TEXTURE_ALIAS_PATH = "textureAliases"; // Location of alias definitions
        public static final String BIOME_JSON_PATH = "biomes"; // Location of all biome files
        public static final String REGION_IMAGE_PATH = "world/world.png"; // The main image that controls the world
        public static final String SHADER_PATH = "shaders"; // The location of all shader files
        public static final String MATERIAL_JSON_PATH = "materials"; // The location of all material json files
        public static final String MODEL_JSON_PATH = "models"; // The location of all material json files
        public static final String PASS_JSON_PATH = "processingPasses"; // The location of all pass json files

        // File Extension Settings
        private static final String[] JSON_FILE_EXTENSIONS_INTERNAL = {
                        "json"
        };
        public static final ObjectArraySet<String> JSON_FILE_EXTENSIONS = new ObjectArraySet<>(
                        JSON_FILE_EXTENSIONS_INTERNAL);

        private static final String[] TEXTURE_FILE_EXTENSIONS_INTERNAL = {
                        "png", "jpg", "jpeg", "tga", "bmp"
        };
        public static final ObjectArraySet<String> TEXTURE_FILE_EXTENSIONS = new ObjectArraySet<>(
                        TEXTURE_FILE_EXTENSIONS_INTERNAL);

        private static final String[] VERT_FILE_EXTENSIONS_INTERNAL = {
                        "vsh", "vert", "vs", "vertex"
        };
        public static final ObjectArraySet<String> VERT_FILE_EXTENSIONS = new ObjectArraySet<>(
                        VERT_FILE_EXTENSIONS_INTERNAL);

        private static final String[] FRAG_FILE_EXTENSIONS_INTERNAL = {
                        "fsh", "frag", "fs", "fragment", "pixel"
        };
        public static final ObjectArraySet<String> FRAG_FILE_EXTENSIONS = new ObjectArraySet<>(
                        FRAG_FILE_EXTENSIONS_INTERNAL);

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
        public static final int MAX_MODEL_READ_PASSES = 5;
        public static final int VERT_POS = 3; // X, Y, Z
        public static final int VERT_NOR = 3; // nX, nY, nZ
        public static final int VERT_COL = 1; // Color
        public static final int VERT_UV0 = 2; // UV
        public static final int STATIC_VERT_STRIDE = VERT_POS + VERT_NOR + VERT_COL + VERT_UV0;
        public static final int CHUNK_VERT_BUFFER = 128;
        public static final int MESH_VERT_LIMIT = 32767;
}
