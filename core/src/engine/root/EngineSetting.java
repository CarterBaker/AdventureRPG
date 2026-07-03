package engine.root;

import engine.graphics.color.Color;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class EngineSetting {

        /*
         * Central compile-time constant registry. Runtime systems read shared
         * values from this class instead of embedding literals in logic.
         */

        // GL — Buffers \\

        public static final int GL_ARRAY_BUFFER = 0x8892;
        public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
        public static final int GL_UNIFORM_BUFFER = 0x8A11;
        public static final int GL_DYNAMIC_DRAW = 0x88E8;
        public static final int GL_STATIC_DRAW = 0x88E4;

        // GL — Primitives \\

        public static final int GL_TRIANGLES = 0x0004;
        public static final int GL_UNSIGNED_SHORT = 0x1403;
        public static final int GL_UNSIGNED_BYTE = 0x1401;
        public static final int GL_FLOAT = 0x1406;
        public static final int GL_PATCHES = 0x000E;

        // GL — Textures \\

        public static final int GL_TEXTURE_2D = 0x0DE1;
        public static final int GL_TEXTURE_2D_ARRAY = 0x8C1A;
        public static final int GL_TEXTURE0 = 0x84C0;
        public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
        public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
        public static final int GL_TEXTURE_WRAP_S = 0x2802;
        public static final int GL_TEXTURE_WRAP_T = 0x2803;
        public static final int GL_LINEAR = 0x2601;
        public static final int GL_NEAREST = 0x2600;
        public static final int GL_CLAMP_TO_EDGE = 0x812F;
        public static final int GL_REPEAT = 0x2901;

        // GL — Texture Formats \\

        public static final int GL_RGBA = 0x1908;
        public static final int GL_RGB = 0x1907;
        public static final int GL_DEPTH_COMPONENT = 0x1902;
        public static final int GL_RGBA8 = 0x8058;
        public static final int GL_RGB8 = 0x8051;
        public static final int GL_RGB16F = 0x881B;
        public static final int GL_RGB32F = 0x8815;
        public static final int GL_RGBA16F = 0x881A;
        public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
        public static final int GL_DEPTH_COMPONENT32F = 0x8CAC;

        // GL — State \\

        public static final int GL_DEPTH_TEST = 0x0B71;
        public static final int GL_BLEND = 0x0BE2;
        public static final int GL_CULL_FACE = 0x0B44;
        public static final int GL_SCISSOR_TEST = 0x0C11;
        public static final int GL_SRC_ALPHA = 0x0302;
        public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
        public static final int GL_BACK = 0x0405;
        public static final int GL_CCW = 0x0901;
        public static final int GL_LEQUAL = 0x0203;
        public static final int GL_NO_ERROR = 0;

        // GL — Shaders \\

        public static final int GL_VERTEX_SHADER = 0x8B31;
        public static final int GL_FRAGMENT_SHADER = 0x8B30;
        public static final int GL_LINK_STATUS = 0x8B82;
        public static final int GL_COMPILE_STATUS = 0x8B81;

        // GL — Framebuffers \\

        public static final int GL_FRAMEBUFFER = 0x8D40;
        public static final int GL_RENDERBUFFER = 0x8D41;
        public static final int GL_COLOR_BUFFER_BIT = 0x4000;
        public static final int GL_DEPTH_BUFFER_BIT = 0x0100;
        public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
        public static final int GL_COLOR_ATTACHMENT1 = 0x8CE1;
        public static final int GL_COLOR_ATTACHMENT2 = 0x8CE2;
        public static final int GL_COLOR_ATTACHMENT3 = 0x8CE3;
        public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
        public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;

        // GL — Tessellation \\

        public static final int GL_PATCH_VERTICES = 0x8E72;

        // Application \\

        public static final String GAME_DIRECTORY = "AdventureRPG";
        public static final String GAME_DOCUMENTS_SUBPATH = "Documents/My Games";
        public static final String SETTINGS_FILE_NAME = "Settings.json";
        public static final String EDITOR_SETTINGS_FILE_NAME = "EditorSettings.json";
        public static final String BIN_DIRECTORY = "bin";
        public static final String EDITOR_LAYOUT_DIRECTORY = "editorLayout";
        public static final String EDITOR_LAYOUT_SESSION_FILE = "LastSession.json";

        // Engine \\

        public static final String VERSION = "0.0.0.1a";
        public static final int LOADER_BATCH_SIZE = 32;

        // Registry \\

        public static final int FNV_OFFSET_BASIS = 0x811c9dc5;
        public static final int FNV_PRIME = 0x01000193;
        public static final short REGISTRY_RESERVED_ID = 0;

        // Sentinel Values \\

        public static final int GL_HANDLE_NONE = 0;
        public static final int GL_INVALID_INDEX = 0xFFFFFFFF;
        public static final int INDEX_NOT_FOUND = -1;

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
        public static final ObjectArraySet<String> TCS_FILE_EXTENSIONS = new ObjectArraySet<>(
                        new String[] { "tcs", "tesc" });
        public static final ObjectArraySet<String> TES_FILE_EXTENSIONS = new ObjectArraySet<>(
                        new String[] { "tes", "tese" });

        // Asset Paths \\

        public static final String BEHAVIOR_JSON_PATH = "behaviors";
        public static final String BIOME_JSON_PATH = "biomes";
        public static final String BLOCK_JSON_PATH = "blocks";
        public static final String BLOCK_TEXTURE_ALIAS_PATH = "texturealiases";
        public static final String BLOCK_TEXTURE_PATH = "textures";
        public static final String CALENDAR_JSON_PATH = "calendars";
        public static final String ENTITY_JSON_PATH = "entities";
        public static final String FBO_CATALOG_JSON_PATH = "application/fbos";
        public static final String FONT_PATH = "fonts";
        public static final String ITEM_JSON_PATH = "items";
        public static final String MATERIAL_JSON_PATH = "materials";
        public static final String MENU_JSON_PATH = "menus";
        public static final String MESH_JSON_PATH = "mesh";
        public static final String PASS_JSON_PATH = "processingpasses";
        public static final String SHADER_PATH = "shaders";
        public static final String RIG_JSON_PATH = "rigs";
        public static final String SPRITE_PATH = "sprites";
        public static final String THREAD_CATALOG_PATH = "application/threads";
        public static final String TOOL_TYPE_JSON_PATH = "tools";
        public static final String UBO_JSON_PATH = "ubos";
        public static final String WORLD_TEXTURE_PATH = "worlds";
        public static final String WEATHER_JSON_PATH = "weathers";
        public static final String CLOUD_JSON_PATH = "clouds";
        public static final String SEASON_JSON_PATH = "seasons";

        // Settings \\

        public static final String SETTINGS_UBO = "SettingsData";

        // Window \\

        public static final String WINDOW_TITLE = "TerraArcana";
        public static final int MIN_WINDOW_DIMENSION = 64;

        // Blit \\

        public static final String DEFAULT_BLIT_MATERIAL = "fullscreen/Blit";
        public static final String DEFAULT_BLIT_MESH = "util/Blit";

        // Sprite \\

        public static final String SPRITE_DEFAULT_MATERIAL = "util/SpriteDefault";
        public static final String SPRITE_DEFAULT_MESH = "util/Sprite";

        // Cursor \\

        public static final int CURSOR_DEFAULT = 0;
        public static final int CURSOR_RESIZE_H = 1;
        public static final int CURSOR_RESIZE_V = 2;

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

        // Shader Pipeline \\

        public static final int SHADER_ALIAS_LIBRARY_INITIAL_CAPACITY = 16;
        public static final int SHADER_ALIAS_LIBRARY_GROWTH_FACTOR = 2;
        public static final float SHADER_ALIAS_DEFAULT_ALPHA = 1.0f;
        public static final int SHADER_UBO_UNSPECIFIED_BINDING = INDEX_NOT_FOUND;
        public static final int GL_TESS_CONTROL_SHADER = 0x8E88;
        public static final int GL_TESS_EVALUATION_SHADER = 0x8E87;

        // Composite Rendering \\

        public static final int COMPOSITE_UPLOAD_BUFFER_GROWTH_FACTOR = 2;
        public static final int COMPOSITE_UPLOAD_VERSION_UNINITIALIZED = INDEX_NOT_FOUND;

        // Camera \\

        public static final float CAMERA_NEAR_PLANE = 0.1f;
        public static final float CAMERA_FAR_PLANE = 1000f;
        public static final float CAMERA_MAX_PITCH_DEGREES = 89f;

        // Camera UBO Uniforms \\

        public static final String UNIFORM_CAM_FAR_PLANE = "u_farPlane";
        public static final String UNIFORM_CAM_FOV = "u_cameraFOV";
        public static final String UNIFORM_CAM_INVERSE_PROJECTION = "u_inverseProjection";
        public static final String UNIFORM_CAM_INVERSE_VIEW = "u_inverseView";
        public static final String UNIFORM_CAM_NEAR_PLANE = "u_nearPlane";
        public static final String UNIFORM_CAM_POSITION = "u_cameraPosition";
        public static final String UNIFORM_CAM_PROJECTION = "u_projection";
        public static final String UNIFORM_CAM_VIEW = "u_view";
        public static final String UNIFORM_CAM_VIEW_PROJECTION = "u_viewProjection";
        public static final String UNIFORM_CAM_VIEWPORT = "u_viewport";

        // Ortho UBO Uniforms \\

        public static final String UNIFORM_ORTHO_PROJECTION = "u_orthoProjection";
        public static final String UNIFORM_ORTHO_SCREEN_SIZE = "u_screenSize";

        // Frustum Culling \\

        public static final float FRUSTUM_ALWAYS_VISIBLE_DIST_SQ = 4.5f;
        public static final float FRUSTUM_CHUNK_BLEED_SCALE = 0.75f;
        public static final float FRUSTUM_HALF_PI = (float) (Math.PI / 2f);
        public static final float FRUSTUM_MIN_BLEED = 0.1f;
        public static final float FRUSTUM_PI = (float) Math.PI;
        public static final float FRUSTUM_PITCH_MIN_DIST_SQ = 25f;
        public static final float FRUSTUM_PITCH_POWER_ANGLE = 1f;
        public static final float FRUSTUM_PITCH_POWER_DISTANCE = 6f;
        public static final float FRUSTUM_TWO_PI = (float) (Math.PI * 2f);

        // Texture \\

        public static final String TEXTURE_UV_SCALE_UNIFORM = "u_uvPerBlock";

        // Mesh \\

        public static final int CHUNK_VERT_BUFFER = 128;
        public static final int DEFAULT_BLOCK_DIRECTION = 4;
        public static final int MESH_VERT_LIMIT = 32767;

        // Geometry \\

        public static final int QUAD_VERTEX_COUNT = 4;
        public static final int QUAD_INDEX_COUNT = 6;
        public static final int COMPOSITE_BUFFER_INITIAL_CAPACITY = 64;

        // UBOs \\

        public static final String CAMERA_DATA_UBO = "CameraData";
        public static final String SUN_LIGHT_UBO = "SunLightData";
        public static final String MOON_LIGHT_UBO = "MoonLightData";
        public static final String GRID_COORDINATE_UBO = "GridCoordinateData";
        public static final String ITEM_ROTATION_UBO = "ItemRotationData";
        public static final String ORTHO_DATA_UBO = "OrthoData";
        public static final String PLAYER_POSITION_UBO = "PlayerPositionData";
        public static final String UBO_TIME_DATA_NAME = "TimeData";

        // Sun/Moon UBO Uniforms \\

        public static final String UNIFORM_SUN_DIRECTION = "u_sunDirection";
        public static final String UNIFORM_SUN_INTENSITY = "u_sunIntensity";
        public static final String UNIFORM_SUN_COLOR = "u_sunColor";
        public static final String UNIFORM_MOON_DIRECTION = "u_moonDirection";
        public static final String UNIFORM_MOON_INTENSITY = "u_moonIntensity";
        public static final String UNIFORM_MOON_COLOR = "u_moonColor";

        // Post Processing \\

        public static final String SSAO_DATA_UBO = "SSAOData";
        public static final int SSAO_KERNEL_SIZE = 64;

        // Block \\

        public static final String AIR_BLOCK_NAME = "TerraArcana/Air";
        public static final short DEFAULT_BLOCK_ORIENTATION = (short) (DEFAULT_BLOCK_DIRECTION * 4);
        public static final int ENCODED_FACE_NATURAL_FULL_OFFSET = 24;

        // World \\

        public static final String CHUNK_VAO = "util/vao/ChunkVAO";
        public static final String DEFAULT_BIOME_NAME = "Empty";
        public static final int CHUNK_POOL_MAX_OVERFLOW = 32;
        public static final int GRID_SLOTS_SCAN_PER_FRAME = 32;
        public static final int MAX_CHUNK_STREAM_PER_BATCH = 32;
        public static final int MAX_CHUNK_STREAM_PER_FRAME = 128;
        public static final int MAX_CHUNK_STREAM_PER_QUEUE = 1024;
        public static final int MEGA_ASSESS_PER_FRAME = GRID_SLOTS_SCAN_PER_FRAME / MEGA_CHUNK_SIZE;
        public static final int MEGA_POOL_MAX_OVERFLOW = 8;
        public static final String STARTING_WORLD = "TerraArcana";

        // World Defaults \\

        public static final String DEFAULT_CALENDAR_NAME = "standard/Default";
        public static final float DEFAULT_DAYS_PER_DAY = 20.0f;
        public static final float DEFAULT_GRAVITY_MULTIPLIER = 1.0f;
        public static final float DEFAULT_GRAVITY_X = 0.0f;
        public static final float DEFAULT_GRAVITY_Y = -1.0f;
        public static final float DEFAULT_GRAVITY_Z = 0.0f;

        // Sky \\

        public static final String SKY_COLOR_UBO = "SkyColorData";
        public static final float SKY_FOG_BLEND_START = 0.4f;
        public static final float SKY_ALTITUDE_POWER = 1.2f;

        // Weather \\

        public static final String WEATHER_DATA_UBO = "WeatherData";
        public static final String WEATHER_REGION_DATA_UBO = "WeatherRegionData";
        public static final int WEATHER_REGION_SAMPLE_DISTANCE = 96; // chunk units between cardinal samples
        public static final float WEATHER_NOISE_CELL_SIZE = 256.0f; // chunk units per noise cell
        public static final float WEATHER_WIND_DRIFT_SPEED = 0.01f; // noise-space units per second

        // Horizon & Overhead \\

        public static final float HORIZON_DISTANCE = 1024.0f; // Radius from the player used for weather sampling
        public static final int OVERHEAD_CELL_SIZE = 64; // chunk units per overhead cell edge
        public static final int OVERHEAD_MAX_STREAM_PER_FRAME = 4; // max overhead cells created/released per frame

        // Wind \\

        public static final float WIND_GLOBAL_DIRECTION_DEGREES = 45.0f; // fixed planetary prevailing airflow
        public static final float WIND_GLOBAL_SPEED = 1.0f; // base wind speed scale — not a real-world unit yet
        public static final float WIND_SPRING_DIRECTION_OFFSET = 10.0f; // degrees added to the global angle
        public static final float WIND_SUMMER_DIRECTION_OFFSET = -15.0f;
        public static final float WIND_FALL_DIRECTION_OFFSET = 20.0f;
        public static final float WIND_WINTER_DIRECTION_OFFSET = -25.0f;
        public static final float WIND_SPRING_SPEED_MUL = 1.00f;
        public static final float WIND_SUMMER_SPEED_MUL = 0.85f;
        public static final float WIND_FALL_SPEED_MUL = 1.15f;
        public static final float WIND_WINTER_SPEED_MUL = 1.35f;

        // Sky Noise
        public static final float SKY_NOISE_ALTITUDE_BLEND = 0.35f;
        public static final float SKY_NOISE_TEXTURE_STRENGTH = 0.02f;

        // Sky Seasonal
        public static final float SKY_SEASONAL_TINT_OFFSET_SCALE = 0.15f;
        public static final float SKY_SEASONAL_STRENGTH_SCALE = 0.50f;
        public static final float SKY_HORIZON_DESATURATION = 0.12f;

        // Sky Daily Variation
        public static final float SKY_DAILY_OFFSET_R_SCALE = 0.08f;
        public static final float SKY_DAILY_OFFSET_R_BIAS = -0.04f;
        public static final float SKY_DAILY_OFFSET_G_SCALE = 0.06f;
        public static final float SKY_DAILY_OFFSET_G_BIAS = -0.03f;
        public static final float SKY_DAILY_OFFSET_B_SCALE = 0.04f;
        public static final float SKY_DAILY_OFFSET_B_BIAS = -0.02f;
        public static final float SKY_DAILY_HASH_G = 7919.0f;
        public static final float SKY_DAILY_HASH_B = 5333.0f;

        // Sky Palette - Night
        public static final float SKY_NIGHT_TOP_R = 0.020f;
        public static final float SKY_NIGHT_TOP_G = 0.020f;
        public static final float SKY_NIGHT_TOP_B = 0.080f;
        public static final float SKY_NIGHT_BOTTOM_R = 0.005f;
        public static final float SKY_NIGHT_BOTTOM_G = 0.005f;
        public static final float SKY_NIGHT_BOTTOM_B = 0.020f;

        // Sky Palette - Day
        public static final float SKY_DAY_TOP_R = 0.60f;
        public static final float SKY_DAY_TOP_G = 0.82f;
        public static final float SKY_DAY_TOP_B = 1.00f;
        public static final float SKY_DAY_BOTTOM_R = 0.32f;
        public static final float SKY_DAY_BOTTOM_G = 0.52f;
        public static final float SKY_DAY_BOTTOM_B = 0.80f;

        // Sky Season Tints
        public static final float SKY_WINTER_TINT_R = 0.70f;
        public static final float SKY_WINTER_TINT_G = 0.75f;
        public static final float SKY_WINTER_TINT_B = 0.95f;
        public static final float SKY_SUMMER_TINT_R = 1.05f;
        public static final float SKY_SUMMER_TINT_G = 0.95f;
        public static final float SKY_SUMMER_TINT_B = 0.85f;
        public static final float SKY_SPRING_TINT_R = 1.00f;
        public static final float SKY_SPRING_TINT_G = 0.90f;
        public static final float SKY_SPRING_TINT_B = 0.80f;
        public static final float SKY_FALL_TINT_R = 0.95f;
        public static final float SKY_FALL_TINT_G = 0.80f;
        public static final float SKY_FALL_TINT_B = 0.70f;

        // Sky Sunrise/Sunset Colors
        public static final float SKY_WINTER_SUNRISE_R = 0.80f;
        public static final float SKY_WINTER_SUNRISE_G = 0.50f;
        public static final float SKY_WINTER_SUNRISE_B = 0.60f;
        public static final float SKY_SUMMER_SUNRISE_R = 1.00f;
        public static final float SKY_SUMMER_SUNRISE_G = 0.50f;
        public static final float SKY_SUMMER_SUNRISE_B = 0.25f;
        public static final float SKY_SPRING_SUNRISE_R = 0.95f;
        public static final float SKY_SPRING_SUNRISE_G = 0.65f;
        public static final float SKY_SPRING_SUNRISE_B = 0.45f;
        public static final float SKY_FALL_SUNRISE_R = 0.85f;
        public static final float SKY_FALL_SUNRISE_G = 0.45f;
        public static final float SKY_FALL_SUNRISE_B = 0.25f;

        // Lighting \\

        public static final float MOON_BRIGHTNESS_BASE = 0.7f;
        public static final float MOON_BRIGHTNESS_LUNAR_SCALE = 0.3f;
        public static final float MOON_COLOR_B = 1.0f;
        public static final float MOON_COLOR_G = 0.85f;
        public static final float MOON_COLOR_R = 0.75f;
        public static final float MOON_HORIZON_CUTOFF = 0.85f;
        public static final float MOON_MAX_INTENSITY = 0.25f;
        public static final float MOON_PHASE_MAX = 0.95f;
        public static final float MOON_PHASE_MIN = 0.05f;
        public static final float SUN_BLEND_THRESHOLD = 0.15f;
        public static final float SUN_HORIZON_CUTOFF = 0.85f;

        // Time \\

        public static final int DAYS_PER_DAY = 20;
        public static final int HOURS_PER_DAY = 24;
        public static final int LUNAR_CYCLE_DAYS = 28;
        public static final long MILLIS_PER_REAL_DAY = 86400000L;
        public static final float MIDDAY_OFFSET = 0.5f;
        public static final int MINUTES_PER_HOUR = 60;
        public static final int YEARS_PER_AGE = 1500;

        // Starting Time \\

        public static final int STARTING_AGE = 3;
        public static final int STARTING_DAY_OF_MONTH = 15;
        public static final int STARTING_HOUR = 12;
        public static final int STARTING_MINUTE = 30;
        public static final int STARTING_MONTH = 6;
        public static final int STARTING_YEAR = 1356;

        // Clock \\

        public static final double CLOCK_MAX_SEASON_SHIFT = 0.08;
        public static final double CLOCK_MIDNIGHT = 0.0;
        public static final double CLOCK_NOISE_DIVISOR = 1.6777216E7;
        public static final long CLOCK_NOISE_MASK = 16777215L;
        public static final double CLOCK_NOISE_MIN = 0.001;
        public static final long CLOCK_NOISE_MULTIPLIER = -49064778989728563L;
        public static final double CLOCK_NOON = 0.5;
        public static final double CLOCK_QUARTER = 0.25;
        public static final double CLOCK_SUNRISE_MAX = 0.40;
        public static final double CLOCK_SUNRISE_MIN = 0.05;
        public static final double CLOCK_SUNSET_MAX = 0.95;
        public static final double CLOCK_SUNSET_MIN = 0.60;
        public static final double CLOCK_THREE_QUARTERS = 0.75;

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

        // Rig \\

        public static final float DEFAULT_BONE_SIZE = 0.25f;
        public static final int MAX_BONE_INFLUENCES = 4;
        public static final float BONE_WEIGHT_SUM_EPSILON = 0.001f;

        // Player \\

        public static final float BLOCK_PLACEMENT_INTERVAL = 0.1f;
        public static final String DEFAULT_PLAYER_RACE = "Humanoid";

        // Item \\

        public static final String DEFAULT_ITEM_MATERIAL = "items/StandardItems";
        public static final short TOOL_NONE = 0;

        // Font \\

        public static final String FONT_DEFAULT_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,!?:;'\"-+*/\\()[]{}@#$%^&=<>|~`_";
        public static final Color FONT_DEFAULT_COLOR = Color.RED;
        public static final int FONT_DEFAULT_OFFSET_INDEX_X = 0;
        public static final int FONT_DEFAULT_OFFSET_INDEX_Y = 1;
        public static final String FONT_DEFAULT_MATERIAL = "util/FontDefault";
        public static final String FONT_DEFAULT_MESH = "util/LabelQuad";
        public static final String FONT_DEFAULT_NAME = "MontserratAlternates";
        public static final String FONT_DEFAULT_SIZE_PERCENT = "50%";
        public static final String FONT_DEFAULT_VAO = "util/vao/LabelVAO";
        public static final float FONT_LETTER_SPACING_RATIO = 0.05f;
        public static final int FONT_RASTER_SIZE = 24;
        public static final float FONT_SPACE_WIDTH_RATIO = 0.25f;

        // Menu \\

        public static final String MENU_TAB_SHELL = "editor/TabFrame/TabFrame";
        public static final String MENU_TAB_GHOST = "editor/TabFrame/TabGhost";
        public static final String TAB_TITLE_PREVIEW = "Preview";
        public static final String WINDOW_TITLE_EDITOR_SECONDARY = "Secondary";
        public static final float DROPDOWN_COLLAPSE_TOLERANCE = 12f;
        public static final String ELEMENT_DEFAULT_MAX_SIZE = "100%";
        public static final String ELEMENT_DEFAULT_MIN_SIZE = "0%";
        public static final String ELEMENT_DEFAULT_POSITION = "0%";
        public static final String ELEMENT_DEFAULT_SIZE = "100%";
        public static final int MAX_MASK_DEPTH = 8;

        // Tab Drag \\

        public static final float DIVIDER_HIT_TOLERANCE = 6f;
        public static final float RATIO_MIN = 0.1f;
        public static final float RATIO_MAX = 0.9f;
        public static final int TAB_DRAG_PREVIEW_W = 256;
        public static final int TAB_DRAG_PREVIEW_H = 144;
        public static final int TAB_DEFAULT_TAB_DEPTH = 1;
        public static final int TAB_DEFAULT_CONTENT_DEPTH = 2;
        public static final int TAB_DRAG_GHOST_DEPTH = 3;
        public static final int TAB_DRAG_TAB_DEPTH = 4;
        public static final int TAB_DRAG_CONTENT_DEPTH = 5;
        public static final float TAB_DRAG_EDGE_FRACTION = 0.25f;
        public static final String TAB_ZONE_GHOST_WINDOW_TITLE = "TabZoneGhost";
}
