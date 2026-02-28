package com.internal.core.engine.settings;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class EngineSetting {

        // Internal Settings
        public static final String THREAD_DEFINITIONS = "internal/threads";

        // Path Settings
        public static final String BLOCK_TEXTURE_PATH = "textures";
        public static final String SPRITE_PATH = "sprites";
        public static final String BLOCK_TEXTURE_ALIAS_PATH = "textureAliases";
        public static final String SHADER_PATH = "shaders";
        public static final String MATERIAL_JSON_PATH = "materials";
        public static final String PASS_JSON_PATH = "processingPasses";
        public static final String MESH_JSON_PATH = "mesh";
        public static final String WORLD_TEXTURE_PATH = "worlds";
        public static final String CALENDAR_JSON_PATH = "calendar/Calendar.json";
        public static final String BLOCK_JSON_PATH = "blocks";
        public static final String BIOME_JSON_PATH = "biomes";
        public static final String ENTITY_JSON_PATH = "entities";
        public static final String MENU_JSON_PATH = "menus";

        // File Extension Settings

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

        // Frag shader extensions
        private static final String[] FRAG_FILE_EXTENSIONS_INTERNAL = {
                        "fsh", "frag", "fs", "fragment", "pixel"
        };
        public static final ObjectArraySet<String> FRAG_FILE_EXTENSIONS = new ObjectArraySet<>(
                        FRAG_FILE_EXTENSIONS_INTERNAL);

        // Include shader extensions
        private static final String[] INCLUDE_FILE_EXTENSIONS_INTERNAL = {
                        "glsl", "inc", "glslinc"
        };
        public static final ObjectArraySet<String> INCLUDE_FILE_EXTENSIONS = new ObjectArraySet<>(
                        INCLUDE_FILE_EXTENSIONS_INTERNAL);

        // Atlas Settings
        public static final int BLOCK_TEXTURE_SIZE = 32;
        public static final int CHUNKS_PER_PIXEL = 32;

        // Sprite Settings
        public static final String SPRITE_DEFAULT_MESH = "util/Sprite";
        public static final String SPRITE_DEFAULT_MATERIAL = "util/Sprite";

        // Movement Settings
        public static final float BASE_WALKING_SPEED = 1.5f;

        // Time Settings
        public static final int MINUTES_PER_HOUR = 60;
        public static final int HOURS_PER_DAY = 24;
        public static final float MIDDAY_OFFSET = 0.5f;
        public static final int DAYS_PER_DAY = 20;
        public static final int YEARS_PER_AGE = 1500;

        public static final int STARTING_MINUTE = 30;
        public static final int STARTING_HOUR = 12;
        public static final int STARTING_MONTH = 6;
        public static final int STARTING_DAY_OF_MONTH = 15;
        public static final int STARTING_YEAR = 1356;
        public static final int STARTING_AGE = 3;

        // Scale Settings
        public static final float BLOCK_SIZE = 1.0f;
        public static final int BLOCK_PALETTE_THRESHOLD = 512;
        public static final int BIOME_SIZE = 4;
        public static final int CHUNK_SIZE = 16;
        public static final int MEGA_CHUNK_SIZE = 4;
        public static final int WORLD_HEIGHT = 64;

        // Mesh
        public static final int DEFAULT_BLOCK_DIRECTION = 4;
        public static final int CHUNK_VERT_BUFFER = 128;
        public static final int MESH_VERT_LIMIT = 32767;

        // World
        public static final String CHUNK_VAO = "util/vao/ChunkVAO";
        public static final String STARTING_WORLD = "TerraArcana";
        public static final String GRID_COORDINATE_UBO = "GridCoordinateData";
        public static final int MAX_CHUNK_STREAM_PER_QUEUE = 1024;
        public static final int MAX_CHUNK_STREAM_PER_FRAME = 128;
        public static final int MAX_CHUNK_STREAM_PER_BATCH = 32;
        public static final int GRID_SLOTS_SCAN_PER_FRAME = 32;
        public static final int CHUNK_POOL_MAX_OVERFLOW = 32;
        public static final int MEGA_POOL_MAX_OVERFLOW = 8;
        public static final int MEGA_ASSESS_PER_FRAME = GRID_SLOTS_SCAN_PER_FRAME / MEGA_CHUNK_SIZE;

        // Player
        public static final String DEFAULT_PLAYER_RACE = "humanoid";
        public static final float BLOCK_PLACEMENT_INTERVAL = 0.1f;

        // Lighting
        public static final int LUNAR_CYCLE_DAYS = 28;
}