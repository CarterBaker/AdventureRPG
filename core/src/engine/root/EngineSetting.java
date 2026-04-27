package engine.root;

import engine.graphics.color.Color;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class EngineSetting {

        // Engine Settings \\

        public static final String VERSION = "0.0.0.1a";
        public static final int LOADER_BATCH_SIZE = 32;
        public static final String THREAD_DEFINITIONS = "program/threads";

        // Registry \\

        public static final int FNV_OFFSET_BASIS = 0x811c9dc5;
        public static final int FNV_PRIME = 0x01000193;
        public static final short REGISTRY_RESERVED_ID = 0;

        // File Extensions \\

        public static final ObjectArraySet<String> FONT_FILE_EXTENSIONS = new ObjectArraySet<>(
                        new String[] { "ttf", "otf" });
        public static final ObjectArraySet<String> FRAG_FILE_EXTENSIONS = new ObjectArraySet<>(
                        new String[] { "fsh", "frag", "fs", "fragment", "pixel" });
        public static final ObjectArraySet<String> INCLUDE_FILE_EXTENSIONS = new ObjectArraySet<>(
                        new String[] { "glsl", "inc", "glslinc" });
        public static final ObjectArraySet<String> JSON_FILE_EXTENSIONS = new ObjectArraySet<>(
                        new String[] { "json" });
        public static final ObjectArraySet<String> TEXTURE_FILE_EXTENSIONS = new ObjectArraySet<>(
                        new String[] { "png", "jpg", "jpeg", "tga", "bmp" });
        public static final ObjectArraySet<String> VERT_FILE_EXTENSIONS = new ObjectArraySet<>(
                        new String[] { "vsh", "vert", "vs", "vertex" });

        // Asset Paths \\

        public static final String BEHAVIOR_JSON_PATH = "behaviors";
        public static final String BIOME_JSON_PATH = "biomes";
        public static final String BLOCK_JSON_PATH = "blocks";
        public static final String BLOCK_TEXTURE_PATH = "textures";
        public static final String BLOCK_TEXTURE_ALIAS_PATH = "texturealiases";
        public static final String CALENDAR_JSON_PATH = "calendars";
        public static final String ENTITY_JSON_PATH = "entities";
        public static final String FONT_PATH = "fonts";
        public static final String ITEM_JSON_PATH = "items";
        public static final String MATERIAL_JSON_PATH = "materials";
        public static final String MENU_JSON_PATH = "menus";
        public static final String MESH_JSON_PATH = "mesh";
        public static final String PASS_JSON_PATH = "processingpasses";
        public static final String PASS_SKY = "Sky";
        public static final String FBO_CATALOG_JSON_PATH = "assets/fbos.json";
        public static final String SHADER_PATH = "shaders";
        public static final String SPRITE_PATH = "sprites";
        public static final String TOOL_TYPE_JSON_PATH = "tools";
        public static final String UBO_JSON_PATH = "ubos";
        public static final String WORLD_TEXTURE_PATH = "worlds";

        // Window \\

        public static final String WINDOW_TITLE = "TerraArcana";
        public static final int MIN_WINDOW_DIMENSION = 64;

        // Scale \\

        public static final float BLOCK_SIZE = 1.0f;
        public static final int BIOME_SIZE = 4;
        public static final int CHUNK_SIZE = 16;
        public static final int CHUNKS_PER_PIXEL = 32;
        public static final int MEGA_CHUNK_SIZE = 4;
        public static final int WORLD_HEIGHT = 64;
        public static final int BLOCK_PALETTE_THRESHOLD = 512;
        public static final int SUB_VOXEL_RESOLUTION = 16;

        // Rendering \\

        public static final int MAX_RENDER_CALLS_PER_FRAME = 4096;

        public static final int GL_HANDLE_NONE = 0;
        public static final int GL_INVALID_INDEX = 0xFFFFFFFF;
        public static final int GL_NEAREST = 0x2600;
        public static final int INDEX_NOT_FOUND = -1;

        // Shader Pipeline \\

        public static final int SHADER_ALIAS_LIBRARY_INITIAL_CAPACITY = 16;
        public static final int SHADER_ALIAS_LIBRARY_GROWTH_FACTOR = 2;
        public static final float SHADER_ALIAS_DEFAULT_ALPHA = 1.0f;
        public static final int SHADER_UBO_UNSPECIFIED_BINDING = INDEX_NOT_FOUND;

        // Composite Rendering \\

        public static final int COMPOSITE_UPLOAD_BUFFER_GROWTH_FACTOR = 2;
        public static final int COMPOSITE_UPLOAD_VERSION_UNINITIALIZED = INDEX_NOT_FOUND;

        // Camera \\

        public static final float CAMERA_NEAR_PLANE = 0.1f;
        public static final float CAMERA_FAR_PLANE = 1000f;
        public static final float CAMERA_MAX_PITCH_DEGREES = 89f;

        // Frustum Culling \\

        public static final float FRUSTUM_TWO_PI = (float) (Math.PI * 2f);
        public static final float FRUSTUM_PI = (float) Math.PI;
        public static final float FRUSTUM_HALF_PI = (float) (Math.PI / 2f);
        public static final float FRUSTUM_MIN_BLEED = 0.1f;
        public static final float FRUSTUM_CHUNK_BLEED_SCALE = 0.75f;
        public static final float FRUSTUM_ALWAYS_VISIBLE_DIST_SQ = 4.5f;
        public static final float FRUSTUM_PITCH_MIN_DIST_SQ = 25f;
        public static final float FRUSTUM_PITCH_POWER_ANGLE = 1f;
        public static final float FRUSTUM_PITCH_POWER_DISTANCE = 6f;

        // Texture \\

        public static final String TEXTURE_UV_SCALE_UNIFORM = "u_uvPerBlock";

        // Sprite \\

        public static final String SPRITE_DEFAULT_MATERIAL = "util/SpriteDefault";
        public static final String SPRITE_DEFAULT_MESH = "util/Sprite";

        // Blit \\

        public static final String DEFAULT_BLIT_MATERIAL = "fullscreen/Blit";
        public static final String DEFAULT_BLIT_MESH = "util/Blit";

        // Framebuffer Names \\

        public static final String FBO_SKY = "SkyScene";
        public static final String FBO_WORLD = "MainScene";
        public static final String FBO_UI = "UIScene";

        // Mesh \\

        public static final int CHUNK_VERT_BUFFER = 128;
        public static final int DEFAULT_BLOCK_DIRECTION = 4;
        public static final int MESH_VERT_LIMIT = 32767;

        // Geometry \\

        public static final int COMPOSITE_BUFFER_INITIAL_CAPACITY = 64;

        // Block \\

        public static final String AIR_BLOCK_NAME = "TerraArcana/Air";
        public static final int ENCODED_FACE_NATURAL_FULL_OFFSET = 24;
        public static final short DEFAULT_BLOCK_ORIENTATION = (short) (DEFAULT_BLOCK_DIRECTION * 4);

        // World \\

        public static final String STARTING_WORLD = "TerraArcana";
        public static final String CHUNK_VAO = "util/vao/ChunkVAO";
        public static final String DEFAULT_BIOME_NAME = "Empty";
        public static final int CHUNK_POOL_MAX_OVERFLOW = 32;
        public static final int GRID_SLOTS_SCAN_PER_FRAME = 32;
        public static final int MAX_CHUNK_STREAM_PER_BATCH = 32;
        public static final int MAX_CHUNK_STREAM_PER_FRAME = 128;
        public static final int MAX_CHUNK_STREAM_PER_QUEUE = 1024;
        public static final int MEGA_ASSESS_PER_FRAME = GRID_SLOTS_SCAN_PER_FRAME / MEGA_CHUNK_SIZE;
        public static final int MEGA_POOL_MAX_OVERFLOW = 8;

        // World Defaults \\

        public static final float DEFAULT_GRAVITY_MULTIPLIER = 1.0f;
        public static final float DEFAULT_GRAVITY_X = 0.0f;
        public static final float DEFAULT_GRAVITY_Y = -1.0f;
        public static final float DEFAULT_GRAVITY_Z = 0.0f;
        public static final float DEFAULT_DAYS_PER_DAY = 20.0f;
        public static final String DEFAULT_CALENDAR_NAME = "standard/Default";

        // UBOs \\

        public static final String CAMERA_DATA_UBO = "CameraData";
        public static final String ORTHO_DATA_UBO = "OrthoData";
        public static final String DIRECTIONAL_LIGHT_UBO = "DirectionalLightData";
        public static final String GRID_COORDINATE_UBO = "GridCoordinateData";
        public static final String ITEM_ROTATION_UBO = "ItemRotationData";
        public static final String PLAYER_POSITION_UBO = "PlayerPositionData";
        public static final String UBO_TIME_DATA_NAME = "TimeData";

        // Camera UBO Uniforms \\

        public static final String UNIFORM_CAM_PROJECTION = "u_projection";
        public static final String UNIFORM_CAM_VIEW = "u_view";
        public static final String UNIFORM_CAM_INVERSE_PROJECTION = "u_inverseProjection";
        public static final String UNIFORM_CAM_INVERSE_VIEW = "u_inverseView";
        public static final String UNIFORM_CAM_VIEW_PROJECTION = "u_viewProjection";
        public static final String UNIFORM_CAM_POSITION = "u_cameraPosition";
        public static final String UNIFORM_CAM_FOV = "u_cameraFOV";
        public static final String UNIFORM_CAM_VIEWPORT = "u_viewport";
        public static final String UNIFORM_CAM_NEAR_PLANE = "u_nearPlane";
        public static final String UNIFORM_CAM_FAR_PLANE = "u_farPlane";

        // Ortho UBO Uniforms \\

        public static final String UNIFORM_ORTHO_PROJECTION = "u_orthoProjection";
        public static final String UNIFORM_ORTHO_SCREEN_SIZE = "u_screenSize";

        // Time \\

        public static final int DAYS_PER_DAY = 20;
        public static final int HOURS_PER_DAY = 24;
        public static final int MINUTES_PER_HOUR = 60;
        public static final int LUNAR_CYCLE_DAYS = 28;
        public static final int YEARS_PER_AGE = 1500;
        public static final long MILLIS_PER_REAL_DAY = 86400000L;
        public static final float MIDDAY_OFFSET = 0.5f;

        // Starting Time \\

        public static final int STARTING_AGE = 3;
        public static final int STARTING_YEAR = 1356;
        public static final int STARTING_MONTH = 6;
        public static final int STARTING_DAY_OF_MONTH = 15;
        public static final int STARTING_HOUR = 12;
        public static final int STARTING_MINUTE = 30;

        // Clock Visuals \\

        public static final double CLOCK_MIDNIGHT = 0.0;
        public static final double CLOCK_QUARTER = 0.25;
        public static final double CLOCK_NOON = 0.5;
        public static final double CLOCK_THREE_QUARTERS = 0.75;
        public static final double CLOCK_MAX_SEASON_SHIFT = 0.08;
        public static final double CLOCK_SUNRISE_MIN = 0.05;
        public static final double CLOCK_SUNRISE_MAX = 0.40;
        public static final double CLOCK_SUNSET_MIN = 0.60;
        public static final double CLOCK_SUNSET_MAX = 0.95;

        // Clock Noise \\

        public static final long CLOCK_NOISE_MASK = 16777215L;
        public static final long CLOCK_NOISE_MULTIPLIER = -49064778989728563L;
        public static final double CLOCK_NOISE_DIVISOR = 1.6777216E7;
        public static final double CLOCK_NOISE_MIN = 0.001;

        // Lighting \\

        public static final float SUN_HORIZON_CUTOFF = 0.85f;
        public static final float SUN_BLEND_THRESHOLD = 0.15f;
        public static final float MOON_BRIGHTNESS_BASE = 0.7f;
        public static final float MOON_BRIGHTNESS_LUNAR_SCALE = 0.3f;
        public static final float MOON_COLOR_R = 0.75f;
        public static final float MOON_COLOR_G = 0.85f;
        public static final float MOON_COLOR_B = 1.0f;
        public static final float MOON_HORIZON_CUTOFF = 0.85f;
        public static final float MOON_MAX_INTENSITY = 0.25f;
        public static final float MOON_PHASE_MIN = 0.05f;
        public static final float MOON_PHASE_MAX = 0.95f;

        // Physics \\

        public static final float FIXED_TIME_STEP = 0.02f;
        public static final float GRAVITY_FORCE = 9.8f;
        public static final float JUMP_HOLD_FRACTION = 0.4f;
        public static final float MOVEMENT_ACCELERATION = 8.0f;

        // Movement \\

        public static final float BASE_WALKING_SPEED = 1.5f;
        public static final float JUMP_SCALE = 1.8f;
        public static final float MOVEMENT_SCALE = 1.5f;

        // Raycast \\

        public static final float REACH_SCALE = 4.0f;

        // Entity \\

        public static final float DEFAULT_ENTITY_SIZE = 1f;
        public static final float DEFAULT_ENTITY_WEIGHT = 1f;
        public static final float DEFAULT_EYE_LEVEL = 0.91f;
        public static final float DEFAULT_JUMP_DURATION = 0.5f;
        public static final float DEFAULT_JUMP_HEIGHT = 0.5f;
        public static final float DEFAULT_MOVEMENT_SPEED = 3.3f;
        public static final float DEFAULT_REACH = 1f;
        public static final float DEFAULT_SPRINT_SPEED = 7f;
        public static final float DEFAULT_WALK_SPEED = 1.4f;

        // Player \\

        public static final float BLOCK_PLACEMENT_INTERVAL = 0.1f;
        public static final String DEFAULT_PLAYER_RACE = "Humanoid";

        // Item \\

        public static final String DEFAULT_ITEM_MATERIAL = "items/StandardItems";
        public static final short TOOL_NONE = 0;

        // Font \\

        public static final String FONT_DEFAULT_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
                        + "0123456789 .,!?:;'\"-+*/\\()[]{}@#$%^&=<>|~`_";
        public static final String FONT_DEFAULT_NAME = "MontserratAlternates";
        public static final Color FONT_DEFAULT_COLOR = Color.RED;
        public static final String FONT_DEFAULT_MATERIAL = "util/FontDefault";
        public static final String FONT_DEFAULT_MESH = "util/LabelQuad";
        public static final String FONT_DEFAULT_VAO = "util/vao/LabelVAO";
        public static final String FONT_DEFAULT_SIZE_PERCENT = "50%";
        public static final float FONT_LETTER_SPACING_RATIO = 0.05f;
        public static final float FONT_SPACE_WIDTH_RATIO = 0.25f;
        public static final int FONT_DEFAULT_OFFSET_INDEX_X = 0;
        public static final int FONT_DEFAULT_OFFSET_INDEX_Y = 1;
        public static final int FONT_RASTER_SIZE = 24;

        // Menu \\

        public static final String ELEMENT_DEFAULT_SIZE = "100%";
        public static final String ELEMENT_DEFAULT_POSITION = "0%";
        public static final String ELEMENT_DEFAULT_MIN_SIZE = "0%";
        public static final String ELEMENT_DEFAULT_MAX_SIZE = "100%";
        public static final int MAX_MASK_DEPTH = 8;
        public static final float DROPDOWN_COLLAPSE_TOLERANCE = 12f;
}
