package engine.root;

import engine.graphics.color.Color;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class EngineSetting {

    /*
     * Every constant the engine needs or that more than one context shares —
     * GL enums, paths, registry sentinels, tuning values, uniform and block
     * names, and launcher defaults. Context-only constants live in that
     * context's own Setting class.
     */

    // OpenGL Constants
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_BACK = 0x0405;
    public static final int GL_BGRA = 0x80E1;
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_CCW = 0x0901;
    public static final int GL_CLAMP_TO_EDGE = 0x812F;
    public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int GL_COLOR_BUFFER_BIT = 0x4000;
    public static final int GL_COMPILE_STATUS = 0x8B81;
    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    public static final int GL_DEPTH_BUFFER_BIT = 0x0100;
    public static final int GL_DEPTH_COMPONENT = 0x1902;
    public static final int GL_DEPTH_COMPONENT32F = 0x8CAC;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_FRAMEBUFFER = 0x8D40;
    public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    public static final int GL_FRONT = 0x0404;
    public static final int GL_LEQUAL = 0x0203;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_LINK_STATUS = 0x8B82;
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_ONE = 0x0001;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_PATCHES = 0x000E;
    public static final int GL_PATCH_VERTICES = 0x8E72;
    public static final int GL_PIXEL_PACK_BUFFER = 0x88EB;
    public static final int GL_QUERY_RESULT = 0x8866;
    public static final int GL_QUERY_RESULT_AVAILABLE = 0x8867;
    public static final int GL_RENDERBUFFER = 0x8D41;
    public static final int GL_REPEAT = 0x2901;
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGB16F = 0x881B;
    public static final int GL_RGB32F = 0x8815;
    public static final int GL_RGB8 = 0x8051;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_RGBA16F = 0x881A;
    public static final int GL_RGBA32F = 0x8814;
    public static final int GL_RGBA8 = 0x8058;
    public static final int GL_SCISSOR_TEST = 0x0C11;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_STREAM_READ = 0x88E1;
    public static final int GL_TEXTURE0 = 0x84C0;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_TEXTURE_2D_ARRAY = 0x8C1A;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_TIMESTAMP = 0x8E28;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_VERTEX_SHADER = 0x8B31;

    // Buffers
    public static final int SCRATCH_BUFFER_GROWTH_FACTOR = 2;

    // Uniform Components
    public static final int VECTOR2_COMPONENT_COUNT = 2;
    public static final int VECTOR3_COMPONENT_COUNT = 3;
    public static final int VECTOR4_COMPONENT_COUNT = 4;
    public static final int MATRIX2_ELEMENT_COUNT = 4;
    public static final int MATRIX3_ELEMENT_COUNT = 9;
    public static final int MATRIX4_ELEMENT_COUNT = 16;

    // Sentinel & Utility Values
    public static final int AXIS_COUNT = 3;
    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_Z = 2;
    public static final String[] AXIS_KEYS = { "x", "y", "z" };
    public static final int BOX_INT_STRIDE = 6;
    public static final int BOX_MAX_X = 3;
    public static final int BOX_MAX_Y = 4;
    public static final int BOX_MAX_Z = 5;
    public static final int BOX_MIN_X = 0;
    public static final int BOX_MIN_Y = 1;
    public static final int BOX_MIN_Z = 2;
    public static final int BOX_CORNER_BIT_X = 1;
    public static final int BOX_CORNER_BIT_Y = 2;
    public static final int BOX_CORNER_BIT_Z = 4;
    public static final int BOX_CORNER_COUNT = 8;
    public static final float COLOR_CHANNEL_BYTE_MAX = 255f;
    public static final int COLOR_CHANNEL_COUNT = 4;
    public static final float COLOR_CHANNEL_MAX = 1.0f;
    public static final float COLOR_CHANNEL_MIN = 0.0f;
    public static final int PACKED_COLOR_ALPHA_SHIFT = 24;
    public static final int PACKED_COLOR_CHANNEL_MASK = 0xFF;
    public static final int PACKED_COLOR_GREEN_SHIFT = 8;
    public static final int PACKED_COLOR_RED_SHIFT = 16;
    public static final int PACKED_COLOR_WHITE = 0xFFFFFF;
    public static final int FNV_OFFSET_BASIS = 0x811c9dc5;
    public static final int FNV_PRIME = 0x01000193;
    public static final int GL_HANDLE_NONE = 0;
    public static final int GL_INVALID_INDEX = 0xFFFFFFFF;
    public static final int GL_INVALID_LOCATION = -1;
    public static final int STD140_ARRAY_STRIDE_ALIGNMENT = 16;
    public static final long HASH_FINALIZER_MULTIPLIER_1 = 0xff51afd7ed558ccdL;
    public static final long HASH_FINALIZER_MULTIPLIER_2 = 0xc4ceb9fe1a85ec53L;
    public static final int INDEX_NOT_FOUND = -1;
    public static final float DIVISION_EPSILON = 0.001f;
    public static final float PERCENT_MAX = 100f;
    public static final float QUARTER_TURN_DEGREES = 90f;
    public static final float HALF_TURN_DEGREES = 180f;
    public static final double NOISE_SEAM_BLEND_WAVELENGTHS = 3.0;
    public static final short REGISTRY_RESERVED_ID = 0;
    public static final int REGISTRY_ID_NONE = -1;
    public static final int REGISTRY_SHORT_ID_COUNT = 0x8000;
    public static final int REGISTRY_INT_ID_COUNT = Integer.MAX_VALUE;
    public static final int REGISTRY_ITEM_ID_COUNT = 0x10000;
    public static final int REGISTRY_ITEM_ID_SHIFT = 16;
    public static final int REGISTRY_NAME_SEED_MASK = 0x7FFF;

    // Engine & Application
    public static final String BIN_DIRECTORY = "bin";
    public static final int CHARACTER_NAME_FIRST_NUMBER = 1;
    public static final String CHARACTER_NAME_PREFIX = "Character ";
    public static final String CHARACTER_SAVE_DIRECTORY = "Characters";
    public static final String EDITOR_LAYOUT_DIRECTORY = "editorLayout";
    public static final String EDITOR_SETTINGS_FILE_NAME = "EditorSettings.arpg";
    public static final String GAME_DIRECTORY = "AdventureRPG";
    public static final String GAME_DOCUMENTS_SUBPATH = "Documents/My Games";
    public static final String TEMPORARY_FILE_SUFFIX = ".tmp";
    public static final int LOADER_BATCH_SIZE = 32;
    public static final String SAVE_DIRECTORY = "Saves";
    public static final String SETTINGS_FILE_NAME = "Settings.arpg";
    public static final String SETTINGS_UBO = "SettingsData";
    public static final String POST_PROCESS_UBO = "PostProcessData";
    public static final String VERSION = "0.0.0.1a";

    // Logging
    public static final String LOG_DIRECTORY = "Logs";
    public static final String LOG_DIRECTORY_PROPERTY = "adventurerpg.logDirectory";
    public static final String LOG_FILE_NAME_FORMAT = "AdventureRPG_%s_%s.log";
    public static final String LOG_FILE_TIMESTAMP_PATTERN = "yyyy-MM-dd_HH-mm-ss";
    public static final int LOG_HISTORY_CAPACITY = 4096;
    public static final String LOG_LINE_BREAK_PATTERN = "\\R";
    public static final String LOG_LINE_FORMAT = "[%s] [%s] %s";
    public static final String LOG_SESSION_EDITOR = "Editor";
    public static final String LOG_SESSION_GAME = "Game";
    public static final String LOG_TIME_PATTERN = "HH:mm:ss.SSS";

    // File Paths & Extensions
    public static final String ANIMATION_PATH = "animations";
    public static final String ANIMATION_TREE_PATH = "animationtrees";
    public static final String BEHAVIOR_PATH = "behaviors";
    public static final String BIOME_PATH = "biomes";
    public static final String BLOCK_PATH = "blocks";
    public static final String BLOCK_TEXTURE_ALIAS_PATH = "texturealiases";
    public static final String BLOCK_TEXTURE_PATH = "textures";
    public static final String CALENDAR_PATH = "calendars";
    public static final String CLOUD_PATH = "clouds";
    public static final String ENTITY_PATH = "entities";
    public static final String FEATURE_PATH = "features";
    public static final String FBO_CATALOG_PATH = "application/fbos";
    public static final ObjectArraySet<String> FONT_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "ttf",
        "otf" });
    public static final String FONT_PATH = "fonts";
    public static final ObjectArraySet<String> FRAG_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "fsh",
        "frag", "fs", "fragment", "pixel" });
    public static final ObjectArraySet<String> INCLUDE_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "glsl",
        "inc", "glslinc" });
    public static final String ITEM_PATH = "items";
    public static final String MATERIAL_PATH = "materials";
    public static final String MENU_PATH = "menus";
    public static final String MESH_PATH = "mesh";
    public static final String PASS_PATH = "processingpasses";
    public static final String RIG_PATH = "rigs";
    public static final String SEASON_PATH = "seasons";
    public static final String SHADER_PATH = "shaders";
    public static final String SPRITE_PATH = "sprites";
    public static final String STRUCTURE_PATH = "structures";
    public static final String TREE_PATH = "trees";
    public static final ObjectArraySet<String> TCS_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "tcs",
        "tesc" });
    public static final ObjectArraySet<String> TES_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "tes",
        "tese" });
    public static final ObjectArraySet<String> TEXTURE_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "png",
        "jpg", "jpeg", "tga", "bmp" });
    public static final String THREAD_CATALOG_PATH = "application/threads";
    public static final String TOOL_TYPE_PATH = "tools";
    public static final String UBO_PATH = "ubos";
    public static final String VEHICLE_PATH = "vehicles";
    public static final ObjectArraySet<String> VERT_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "vsh",
        "vert", "vs", "vertex" });
    public static final String WEATHER_PATH = "weathers";
    public static final String WORLD_TEXTURE_PATH = "worlds";

    // ARPG Files
    public static final String ARPG_FILE_EXTENSION = "arpg";
    public static final ObjectArraySet<String> ARPG_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "arpg" });
    public static final String ARPG_FILE_MAGIC = "ARPG";
    public static final int ARPG_FORMAT_VERSION = 2;
    public static final String ARPG_TEXT_FILE_EXTENSION = "json";

    // ARPG Binary
    public static final int ARPG_TAG_NULL = 0;
    public static final int ARPG_TAG_FALSE = 1;
    public static final int ARPG_TAG_TRUE = 2;
    public static final int ARPG_TAG_INTEGER = 3;
    public static final int ARPG_TAG_DECIMAL = 4;
    public static final int ARPG_TAG_DOUBLE = 5;
    public static final int ARPG_TAG_STRING = 6;
    public static final int ARPG_TAG_ARRAY = 7;
    public static final int ARPG_TAG_OBJECT = 8;
    public static final int ARPG_BYTE_MASK = 0xFF;
    public static final int ARPG_BYTE_BITS = 8;
    public static final int ARPG_VARINT_PAYLOAD_MASK = 0x7F;
    public static final int ARPG_VARINT_CONTINUE_BIT = 0x80;
    public static final int ARPG_VARINT_SHIFT = 7;
    public static final int ARPG_VARINT_MAX_SHIFT = 63;
    public static final int ARPG_DECIMAL_MAX_SCALE = 22;
    public static final long ARPG_DECIMAL_MAX_MANTISSA = 1L << 53;
    public static final int ARPG_DECIMAL_BASE = 10;
    public static final int ARPG_BUFFER_INITIAL_CAPACITY = 256;

    // ARPG Text
    public static final String ARPG_TEXT_INDENT = "  ";
    public static final String ARPG_TEXT_NULL_LITERAL = "null";
    public static final char ARPG_TEXT_BYTE_ORDER_MARK = '\uFEFF';
    public static final char ARPG_TEXT_ESCAPE_LIMIT = 0x20;
    public static final int ARPG_TEXT_UNICODE_DIGITS = 4;
    public static final int ARPG_TEXT_HEX_RADIX = 16;

    // ARPG Tool
    public static final String ARPG_TOOL_COMMAND_ENCODE = "encode";
    public static final String ARPG_TOOL_COMMAND_DECODE = "decode";
    public static final String ARPG_TOOL_COMMAND_CONVERT = "convert";
    public static final String ARPG_TOOL_COMMAND_EXPORT = "export";
    public static final String ARPG_TOOL_COMMAND_VERIFY = "verify";
    public static final int ARPG_TOOL_EXIT_SUCCESS = 0;
    public static final int ARPG_TOOL_EXIT_FAILURE = 1;
    public static final String ARPG_TOOL_USAGE = String.join("\n",
            "ARPG data tool. Every path is relative to the project root.",
            "  encode <text.json> [file.arpg]  Seal one text file (default target: same name, .arpg).",
            "  decode <file.arpg> [text.json]  Open one sealed file as text (default: print it).",
            "  convert <directory>             Seal every .json below the directory and delete the .json.",
            "  export <directory>              Write a .json text copy beside every .arpg below the directory.",
            "  verify <file-or-directory>      Open every .arpg and report any that fail.");

    // Threading & Frame Rate
    public static final int AUTO_THREAD_POOL_RESERVED_CORES = 2;
    public static final int DEFAULT_IN_FLIGHT_MULTIPLIER = 3;
    public static final long FRAME_PACING_SLEEP_CHUNK_NANOS = 1_000_000L;
    public static final long FRAME_PACING_SLEEP_THRESHOLD_NANOS = 2_000_000L;
    public static final int MAX_THREAD_POOL_SIZE = 32;
    public static final int MIN_AUTO_THREAD_POOL_SIZE = 2;
    public static final String WORLD_STREAMING_THREAD_NAME = "WorldStreaming";
    public static final long NANOS_PER_MILLI = 1_000_000L;
    public static final long NANOS_PER_SECOND = 1_000_000_000L;
    public static final int TARGET_FRAME_RATE = 60;

    // Profiler
    public static final float BYTES_PER_MEGABYTE = 1024f * 1024f;
    public static final int PROFILER_CAPTURE_FRAMES = 600;
    public static final String PROFILER_COUNTER_DRAW_CALLS = "Draw Calls";
    public static final String PROFILER_COUNTER_TRIANGLES = "Triangles";
    public static final String PROFILER_DIRECTORY = "Profiler";
    public static final String PROFILER_GPU_FRAME_LABEL = "GPU Frame";
    public static final String PROFILER_GPU_PASS_PREFIX = "Pass: ";
    public static final int PROFILER_GPU_QUERY_GROWTH = 32;
    public static final String PROFILER_GPU_SCREEN_PASS = "Screen";
    public static final int PROFILER_GPU_SLOT_COUNT = 4;
    public static final String PROFILER_GROUP_ENGINE = "Engine";
    public static final String PROFILER_MEMORY_GC_COLLECTIONS = "GC Collections";
    public static final String PROFILER_MEMORY_GC_MILLIS = "GC Time (ms)";
    public static final String PROFILER_MEMORY_HEAP_COMMITTED = "Heap Committed (MB)";
    public static final String PROFILER_MEMORY_HEAP_USED = "Heap Used (MB)";
    public static final String PROFILER_FRAME_LABEL = "Frame";
    public static final int PROFILER_HISTORY_FRAMES = 240;
    public static final float[] PROFILER_PERCENTILES = { 0.5f, 0.95f, 0.99f };
    public static final String PROFILER_REPORT_PREFIX = "Report_";
    public static final String PROFILER_REPORT_TIMESTAMP_PATTERN = "yyyy-MM-dd_HH-mm-ss";
    public static final int PROFILER_REPORT_TOP_ENTRIES = 80;
    public static final long PROFILER_UNSTAMPED = Long.MIN_VALUE;

    // Window & Display
    public static final int CURSOR_RESIZE_H = 1;
    public static final int CURSOR_RESIZE_V = 2;
    public static final int MIN_WINDOW_DIMENSION = 64;
    public static final int WINDOW_POSITION_UNSET = Integer.MIN_VALUE;
    public static final String WINDOW_TITLE = "TerraArcana";
    public static final String LAUNCHER_WINDOW_TITLE = "AdventureRPG";
    public static final String LAUNCHER_EDITOR_WINDOW_TITLE = "AdventureRPG — Editor";
    public static final int OPENGL_VERSION_MAJOR = 4;
    public static final int OPENGL_VERSION_MINOR = 1;
    public static final String USER_HOME_PROPERTY = "user.home";
    public static final String PATH_SEPARATOR = "/";
    public static final String[][] AWT_RASTERIZATION_PROPERTIES = {
            { "java.awt.headless", "true" },
            { "sun.java2d.noddraw", "true" },
            { "sun.java2d.d3d", "false" },
            { "sun.java2d.opengl", "false" } };

    // Input & Bindings
    public static final int BINDING_MOUSE_CODE_OFFSET = 1000;
    public static final int INPUT_KEY_CODE_COUNT = 512;
    public static final int INPUT_MOUSE_BUTTON_COUNT = 8;
    public static final String INPUT_NAME_COMBO_SEPARATOR = " + ";
    public static final String INPUT_NAME_FIELD_SEPARATOR = "_";
    public static final String INPUT_NAME_MOUSE_PREFIX = "Mouse ";
    public static final String INPUT_NAME_UNKNOWN = "Unbound";
    public static final String INPUT_NAME_WORD_SEPARATOR = " ";

    // User Settings
    public static final float FIELD_OF_VIEW_MAX = 110f;
    public static final float FIELD_OF_VIEW_MIN = 50f;
    public static final float MOUSE_SENSITIVITY_MAX = 1.0f;
    public static final float MOUSE_SENSITIVITY_MIN = 0.01f;
    public static final int NEAR_TESSELLATION_RADIUS_MAX = 8;
    public static final int NEAR_TESSELLATION_RADIUS_MIN = 1;
    public static final int RENDER_DISTANCE_MAX = 112;
    public static final int RENDER_DISTANCE_MIN = 16;

    // User Settings — Post Processing
    public static final float BLOOM_INTENSITY_MAX = 1f;
    public static final float BLOOM_INTENSITY_MIN = 0f;
    public static final float BRIGHTNESS_MAX = 1.5f;
    public static final float BRIGHTNESS_MIN = 0.5f;
    public static final float CHROMATIC_ABERRATION_MAX = 1f;
    public static final float CHROMATIC_ABERRATION_MIN = 0f;
    public static final float CONTRAST_MAX = 1.5f;
    public static final float CONTRAST_MIN = 0.5f;
    public static final float DEPTH_OF_FIELD_BLUR_MAX = 1f;
    public static final float DEPTH_OF_FIELD_BLUR_MIN = 0f;
    public static final float DEPTH_OF_FIELD_STRENGTH_MAX = 1f;
    public static final float DEPTH_OF_FIELD_STRENGTH_MIN = 0f;
    public static final float FILM_GRAIN_MAX = 1f;
    public static final float FILM_GRAIN_MIN = 0f;
    public static final float OUTLINE_STRENGTH_MAX = 1f;
    public static final float OUTLINE_STRENGTH_MIN = 0f;
    public static final int OUTLINE_THICKNESS_MAX = 3;
    public static final int OUTLINE_THICKNESS_MIN = 1;
    public static final float SATURATION_MAX = 2f;
    public static final float SATURATION_MIN = 0f;
    public static final float VIGNETTE_MAX = 1f;
    public static final float VIGNETTE_MIN = 0f;

    // World Scale
    public static final int BIOME_SIZE = 4;
    public static final int BLOCK_PALETTE_THRESHOLD = 512;
    public static final float BLOCK_SIZE = 1.0f;
    public static final int CHUNKS_PER_PIXEL = 32;
    public static final int CHUNK_SIZE = 16;
    public static final int MACRO_CHUNK_SIZE = 16;
    public static final int MEGA_CHUNK_SIZE = 4;
    public static final int SUB_BLOCK_DIVISIONS = 2;
    public static final int SUB_VOXEL_RESOLUTION = 16;
    public static final int WORLD_HEIGHT = 64;

    // Natural Noise
    public static final float NATURAL_DETAIL_NORMAL_AMPLITUDE_BLOCKS = 0.22f;
    public static final float NATURAL_EDGE_WARP_DETAIL_SHARE = 0.5f;
    public static final float NATURAL_EDGE_WARP_HORIZONTAL_BLOCKS = 0.35f;
    public static final float NATURAL_EDGE_WARP_VERTICAL_BLOCKS = 0.18f;
    public static final float NATURAL_GROUND_OFFSET_SMOOTHING = 10.0f;
    public static final float NATURAL_NOISE_CELL_BLOCKS = 2.0f;
    public static final int NATURAL_NOISE_CHANNELS = 4;
    public static final float NATURAL_NOISE_MID_TIER_MARGIN_BLOCKS = 512.0f;
    public static final int NATURAL_NOISE_PERIOD_CHUNKS = 4;
    public static final int NATURAL_NOISE_PLANES = 3;
    public static final int NATURAL_NOISE_PERIOD_BLOCKS = NATURAL_NOISE_PERIOD_CHUNKS * CHUNK_SIZE;
    public static final int NATURAL_NOISE_LATTICE_PERIOD =
            (int) (NATURAL_NOISE_PERIOD_BLOCKS / NATURAL_NOISE_CELL_BLOCKS);
    public static final int NATURAL_NOISE_LATTICE_SIZE = NATURAL_NOISE_LATTICE_PERIOD * NATURAL_NOISE_LATTICE_PERIOD;
    public static final float NATURAL_NOISE_PLANE_OFFSET_CELLS = 11.37f;
    public static final long NATURAL_NOISE_SEED = 1592148161L;
    public static final String NATURAL_NOISE_UBO = "NaturalNoiseData";

    // Rendering Pipeline
    public static final float BLIT_FULLSCREEN_SENTINEL = -1f;
    public static final String BLIT_PREMULTIPLIED_UNIFORM = "u_premultiplied";
    public static final String BLIT_RESOLVE_UNIFORM = "u_resolve";
    public static final String CAMERA_DATA_UBO = "CameraData";
    public static final String CELESTIAL_DATA_UBO = "CelestialData";
    public static final int COMPOSITE_BUFFER_INITIAL_CAPACITY = 64;
    public static final int COMPOSITE_UPLOAD_VERSION_UNINITIALIZED = INDEX_NOT_FOUND;
    public static final String DEFAULT_BLIT_MATERIAL = "util/BlitMaterial";
    public static final String DEFAULT_BLIT_MESH = "util/BlitMesh";
    public static final int DEFAULT_BLOCK_DIRECTION = 4;
    public static final float DEFAULT_FBO_RESOLUTION_SCALE = 1.0f;
    public static final float FRUSTUM_ALWAYS_VISIBLE_DIST_SQ = 4.5f;
    public static final float FRUSTUM_CHUNK_BLEED_SCALE = 0.75f;
    public static final float FRUSTUM_HALF_PI = (float) (Math.PI / 2f);
    public static final float FRUSTUM_MIN_BLEED = 0.1f;
    public static final float FRUSTUM_PI = (float) Math.PI;
    public static final float FRUSTUM_PITCH_MIN_DIST_SQ = 25f;
    public static final float FRUSTUM_PITCH_POWER_ANGLE = 1f;
    public static final float FRUSTUM_PITCH_POWER_DISTANCE = 6f;
    public static final float FRUSTUM_TWO_PI = (float) (Math.PI * 2f);
    public static final int GL_TESS_CONTROL_SHADER = 0x8E88;
    public static final int GL_TESS_EVALUATION_SHADER = 0x8E87;
    public static final String GRID_COORDINATE_UBO = "GridCoordinateData";
    public static final int MAX_RENDER_CALLS_PER_FRAME = 16384;
    public static final int MESH_VERT_LIMIT = 32767;
    public static final String MOON_LIGHT_UBO = "MoonLightData";
    public static final String ORTHO_DATA_UBO = "OrthoData";
    public static final String PLAYER_POSITION_UBO = "PlayerPositionData";
    public static final int QUAD_INDEX_COUNT = 6;
    public static final int QUAD_VERTEX_COUNT = 4;
    public static final String SHADER_ALIAS_ALBEDO = "Albedo";
    public static final float SHADER_ALIAS_DEFAULT_ALPHA = 1.0f;
    public static final int SHADER_ALIAS_LIBRARY_GROWTH_FACTOR = 2;
    public static final int SHADER_ALIAS_LIBRARY_INITIAL_CAPACITY = 16;
    public static final int SHADER_UBO_UNSPECIFIED_BINDING = INDEX_NOT_FOUND;
    public static final String SPRITE_COLOR_UNIFORM = "u_color";
    public static final Color SPRITE_DEFAULT_COLOR = Color.WHITE;
    public static final String SPRITE_DEFAULT_MATERIAL = "sprites/StandardSpriteMaterial";
    public static final String SPRITE_DEFAULT_MESH = "sprites/SpriteMesh";
    public static final String SPRITE_STRETCH_UNIFORM = "u_stretch";
    public static final String SUN_LIGHT_UBO = "SunLightData";
    public static final String TEXTURE_UV_SCALE_UNIFORM = "u_uvPerBlock";
    public static final int TRIANGLE_VERTEX_COUNT = 3;
    public static final String UBO_TIME_DATA_NAME = "TimeData";

    // Camera
    public static final float CAMERA_FAR_PLANE = 10000f;
    public static final float CAMERA_FIRST_PERSON_THRESHOLD = 0.1f;
    public static final float CAMERA_MAX_PITCH_DEGREES = 89f;
    public static final float CAMERA_MIN_FOV_DEGREES = 1f;
    public static final float CAMERA_MAX_FOV_DEGREES = 179f;
    public static final float CAMERA_MIN_VIEWPORT_DIMENSION = 1f;
    public static final float CAMERA_NEAR_PLANE = 0.1f;
    public static final float CAMERA_ZOOM_DEFAULT = 4f;
    public static final float CAMERA_ZOOM_MAX = 6f;
    public static final float CAMERA_ZOOM_MIN = 0f;
    public static final float CAMERA_ZOOM_SCROLL_SPEED = 0.75f;
    public static final float CAMERA_ZOOM_SMOOTHING = 10f;
    public static final float CHARACTER_PREVIEW_CENTER_HEIGHT = 0.5f;
    public static final float CHARACTER_PREVIEW_DISTANCE = 2.1f;
    public static final float CHARACTER_PREVIEW_FOCUS_HEIGHT = 0.55f;
    public static final float CHARACTER_PREVIEW_LATERAL_OFFSET = 0.26f;
    public static final float CHARACTER_PREVIEW_LIFT = 0.2f;
    public static final String UNIFORM_CAM_FAR_PLANE = "u_farPlane";
    public static final String UNIFORM_CAM_FOV = "u_cameraFOV";
    public static final String UNIFORM_CAM_INVERSE_PROJECTION = "u_inverseProjection";
    public static final String UNIFORM_CAM_INVERSE_VIEW = "u_inverseView";
    public static final String UNIFORM_CAM_NEAR_PLANE = "u_nearPlane";
    public static final String UNIFORM_CAM_POSITION = "u_cameraPosition";
    public static final String UNIFORM_CAM_PROJECTION = "u_projection";
    public static final String UNIFORM_CAM_VIEW = "u_view";
    public static final String UNIFORM_CAM_VIEWPORT = "u_viewport";
    public static final String UNIFORM_CAM_VIEW_PROJECTION = "u_viewProjection";
    public static final String UNIFORM_ORTHO_PROJECTION = "u_orthoProjection";
    public static final String UNIFORM_ORTHO_SCREEN_SIZE = "u_screenSize";
    public static final String UNIFORM_BONE_PALETTE = "u_bonePalette";
    public static final String ITEM_ROTATION_DATA_UBO = "ItemRotationData";
    public static final String BLOCK_ORIENTATION_MAP_DATA_UBO = "BlockOrientationMapData";
    public static final String SLICE_DATA_UBO = "SliceData";
    public static final String UNIFORM_BORDER = "u_border";
    public static final String UNIFORM_CHUNK_SIZE = "u_chunkSize";
    public static final String UNIFORM_CURRENT_DAY = "u_currentDay";
    public static final String UNIFORM_CURRENT_HOUR = "u_currentHour";
    public static final String UNIFORM_CURRENT_MINUTE = "u_currentMinute";
    public static final String UNIFORM_DELTA_TIME = "u_deltaTime";
    public static final String UNIFORM_DEST_RECT = "u_destRect";
    public static final String UNIFORM_DISTANCE_FROM_CENTER = "u_distanceFromCenter";
    public static final String UNIFORM_FACE_ORIENTATIONS = "u_faceOrientations";
    public static final String UNIFORM_FONT_ATLAS = "u_fontAtlas";
    public static final String UNIFORM_GRID_POSITION = "u_gridPosition";
    public static final String UNIFORM_MACRO_COVERAGE = "u_macroCoverage";
    public static final String UNIFORM_NATURAL_NOISE_LATTICE = "u_naturalNoiseLattice";
    public static final String UNIFORM_NEAR_TESSELLATION_RADIUS = "u_nearTessellationRadius";
    public static final String UNIFORM_PLAYER_CHUNK_X = "u_playerChunkX";
    public static final String UNIFORM_PLAYER_CHUNK_Z = "u_playerChunkZ";
    public static final String UNIFORM_PLAYER_POSITION = "u_playerPosition";
    public static final String UNIFORM_RANDOM_NOISE_FROM_DAY = "u_randomNoiseFromDay";
    public static final String UNIFORM_RAW_TIME_OF_DAY = "u_rawTimeOfDay";
    public static final String UNIFORM_RENDER_DISTANCE = "u_renderDistance";
    public static final String UNIFORM_RESOLUTION = "u_resolution";
    public static final String UNIFORM_ROTATIONS = "u_rotations";
    public static final String UNIFORM_SOURCE = "u_source";
    public static final String UNIFORM_SPRITE = "u_sprite";
    public static final String UNIFORM_TEX_SIZE = "u_texSize";
    public static final String UNIFORM_TIME = "u_time";
    public static final String UNIFORM_TIME_OF_DAY = "u_timeOfDay";
    public static final String UNIFORM_TIME_OF_YEAR = "u_timeOfYear";
    public static final String UNIFORM_TRANSFORM = "u_transform";
    public static final String UNIFORM_WEATHER_CELLS = "u_weatherCells";
    public static final String UNIFORM_WEATHER_DOME = "u_weatherDome";
    public static final String UNIFORM_WEATHER_LAYER_COLOR = "u_weatherLayerColor";
    public static final String UNIFORM_WEATHER_LAYER_COUNT = "u_weatherLayerCount";
    public static final String UNIFORM_WEATHER_LAYER_NOISE = "u_weatherLayerNoise";
    public static final String UNIFORM_WEATHER_LAYER_SHAPE = "u_weatherLayerShape";
    public static final String UNIFORM_WEATHER_LAYER_SURFACE = "u_weatherLayerSurface";
    public static final String UNIFORM_WEATHER_MAP_ORIGIN = "u_weatherMapOrigin";

    // Post Processing
    public static final String UNIFORM_ANTI_ALIASING = "u_antiAliasing";
    public static final String UNIFORM_BLOOM_ENABLED = "u_bloomEnabled";
    public static final String UNIFORM_BLOOM_INTENSITY = "u_bloomIntensity";
    public static final String UNIFORM_BRIGHTNESS = "u_brightness";
    public static final String UNIFORM_CHROMATIC_ABERRATION = "u_chromaticAberration";
    public static final String UNIFORM_CONTRAST = "u_contrast";
    public static final String UNIFORM_DOF_BLUR = "u_dofBlur";
    public static final String UNIFORM_DOF_ENABLED = "u_dofEnabled";
    public static final String UNIFORM_DOF_STRENGTH = "u_dofStrength";
    public static final String UNIFORM_FILM_GRAIN = "u_filmGrain";
    public static final String UNIFORM_OUTLINE_ENABLED = "u_outlineEnabled";
    public static final String UNIFORM_OUTLINE_STRENGTH = "u_outlineStrength";
    public static final String UNIFORM_OUTLINE_THICKNESS = "u_outlineThickness";
    public static final String UNIFORM_SATURATION = "u_saturation";
    public static final String UNIFORM_VIGNETTE = "u_vignette";

    // Lighting
    public static final float CELESTIAL_POLE_MAX_ELEVATION_DEGREES = 90f;
    public static final float MOON_BRIGHTNESS_BASE = 0.7f;
    public static final float MOON_BRIGHTNESS_LUNAR_SCALE = 0.3f;
    public static final float MOON_COLOR_B = 1.0f;
    public static final float MOON_COLOR_G = 0.85f;
    public static final float MOON_COLOR_R = 0.75f;
    public static final float MOON_HORIZON_CUTOFF = 0.85f;
    public static final float MOON_MAX_INCLINATION_DEGREES = 90f;
    public static final float MOON_MAX_INTENSITY = 0.25f;
    public static final float MOON_PHASE_MAX = 0.95f;
    public static final float MOON_PHASE_MIN = 0.05f;
    public static final float SUN_HORIZON_CUTOFF = 0.85f;
    public static final String UNIFORM_MOON_COLOR = "u_moonColor";
    public static final String UNIFORM_MOON_DIRECTION = "u_moonDirection";
    public static final String UNIFORM_MOON_INTENSITY = "u_moonIntensity";
    public static final String UNIFORM_MOON_SCALE = "u_moonScale";
    public static final String UNIFORM_STAR_ROTATION = "u_starRotation";
    public static final String UNIFORM_SUN_COLOR = "u_sunColor";
    public static final String UNIFORM_SUN_DIRECTION = "u_sunDirection";
    public static final String UNIFORM_SUN_INTENSITY = "u_sunIntensity";
    public static final String UNIFORM_SUN_SCALE = "u_sunScale";

    // Block & World
    public static final String AIR_BLOCK_NAME = "TerraArcanaBlocks/Air";
    public static final int BLOCK_MAP_COLOR_UNDEFINED = -1;
    public static final int BLOCK_TEXTURE_UNDEFINED = -1;
    public static final float BLOCK_VISCOSITY_UNDEFINED = -1.0f;
    public static final int CHUNK_POOL_MAX_OVERFLOW = 32;
    public static final String CHUNK_VAO = "util/vao/ChunkVAO";
    public static final int COMPLEX_TICK_INTERVAL_FRAMES = 60;
    public static final int COMPLEX_TICK_PHASE_FRAMES = 30;
    public static final float DEFAULT_AXIAL_TILT_DEGREES = 23.5f;
    public static final short DEFAULT_BLOCK_ORIENTATION = (short) (DEFAULT_BLOCK_DIRECTION * 4);
    public static final String DEFAULT_CALENDAR_NAME = "standard/Default";
    public static final double STAR_TEMPERATURE_EXPONENT = 0.25;
    public static final float DEFAULT_GRAVITY_MULTIPLIER = 1.0f;
    public static final float DEFAULT_GRAVITY_X = 0.0f;
    public static final float DEFAULT_GRAVITY_Y = -1.0f;
    public static final float DEFAULT_GRAVITY_Z = 0.0f;
    public static final float DEFAULT_PLANETARY_OFFSET = 0.0f;
    public static final float DEFAULT_WORLD_ROTATION_SPEED = 1.0f;
    public static final int ENCODED_FACE_NATURAL_FULL_OFFSET = 24;
    public static final int ENCODED_FACE_SPIN_COUNT = 4;
    public static final int FULL_TICK_INTERVAL_FRAMES = 60;
    public static final int FULL_TICK_PHASE_FRAMES = 0;
    public static final int GRID_SLOTS_SCAN_PER_FRAME = 32;
    public static final int LIQUID_TICK_INTERVAL_FRAMES = 6;
    public static final int LIQUID_TICK_PHASE_FRAMES = 3;
    public static final int MAX_CHUNK_GPU_UPLOADS_PER_FRAME = 16;
    public static final int MAX_CHUNK_STREAM_PER_BATCH = 32;
    public static final int MAX_CHUNK_STREAM_PER_FRAME = 128;
    public static final int MAX_CHUNK_STREAM_PER_QUEUE = 1024;
    public static final int MAX_CHUNK_UNLOADS_PER_FRAME = 64;
    public static final int MAX_MEGA_GPU_UPLOADS_PER_FRAME = 4;
    public static final int MEGA_ASSESS_PER_FRAME = GRID_SLOTS_SCAN_PER_FRAME / MEGA_CHUNK_SIZE;
    public static final int MEGA_POOL_MAX_OVERFLOW = 8;
    public static final int PARTIAL_TICK_INTERVAL_FRAMES = 60;
    public static final int PARTIAL_TICK_PHASE_FRAMES = 15;
    public static final String STARTING_WORLD = "TerraArcana";
    public static final int WORLD_RENDER_ENTRY_POOL_MAX_PER_MATERIAL = 256;

    // Macro Terrain
    public static final int MACRO_ADMISSIONS_PER_FRAME = 16;
    public static final float MACRO_ANCHOR_CENTER_CHUNKS = 0.5f;
    public static final int MACRO_ASSESS_PER_FRAME = 128;
    public static final float MACRO_CELL_ANGLE_RADIANS = 0.03f;
    public static final int MACRO_CELLS_PER_SIDE_MAX = 32;
    public static final int MACRO_CELLS_PER_SIDE_MIN = 1;
    public static final int MACRO_COVERAGE_BITS_PER_WORD = Integer.SIZE;
    public static final String MACRO_COVERAGE_UBO = "MacroCoverageData";
    public static final int MACRO_COVERAGE_WORD_COUNT = MACRO_CHUNK_SIZE * MACRO_CHUNK_SIZE
            / MACRO_COVERAGE_BITS_PER_WORD;
    public static final int MACRO_COVERAGE_VECTOR_COUNT = MACRO_COVERAGE_WORD_COUNT / VECTOR4_COMPONENT_COUNT;
    public static final String MACRO_MATERIAL = "surface/MacroTerrainMaterial";
    public static final int MACRO_POOL_MAX_OVERFLOW = 16;
    public static final float MACRO_RENDER_DISTANCE_BLOCKS = 8192f;
    public static final int MACRO_TILE_SIZE_BLOCKS = MACRO_CHUNK_SIZE * CHUNK_SIZE;
    public static final int MACRO_RING_REACH_TILES = (int) Math.ceil(MACRO_RENDER_DISTANCE_BLOCKS
            / MACRO_TILE_SIZE_BLOCKS) + 1;
    public static final float MACRO_SKIRT_DEPTH_CELLS = 1f;
    public static final int MACRO_STREAMING_MARGIN_CHUNKS = MACRO_CHUNK_SIZE;
    public static final String MACRO_STREAMING_THREAD_NAME = "MacroStreaming";
    public static final float MACRO_SURFACE_OFFSET_BLOCKS = BLOCK_SIZE;
    public static final String MACRO_VAO = "util/vao/MacroVAO";
    public static final int MACRO_VERTEX_FLOAT_COUNT = 5;
    public static final int MAX_MACRO_GPU_UPLOADS_PER_FRAME = 16;

    // Macro Water
    public static final int MACRO_WATER_COVERAGE_SIZE = RENDER_DISTANCE_MAX;
    public static final String MACRO_WATER_MATERIAL = "liquid/MacroWaterMaterial";
    public static final int MACRO_WATER_MASK_TEXELS_PER_TILE = 16;
    public static final int MACRO_WATER_MASK_TILES = MACRO_RING_REACH_TILES * 2 + 1;
    public static final int MACRO_WATER_MASK_SIZE = MACRO_WATER_MASK_TILES * MACRO_WATER_MASK_TEXELS_PER_TILE;
    public static final int MACRO_WATER_MASK_SPAN_CHUNKS = MACRO_WATER_MASK_TILES * MACRO_CHUNK_SIZE;
    public static final String MACRO_WATER_VAO = "util/vao/MacroWaterVAO";
    public static final String UNIFORM_MACRO_WATER_ANCHOR = "u_macroWaterAnchor";
    public static final String UNIFORM_MACRO_WATER_COVERAGE = "u_macroWaterCoverage";
    public static final String UNIFORM_MACRO_WATER_MASK = "u_macroWaterMask";

    // World Map — a tile pyramid whose finest tile spans one macro tile at one block per texel
    public static final int MAP_CHUNKS_PER_FRAME = 24;
    public static final long MAP_CHUNK_VERSION_NONE = -1L;
    public static final int MAP_COLOR_UNKNOWN = 0x202020;
    public static final int MAP_COLOR_WATER_DEEP = 0x05295C;
    public static final int MAP_COLOR_WATER_SHALLOW = 0x38CCC2;
    public static final float MAP_HILLSHADE_STRENGTH = 0.35f;
    public static final float MAP_LIGHT_X = -0.70710677f;
    public static final float MAP_LIGHT_Z = -0.70710677f;
    public static final int MAP_MAX_LEVELS = 24;
    public static final float MAP_RELIEF_PER_BLOCK = 0.6f;
    public static final float MAP_SLOPE_END = 0.75f;
    public static final float MAP_SLOPE_START = 0.35f;
    public static final String MAP_THREAD_NAME = "WorldMap";
    public static final int MAP_TILE_CACHE_MAX = 192;
    public static final int MAP_TILE_KEY_LEVEL_SHIFT = 48;
    public static final long MAP_TILE_KEY_AXIS_MASK = 0xFFFFFFL;
    public static final int MAP_TILE_KEY_X_SHIFT = 24;
    public static final int MAP_TILE_SAMPLE_SPACING_MIN_BLOCKS = 2;
    public static final int MAP_TILE_SAMPLES_PER_SIDE = 128;
    public static final int MAP_TILE_TEXELS = MACRO_TILE_SIZE_BLOCKS;
    public static final int MAP_TILE_UPLOADS_PER_FRAME = 4;
    public static final float MAP_WATER_CLEAR_BLOCKS = 4f;
    public static final float MAP_WATER_DEEP_BLOCKS = 24f;
    public static final float MAP_WATER_FLOOR_SHARE = 0.45f;

    // World Map — the shared day and night and weather overlays any view can show, and how far a world image edit
    // reaches past the pixels it changed, through the biome field's blend and warp
    public static final int MAP_EDIT_MARGIN_PIXELS = 2;
    public static final float MAP_NIGHT_ELEVATION_DARK = -0.2f;
    public static final float MAP_NIGHT_ELEVATION_LIGHT = 0.1f;
    public static final int MAP_OVERLAY_DAYLIGHT_COLUMNS = 256;
    public static final int MAP_OVERLAY_DAYLIGHT_ROWS = 64;
    public static final int MAP_OVERLAY_WEATHER_SAMPLES_PER_FRAME = 512;
    public static final int MAP_OVERLAY_WEATHER_TEXELS = 256;

    // World — live edits restream what they reach of every loaded grid once edits have been quiet this long, and an
    // edited world pixel reaches terrain this far past itself, through the biome field and the structures it places
    public static final float WORLD_LIVE_REBUILD_DELAY_SECONDS = 0.4f;
    public static final int WORLD_LIVE_REBUILD_MARGIN_CHUNKS = MAP_EDIT_MARGIN_PIXELS * CHUNKS_PER_PIXEL
            + EngineSetting.STRUCTURE_MAX_EXTENT_BLOCKS / CHUNK_SIZE;

    // Sub-Block
    public static final int CHUNK_VERTEX_FLOAT_COUNT = 15;
    public static final int GEOMETRY_EDGE_BITS_PER_CELL = 4;
    public static final int GEOMETRY_EDGE_CELLS_PER_WORD = 6;
    public static final int GEOMETRY_MAX_MERGE_EXTENT = 10;
    public static final int GEOMETRY_META_ENCODED_FACE_BITS = 6;
    public static final int GEOMETRY_META_FACE_BITS = 3;
    public static final int GEOMETRY_META_SIZE_BITS = 5;
    public static final int SUB_BLOCK_MASK_EMPTY = 0;
    public static final int SUB_BLOCK_OCTANT_COUNT = SUB_BLOCK_DIVISIONS * SUB_BLOCK_DIVISIONS
            * SUB_BLOCK_DIVISIONS;
    public static final int SUB_BLOCK_MASK_FULL = (1 << SUB_BLOCK_OCTANT_COUNT) - 1;
    public static final float SUB_BLOCK_SIZE = BLOCK_SIZE / SUB_BLOCK_DIVISIONS;
    public static final float SUB_BLOCK_SMOOTHING_THRESHOLD_BLOCKS = 0.5f;

    // Liquid & Swimming
    public static final int LIQUID_BASIN_MIN_DEPTH = 8;
    public static final int LIQUID_BASIN_SCAN_LIMIT = 1024;
    public static final int LIQUID_BASIN_TRIGGER_LEVEL = 32;
    public static final float LIQUID_EVAPORATION_CHANCE = 0.125f;
    public static final int LIQUID_EVAPORATION_LEVEL = 3;
    public static final int LIQUID_EVAPORATION_RATE = 1;
    public static final float LIQUID_FLOW_INTERVAL_MAX_SECONDS = 20.0f;
    public static final float LIQUID_FLOW_INTERVAL_MIN_SECONDS = 0.1f;
    public static final short LIQUID_LEVEL_BLOCKED = -1;
    public static final short LIQUID_LEVEL_EMPTY = 0;
    public static final short LIQUID_LEVEL_MAX = 64;
    public static final float LIQUID_NO_SURFACE = Float.NaN;
    public static final int LIQUID_PERMANENCE_THRESHOLD = 512;
    public static final int LIQUID_SCAN_SUBCHUNK_LIMIT = 32;
    public static final int LIQUID_SPREAD_LOSS = 1;
    public static final int LIQUID_SPREAD_MIN_DIFFERENCE = 2;
    public static final int LIQUID_SPREAD_RATE = 4;
    public static final float LIQUID_VISCOSITY_TO_FLOW_SECONDS = 2.0f;
    public static final int LIQUID_WET_MIN_LEVEL = 4;
    public static final float SWIM_CLIMB_OUT_HEIGHT_FRACTION = 0.6f;
    public static final float SWIM_CLIMB_OUT_LIFT = 0.05f;
    public static final float SWIM_CLIMB_OUT_MAX_VISCOSITY = 1.0f;
    public static final int SWIM_CLIMB_OUT_SCAN_DEPTH = 4;
    public static final float SWIM_DEEP_THRESHOLD = 0.1f;
    public static final float SWIM_DEPTH_FRACTION = 0.75f;
    public static final float SWIM_DIVE_PITCH_THRESHOLD = 0.5f;
    public static final float SWIM_DIVE_SPEED = 2.0f;
    public static final float SWIM_HEAD_CLEARANCE = 0.12f;
    public static final float SWIM_LEAP_REST_TOLERANCE = 0.1f;
    public static final float SWIM_MIN_SPEED_MULTIPLIER = 0.15f;
    public static final float SWIM_SINK_SPEED = 0.15f;
    public static final float SWIM_SURFACE_HOLD_DEPTH = 0.6f;
    public static final float SWIM_TREAD_RESPONSIVENESS = 6.0f;
    public static final float SWIM_TREAD_SPEED = 2.0f;
    public static final float SWIM_UP_SPEED = 2.4f;
    public static final float SWIM_VERTICAL_RESPONSIVENESS = 4.0f;
    public static final float SWIM_VERTICAL_STATE_SPEED = 0.8f;
    public static final float SWIM_VISCOSITY_DRAG_SCALE = 0.12f;
    public static final float SWIM_VISCOSITY_REFERENCE = 1.0f;
    public static final float SWIM_WAVE_CURRENT_SCALE = 0.75f;
    public static final float WADE_DEEP_SPEED_MULTIPLIER = 0.55f;
    public static final float WADE_RUN_SPEED_MULTIPLIER = 0.4f;
    public static final float WADE_SHALLOW_DEPTH_FACTOR = 0.4f;
    public static final float WATER_ENTRY_LEAP_MAX_FALL_SPEED = 5.0f;
    public static final float WATER_ENTRY_LEAP_MIN_DEPTH_FACTOR = 0.3f;
    public static final float WATER_ENTRY_LEAP_MULTIPLIER = 0.6f;
    public static final float WATER_JUMP_DEEP_MULTIPLIER = 0.4f;
    public static final float WATER_JUMP_MIN_HEIGHT = 0.35f;

    // Biome
    public static final float BIOME_BLEND_BAND_PIXELS = 0.9f;
    public static final float BIOME_BORDER_WARP_DETAIL_FREQUENCY = 2.7f;
    public static final float BIOME_BORDER_WARP_DETAIL_STRENGTH_PIXELS = 0.11f;
    public static final float BIOME_BORDER_WARP_FREQUENCY = 0.5f;
    public static final long BIOME_BORDER_WARP_SEED = 0x8B3C9F1E2A47D5B1L;
    public static final float BIOME_BORDER_WARP_STRENGTH_PIXELS = 0.4f;
    public static final int BIOME_FIELD_MAX_CONTRIBUTORS = 12;
    public static final int BIOME_MAP_COLOR_UNDEFINED = -1;
    public static final int BIOME_MAP_SAMPLE_COUNT = 4;
    public static final long BIOME_MATERIAL_DITHER_SEED = 0x7D2B5E0C93A6F418L;
    public static final float BIOME_OCEAN_FLOOD_THRESHOLD = 0.5f;
    public static final long BIOME_PROBABLE_CORE_SEED = 0x6B19E4D03C7A5F28L;
    public static final int BIOME_PROBABLE_DEFAULT_MAX_ARMS = 3;
    public static final float BIOME_PROBABLE_DEFAULT_MAX_CORE_SCALE = 0.7f;
    public static final float BIOME_PROBABLE_DEFAULT_MAX_SIZE_BLOCKS = 1024f;
    public static final int BIOME_PROBABLE_DEFAULT_MIN_ARMS = 1;
    public static final float BIOME_PROBABLE_DEFAULT_MIN_CORE_SCALE = 0.4f;
    public static final float BIOME_PROBABLE_DEFAULT_MIN_SIZE_BLOCKS = 192f;
    public static final float BIOME_PROBABLE_EDGE_BAND = 0.3f;
    public static final float BIOME_PROBABLE_EDGE_DETAIL_FREQUENCY = 6.5f;
    public static final float BIOME_PROBABLE_EDGE_DETAIL_WEIGHT = 0.45f;
    public static final float BIOME_PROBABLE_EDGE_NOISE_FREQUENCY = 2.4f;
    public static final long BIOME_PROBABLE_EDGE_NOISE_SEED = 0x2E8A5C71D94B0F63L;
    public static final float BIOME_PROBABLE_EDGE_NOISE_STRENGTH = 0.25f;
    public static final float BIOME_PROBABLE_INVERTED_COVERAGE = 0.5f;
    public static final float BIOME_PROBABLE_MAX_CELL_OCCUPANCY = 0.45f;
    public static final int BIOME_PROBABLE_MAX_PATCHES_PER_CELL = 32;
    public static final float BIOME_PROBABLE_NO_PATCH_SHAPE = Float.POSITIVE_INFINITY;
    public static final long BIOME_PROBABLE_SEED = 0x4F1D2C6E9A7B31F5L;
    public static final int BIOME_PROBABLE_SHAPE_AREA_RESOLUTION = 48;
    public static final int BIOME_PROBABLE_SHAPE_AREA_SAMPLES = 64;
    public static final float BIOME_PROBABLE_SHAPE_ARM_BRANCH_CHANCE = 0.3f;
    public static final float BIOME_PROBABLE_SHAPE_ARM_BRANCH_MIN_POSITION = 0.35f;
    public static final float BIOME_PROBABLE_SHAPE_ARM_CENTER_CHANCE = 0.15f;
    public static final float BIOME_PROBABLE_SHAPE_ARM_CONTINUE_CHANCE = 0.35f;
    public static final float BIOME_PROBABLE_SHAPE_ARM_MAX_BEND = 2.0f;
    public static final float BIOME_PROBABLE_SHAPE_ARM_MAX_LENGTH = 0.4f;
    public static final float BIOME_PROBABLE_SHAPE_ARM_MAX_RADIUS = 0.22f;
    public static final float BIOME_PROBABLE_SHAPE_ARM_MAX_TAPER = 1.2f;
    public static final float BIOME_PROBABLE_SHAPE_ARM_MIN_LENGTH = 0.2f;
    public static final float BIOME_PROBABLE_SHAPE_ARM_MIN_RADIUS = 0.12f;
    public static final float BIOME_PROBABLE_SHAPE_ARM_MIN_TAPER = 0.65f;
    public static final float BIOME_PROBABLE_SHAPE_BODY_MAX_RADIUS = 0.28f;
    public static final float BIOME_PROBABLE_SHAPE_BODY_MIN_RADIUS = 0.16f;
    public static final int BIOME_PROBABLE_SHAPE_MAX_ARMS = 6;
    public static final long BIOME_PROBABLE_SHAPE_SEED = 0x9C3B7E14A5D2068FL;
    public static final float BIOME_PROBABLE_SHAPE_WARP_FREQUENCY = 1.4f;
    public static final long BIOME_PROBABLE_SHAPE_WARP_SEED = 0x58D2A97E1F04C6B3L;
    public static final float BIOME_PROBABLE_SHAPE_WARP_STRENGTH = 0.09f;
    public static final float BIOME_SHORE_BUFFER_FULL_OCEAN_SHARE = 0.12f;

    // Terrain Generation
    public static final float DEFAULT_BIOME_TERRAIN_HEIGHT_SCALE = 1.0f;
    public static final String DEFAULT_STONE_BLOCK_NAME = "TerraArcanaBlocks/Stone Block";
    public static final String DEFAULT_SUBSURFACE_BLOCK_NAME = "TerraArcanaBlocks/Dirt Block";
    public static final String DEFAULT_SURFACE_BLOCK_NAME = "TerraArcanaBlocks/Grass Block";
    public static final String DEFAULT_UNDERWATER_BLOCK_NAME = "TerraArcanaBlocks/Sand";
    public static final String DEFAULT_WATER_BLOCK_NAME = "TerraArcanaBlocks/Water";
    public static final int TERRAIN_BEACH_HEIGHT_RANGE_BLOCKS = 3;
    public static final float TERRAIN_CONTINENTALNESS_LACUNARITY = 2.0f;
    public static final int TERRAIN_CONTINENTALNESS_OCTAVES = 4;
    public static final float TERRAIN_CONTINENTALNESS_PERSISTENCE = 0.5f;
    public static final long TERRAIN_CONTINENTALNESS_SEED_SALT = 0x6C4F3A2E9D1B8F73L;
    public static final float[] TERRAIN_CONTINENTALNESS_SPLINE_HEIGHT_BLOCKS = { 40f, 60f, 100f, 145f, 160f,
        168f, 190f, 230f, 290f };
    public static final float[] TERRAIN_CONTINENTALNESS_SPLINE_X = { -1.0f, -0.55f, -0.25f, -0.05f, 0.0f, 0.10f,
        0.30f, 0.60f, 1.0f };
    public static final double TERRAIN_CONTINENTALNESS_WAVELENGTH_BLOCKS = 2400.0;
    public static final float TERRAIN_DETAIL_AMPLITUDE_BLOCKS = 3.0f;
    public static final float TERRAIN_DETAIL_LACUNARITY = 2.0f;
    public static final int TERRAIN_DETAIL_OCTAVES = 3;
    public static final float TERRAIN_DETAIL_PERSISTENCE = 0.5f;
    public static final int TERRAIN_DETAIL_SAMPLE_STRIDE_BLOCKS = 4;
    public static final long TERRAIN_DETAIL_SEED_SALT = 0x4B9F2D6E8C1A3075L;
    public static final float TERRAIN_DETAIL_WAVELENGTH_BLOCKS = 40.0f;
    public static final float TERRAIN_EROSION_LACUNARITY = 2.0f;
    public static final int TERRAIN_EROSION_OCTAVES = 3;
    public static final float TERRAIN_EROSION_PERSISTENCE = 0.5f;
    public static final long TERRAIN_EROSION_SEED_SALT = 0x2F8E4C7A19D3B650L;
    public static final float[] TERRAIN_EROSION_SPLINE_AMPLITUDE_BLOCKS = { 220f, 150f, 70f, 30f, 10f, 4f };
    public static final float[] TERRAIN_EROSION_SPLINE_X = { -1.0f, -0.6f, -0.2f, 0.2f, 0.6f, 1.0f };
    public static final double TERRAIN_EROSION_WAVELENGTH_BLOCKS = 1100.0;
    public static final int TERRAIN_MACRO_SAMPLE_STRIDE_BLOCKS = 8;
    public static final int TERRAIN_MAX_HEIGHT_BLOCKS = 900;
    public static final int TERRAIN_MIN_HEIGHT_BLOCKS = 24;
    public static final long TERRAIN_OCTAVE_HASH_SALT = 0x9E3779B97F4A7C15L;
    public static final float TERRAIN_PV_LACUNARITY = 2.0f;
    public static final int TERRAIN_PV_OCTAVES = 4;
    public static final float TERRAIN_PV_PERSISTENCE = 0.5f;
    public static final long TERRAIN_PV_SEED_SALT = 0xA37D1E9C5B2F8064L;
    public static final float[] TERRAIN_PV_SPLINE_CONTRIBUTION = { -1.0f, -0.1f, 0.3f, 0.7f, 1.0f };
    public static final float[] TERRAIN_PV_SPLINE_X = { 0.0f, 0.5f, 0.75f, 0.9f, 1.0f };
    public static final double TERRAIN_PV_WAVELENGTH_BLOCKS = 380.0;
    public static final int TERRAIN_SEA_LEVEL_BLOCKS = 160;
    public static final int TERRAIN_SURFACE_DEPTH_BLOCKS = 4;

    // Terrain Features
    public static final float DEFAULT_BIOME_CLIFF_COVERAGE = 0.5f;
    public static final float DEFAULT_BIOME_CLIFF_STEP_BLOCKS = 10f;
    public static final float DEFAULT_BIOME_CLIFF_STRENGTH = 0.85f;
    public static final float DEFAULT_BIOME_COAST_COVERAGE = 0.6f;
    public static final float DEFAULT_BIOME_COAST_OVERHANG_BLOCKS = 2f;
    public static final float DEFAULT_BIOME_COAST_SEA_CAVES = 0.4f;
    public static final float DEFAULT_BIOME_RIDGE_WAVELENGTH_BLOCKS = 220f;
    public static final float DEFAULT_BIOME_ROCK_SLOPE = 1.3f;
    public static final float TERRAIN_BEACH_MAX_SLOPE = 0.55f;
    public static final float TERRAIN_CLIFF_MASK_EDGE = 0.2f;
    public static final long TERRAIN_CLIFF_MASK_SEED_SALT = 0x5D3A9E71C2B84F06L;
    public static final double TERRAIN_CLIFF_MASK_WAVELENGTH_BLOCKS = 340.0;
    public static final float TERRAIN_CLIFF_RISER_FRACTION = 0.2f;
    public static final float TERRAIN_COAST_CLIFF_MIN_HEIGHT_BLOCKS = 1f;
    public static final float TERRAIN_COAST_CLIFF_RISE_BLOCKS = 1f;
    public static final float TERRAIN_COAST_MASK_EDGE = 0.2f;
    public static final long TERRAIN_COAST_MASK_SEED_SALT = 0x31C7E5A94D0B2F68L;
    public static final double TERRAIN_COAST_MASK_WAVELENGTH_BLOCKS = 280.0;
    public static final float TERRAIN_COAST_MIN_SLOPE = 0.02f;
    public static final int TERRAIN_COAST_NOTCH_SPLASH_BLOCKS = 1;
    public static final float TERRAIN_COAST_PLATEAU_RELIEF = 0.6f;
    public static final int TERRAIN_COAST_TOE_CLEARANCE_BLOCKS = 3;
    public static final float TERRAIN_COAST_TOE_DROP_BLOCKS = 1f;
    public static final float TERRAIN_COAST_ZONE_OCEAN_WEIGHT = 0.05f;
    public static final int TERRAIN_FEATURE_LAYERS_MAX = 4;
    public static final float TERRAIN_FEATURE_MASK_LACUNARITY = 2.0f;
    public static final int TERRAIN_FEATURE_MASK_OCTAVES = 2;
    public static final float TERRAIN_FEATURE_MASK_PERSISTENCE = 0.5f;
    public static final float TERRAIN_FEATURE_MASK_SPAN = 0.6f;
    public static final float TERRAIN_RIDGE_BIAS = 0.3f;
    public static final float TERRAIN_RIDGE_GAIN = 0.55f;
    public static final float TERRAIN_RIDGE_LACUNARITY = 2.1f;
    public static final int TERRAIN_RIDGE_OCTAVES = 3;
    public static final long TERRAIN_RIDGE_SEED_SALT = 0x7E2B04D9A6C3851FL;

    // Caves
    public static final float CAVE_CAVERN_THRESHOLD_MAX = 0.8f;
    public static final float CAVE_CAVERN_THRESHOLD_MIN = 0.45f;
    public static final long CAVE_CAVERN_SEED_SALT = 0x1A6F3C8E5B92D074L;
    public static final float CAVE_CAVERN_VERTICAL_SCALE = 1.8f;
    public static final double CAVE_CAVERN_WAVELENGTH_BLOCKS = 110.0;
    public static final float CAVE_ENTRANCE_SHARE = 0.5f;
    public static final int CAVE_FLOOR_FADE_BLOCKS = 10;
    public static final int CAVE_LAKE_GUARD_DEPTH_BLOCKS = 44;
    public static final float CAVE_LAKE_GUARD_WEIGHT = 0.25f;
    public static final int CAVE_LATTICE_STEP_BLOCKS = 4;
    public static final int CAVE_ROOF_BLOCKS = 5;
    public static final float CAVE_SEA_BARRIER_WEIGHT = 0.12f;
    public static final float CAVE_SEA_FLOOD_WEIGHT = 0.2f;
    public static final int CAVE_SEA_HEADROOM_BLOCKS = 2;
    public static final float CAVE_SEA_MAX_WIDTH = 0.14f;
    public static final float CAVE_SEA_REACH_BLOCKS = 36f;
    public static final long CAVE_SEA_SEED_SALT = 0x4C8B1E7D20F5A963L;
    public static final float CAVE_SEA_VERTICAL_SCALE = 0.2f;
    public static final double CAVE_SEA_WAVELENGTH_BLOCKS = 44.0;
    public static final float CAVE_TUNNEL_MAX_RADIUS = 0.09f;
    public static final float CAVE_TUNNEL_MIN_RADIUS = 0.045f;
    public static final long CAVE_TUNNEL_SEED_SALT_A = 0x63D1A0F4B7E2C958L;
    public static final long CAVE_TUNNEL_SEED_SALT_B = 0x2B97E5C10D4A3F86L;
    public static final float CAVE_TUNNEL_VERTICAL_SCALE = 1.4f;
    public static final double CAVE_TUNNEL_WAVELENGTH_BLOCKS = 80.0;
    public static final int CAVE_WATER_ROOF_BLOCKS = 6;
    public static final float DEFAULT_BIOME_CAVE_CAVERNS = 0.15f;
    public static final boolean DEFAULT_BIOME_CAVE_ENTRANCES = true;
    public static final int DEFAULT_BIOME_CAVE_MAX_DEPTH_BLOCKS = 80;
    public static final int DEFAULT_BIOME_CAVE_MIN_HEIGHT_BLOCKS = 40;
    public static final float DEFAULT_BIOME_CAVE_TUNNELS = 0.35f;

    // Veins
    public static final float DEFAULT_BIOME_VEIN_ABUNDANCE = 0.3f;
    public static final int DEFAULT_BIOME_VEIN_MAX_DEPTH_BLOCKS = 48;
    public static final int DEFAULT_BIOME_VEIN_MAX_HEIGHT_BLOCKS = TERRAIN_MAX_HEIGHT_BLOCKS;
    public static final int DEFAULT_BIOME_VEIN_MIN_HEIGHT_BLOCKS = 0;
    public static final float DEFAULT_BIOME_VEIN_THICKNESS_BLOCKS = 2f;
    public static final float TERRAIN_VEIN_GATE_RANGE = 0.8f;
    public static final long TERRAIN_VEIN_GATE_SEED_SALT = 0x0F5E9A27C3B81D64L;
    public static final float TERRAIN_VEIN_GATE_THRESHOLD = 0.6f;
    public static final double TERRAIN_VEIN_GATE_WAVELENGTH_BLOCKS = 96.0;
    public static final int TERRAIN_VEIN_PALETTE_MAX = 6;
    public static final long TERRAIN_VEIN_SEED_SALT = 0x95A3D7E04C1B6F28L;
    public static final float TERRAIN_VEIN_SHEET_GRADIENT = 0.8f;
    public static final double TERRAIN_VEIN_WAVELENGTH_BLOCKS = 36.0;

    // Biome Trees — how a biome spreads each kind of tree it links
    public static final int BIOME_MAX_CLUSTER_TREES = 24;
    public static final int BIOME_MAX_TREE_CLUSTER_RADIUS_BLOCKS = 48;
    public static final int BIOME_MAX_TREE_KINDS = 8;
    public static final int BIOME_MIN_TREE_SPACING_BLOCKS = 2;
    public static final float DEFAULT_BIOME_TREE_CHANCE = 0.5f;
    public static final float DEFAULT_BIOME_TREE_CLUSTER_RADIUS_BLOCKS = 8f;
    public static final String DEFAULT_BIOME_TREE_DISTRIBUTION = "scattered";
    public static final int DEFAULT_BIOME_TREE_MAX_CLUSTER_TREES = 6;
    public static final int DEFAULT_BIOME_TREE_MIN_CLUSTER_TREES = 3;
    public static final float DEFAULT_BIOME_TREE_PATCH_COVERAGE = 0.6f;
    public static final float DEFAULT_BIOME_TREE_PATCH_WAVELENGTH_BLOCKS = 96f;
    public static final int DEFAULT_BIOME_TREE_SPACING_BLOCKS = 12;

    // Lakes
    public static final float LAKE_BED_DEPTH_PER_WEIGHT = 24f;
    public static final int LAKE_LEVEL_UNDEFINED = Integer.MIN_VALUE;
    public static final int LAKE_RIM_BLOCKS = 1;
    public static final float LAKE_RIM_FULL_WEIGHT = 0.3f;
    public static final float LAKE_SHORE_WEIGHT = 0.5f;

    // Ocean & Tide
    public static final String OCEAN_DATA_UBO = "OceanData";
    public static final int OCEAN_SPILL_CHUNKS = 2;
    public static final float OCEAN_REACH_THRESHOLD = BIOME_OCEAN_FLOOD_THRESHOLD
            - OCEAN_SPILL_CHUNKS / (BIOME_BLEND_BAND_PIXELS * CHUNKS_PER_PIXEL);
    public static final float OCEAN_TIDE_AMPLITUDE_BLOCKS = 5.0f;
    public static final int OCEAN_TIDE_CHUNKS_PER_TICK = 24;
    public static final int OCEAN_TIDE_LEVEL_STEP = 4;
    public static final float OCEAN_TIDE_NEAP_AMPLITUDE_RATIO = 0.6f;
    public static final double OCEAN_TIDE_PEAK_TIME_OF_DAY = 0.0;
    public static final int OCEAN_TIDE_RANGE_CHUNKS = 16;
    public static final double OCEAN_TIDE_SPRING_NEAP_PERIOD_DAYS = 14.0;
    public static final int OCEAN_TIDE_UNAPPLIED = Integer.MIN_VALUE;

    // Ocean Turbulence
    public static final float OCEAN_TURBULENCE_CELL_WEIGHT = 4.0f;
    public static final float OCEAN_TURBULENCE_GUST_FREQUENCY = 0.05f;
    public static final float OCEAN_TURBULENCE_GUST_VARIANCE = 0.35f;
    public static final float OCEAN_TURBULENCE_MAX_STRENGTH = 4.0f;
    public static final float OCEAN_TURBULENCE_PRECIPITATION_WEIGHT = 0.8f;
    public static final long OCEAN_TURBULENCE_SEED = 0x3C6EF372FE94F82BL;
    public static final float OCEAN_TURBULENCE_STRENGTH_EPSILON = 0.01f;
    public static final int OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR = 4;
    public static final int OCEAN_TURBULENCE_UBO_MAX_ENTRIES = 16;
    public static final float OCEAN_TURBULENCE_WEATHER_CELL_RADIUS_RATIO = 1.0f;
    public static final float OCEAN_TURBULENCE_WEATHER_FADE_START_RATIO = 0.5f;
    public static final float OCEAN_TURBULENCE_WEATHER_RANGE_CELLS = 2.0f;
    public static final float OCEAN_TURBULENCE_WIND_WEIGHT = 1.0f;

    // Ocean Exposure
    public static final int OCEAN_EXPOSURE_BLUR_RADIUS_CELLS = 2;
    public static final int OCEAN_EXPOSURE_CACHE_MAX_CELLS = 16384;
    public static final int OCEAN_EXPOSURE_CELL_BLOCKS = 64;
    public static final float OCEAN_EXPOSURE_FULL_FRACTION = 0.8f;
    public static final int OCEAN_EXPOSURE_GRID_SIZE = 32;
    public static final int OCEAN_EXPOSURE_PROBE_CELLS_PER_FRAME = 24;
    public static final int OCEAN_EXPOSURE_PROBES_PER_AXIS = 2;
    public static final float OCEAN_EXPOSURE_RESPONSE_PER_SECOND = 0.5f;
    public static final float OCEAN_EXPOSURE_START_FRACTION = 0.15f;
    public static final byte OCEAN_EXPOSURE_UNKNOWN = -1;
    public static final int OCEAN_EXPOSURE_VALUES_PER_VECTOR = 4;

    // Ocean Sea State
    public static final double OCEAN_NOISE_GRADIENT_SCALE = 2.0 / 4294967295.0;
    public static final int OCEAN_NOISE_OCTAVE_SEED_STEP = 1013;
    public static final int OCEAN_NOISE_PCG_INCREMENT = 1013904223;
    public static final int OCEAN_NOISE_PCG_MULTIPLIER = 1664525;
    public static final int OCEAN_NOISE_PCG_SEED_MULTIPLIER = 747796405;
    public static final int OCEAN_NOISE_PCG_SHIFT = 16;
    public static final long OCEAN_NOISE_UINT_MASK = 0xFFFFFFFFL;
    public static final float OCEAN_SEA_NOISE_CELL_BLOCKS = 192.0f;
    public static final float OCEAN_SEA_NOISE_CONTRAST = 1.6f;
    public static final float OCEAN_SEA_NOISE_DETAIL_WEIGHT = 0.35f;
    public static final float OCEAN_SEA_NOISE_DRIFT_BLOCKS_PER_SECOND = 1.5f;
    public static final float OCEAN_SEA_NOISE_MAX = 1.45f;
    public static final float OCEAN_SEA_NOISE_MIN = 0.4f;
    public static final int OCEAN_SEA_NOISE_SEED = 7919;
    public static final float OCEAN_SEA_STATE_CALM = 0.35f;
    public static final float OCEAN_WHITECAP_CREST_START = 0.55f;
    public static final float OCEAN_WHITECAP_SEA_STATE_FULL = 3.6f;
    public static final float OCEAN_WHITECAP_SEA_STATE_START = 1.8f;

    // Ocean Waves
    public static final float[] OCEAN_WAVE_AMPLITUDE_RATIOS = { 1.0f, 0.6f, 1.0f, 0.65f, 0.4f, 0.25f };
    public static final float OCEAN_WAVE_AMPLITUDE_EPSILON = 0.001f;
    public static final float[] OCEAN_WAVE_ANGLES_DEGREES = { 0.0f, 18.0f, -30.0f, 25.0f, 55.0f, -65.0f };
    public static final float OCEAN_WAVE_CHOP_AMPLITUDE_PER_SEA_STATE = 0.35f;
    public static final float OCEAN_WAVE_CHOP_MAX_AMPLITUDE_BLOCKS = 1.8f;
    public static final int OCEAN_WAVE_COUNT = 6;
    public static final int OCEAN_WAVE_SHAPE_MEAN_TERMS = 12;
    public static final float OCEAN_WAVE_SHARPNESS = 1.5f;
    public static final float OCEAN_WAVE_SPEED_SCALE = 0.8f;
    public static final float OCEAN_WAVE_SWELL_AMPLITUDE_PER_SEA_STATE = 2.6f;
    public static final int OCEAN_WAVE_SWELL_COUNT = 2;
    public static final float OCEAN_WAVE_SWELL_MAX_AMPLITUDE_BLOCKS = 9.0f;
    public static final float OCEAN_WAVE_SWELL_SEA_STATE_START = 1.2f;
    public static final double OCEAN_WAVE_TIME_WRAP_SECONDS = 3600.0;
    public static final float[] OCEAN_WAVE_WAVELENGTHS_BLOCKS = { 150.0f, 110.0f, 34.0f, 22.0f, 14.0f, 9.0f };

    // Ocean Hulls
    public static final int OCEAN_HULL_HEADER_VECTORS = 4;
    public static final int OCEAN_HULL_MAX_ENTRIES = 4;
    public static final int OCEAN_HULL_STATIONS = 16;
    public static final int OCEAN_HULL_VECTORS_PER_ENTRY = OCEAN_HULL_HEADER_VECTORS
            + OCEAN_HULL_STATIONS / VECTOR4_COMPONENT_COUNT;

    // Ocean Tessellation
    public static final int OCEAN_TESSELLATION_FADE_CHUNKS = 2;
    public static final int OCEAN_TESSELLATION_FAR_RADIUS_CHUNKS = 20;
    public static final int OCEAN_TESSELLATION_MID_RADIUS_CHUNKS = 10;
    public static final int OCEAN_TESSELLATION_NEAR_RADIUS_CHUNKS = 4;

    // Ocean Uniforms
    public static final String UNIFORM_OCEAN_CAMERA = "u_oceanCamera";
    public static final String UNIFORM_OCEAN_EXPOSURE = "u_oceanExposure";
    public static final String UNIFORM_OCEAN_EXPOSURE_GRID = "u_oceanExposureGrid";
    public static final String UNIFORM_OCEAN_HULL_COUNT = "u_oceanHullCount";
    public static final String UNIFORM_OCEAN_HULLS = "u_oceanHulls";
    public static final String UNIFORM_OCEAN_NOISE = "u_oceanNoise";
    public static final String UNIFORM_OCEAN_NOISE_PERIOD = "u_oceanNoisePeriod";
    public static final String UNIFORM_OCEAN_NOISE_SHAPE = "u_oceanNoiseShape";
    public static final String UNIFORM_OCEAN_SEA_STATE = "u_oceanSeaState";
    public static final String UNIFORM_OCEAN_SURFACE = "u_oceanSurface";
    public static final String UNIFORM_OCEAN_TESSELLATION = "u_oceanTessellation";
    public static final String UNIFORM_OCEAN_TURBULENCE_CELLS = "u_oceanTurbulenceCells";
    public static final String UNIFORM_OCEAN_TURBULENCE_COUNT = "u_oceanTurbulenceCount";
    public static final String UNIFORM_OCEAN_TURBULENCE_STRENGTHS = "u_oceanTurbulenceStrengths";
    public static final String UNIFORM_OCEAN_WAVES = "u_oceanWaves";
    public static final String UNIFORM_OCEAN_WAVE_SCALE = "u_oceanWaveScale";
    public static final String UNIFORM_OCEAN_WHITECAP = "u_oceanWhitecap";

    // Water Pass
    public static final String UNIFORM_WATER_CLOUD_COLOR = "u_waterCloudColor";
    public static final String UNIFORM_WATER_CLOUD_DISTANCE = "u_waterCloudDistance";
    public static final String UNIFORM_WATER_SCENE_COLOR = "u_waterSceneColor";
    public static final String UNIFORM_WATER_SCENE_DEPTH = "u_waterSceneDepth";
    public static final String UNIFORM_WATER_SKY_COLOR = "u_waterSkyColor";

    // Structure Generation
    public static final int DEFAULT_STRUCTURE_SEPARATION_BLOCKS = 0;
    public static final int DEFAULT_STRUCTURE_Y_OFFSET_BLOCKS = 0;
    public static final long STRUCTURE_CHANCE_SALT = 0x3E9A71C45B0D82F6L;
    public static final long STRUCTURE_NAME_SEED_MULTIPLIER = 0xD6E8FEB86659FD93L;
    public static final int STRUCTURE_MAX_BLOCK_COUNT = 262144;
    public static final int STRUCTURE_MAX_EXTENT_BLOCKS = 1024;
    public static final long STRUCTURE_OFFSET_X_SALT = 0x5C1F8B2A7E94D063L;
    public static final long STRUCTURE_OFFSET_Z_SALT = 0x8D47E0B3169AC52FL;
    public static final int STRUCTURE_ORIENTATION_SPIN_COUNT = 4;
    public static final long STRUCTURE_PLACEMENT_SEED = 0x1B7F3D95C28E46A0L;
    public static final int STRUCTURE_QUARTER_TURN_COUNT = 4;
    public static final long STRUCTURE_ROTATION_SALT = 0xA60C9E4F2D7B1853L;

    // Tree Parts — the four parts every tree species is drawn with, and the texture corner each carries
    public static final int TREE_PART_BARK = 0;
    public static final int TREE_PART_WOOD = 1;
    public static final int TREE_PART_LEAF = 2;
    public static final int TREE_PART_ACCENT = 3;
    public static final int TREE_PART_COUNT = 4;
    public static final int TREE_PART_CORNER_FLOATS = 2;
    public static final int TREE_WOOD_PART_COUNT = 2;

    // Tree Mesh — how a wood vertex packs its face and edge kinds, and the slots it leaves empty
    public static final int TREE_EDGE_BITS = 2;
    public static final int TREE_HIDER_FLOATS = 5;
    public static final float TREE_LEAF_HIDE_SHARE = 0.66f;
    public static final int TREE_EDGE_CONCAVE = 2;
    public static final int TREE_EDGE_CONVEX = 1;
    public static final int TREE_EDGE_FLAT = 0;
    public static final int TREE_META_EDGE_SHIFT = 3;
    public static final int TREE_WOOD_PADDING_FLOATS = 4;

    // Tree Defaults — every optional field of a tree species file
    public static final String TREE_DEFAULT_BARK_COLOR = "#FFFFFF";
    public static final int TREE_DEFAULT_BARK_SUB_VOXELS = 2;
    public static final String TREE_DEFAULT_BARK_TEXTURE = "items/standard/OakBark";
    public static final float TREE_DEFAULT_BRANCH_ANGLE_DEGREES = 50f;
    public static final int TREE_DEFAULT_BRANCH_CHILDREN = 3;
    public static final float TREE_DEFAULT_BRANCH_CHILD_ANGLE_DEGREES = 35f;
    public static final float TREE_DEFAULT_BRANCH_CHILD_LENGTH = 0.6f;
    public static final float TREE_DEFAULT_BRANCH_GRAVITY = -0.05f;
    public static final float TREE_DEFAULT_BRANCH_LENGTH = 0.4f;
    public static final int TREE_DEFAULT_BRANCH_LEVELS = 2;
    public static final float TREE_DEFAULT_BRANCH_RADIUS_RATIO = 0.6f;
    public static final int TREE_DEFAULT_BRANCH_SEGMENTS = 4;
    public static final float TREE_DEFAULT_BRANCH_TWIST_DEGREES = 137.5f;
    public static final float TREE_DEFAULT_CROWN_START = 0.35f;
    public static final String TREE_DEFAULT_FORM = "broadleaf";
    public static final float TREE_DEFAULT_GROWTH_DAYS = 20f;
    public static final String TREE_DEFAULT_LEAF_COLOR = "#FFFFFF";
    public static final float TREE_DEFAULT_LEAF_DENSITY = 0.8f;
    public static final float TREE_DEFAULT_LEAF_RADIUS_BLOCKS = 2f;
    public static final float TREE_DEFAULT_LEAF_SQUASH = 0.75f;
    public static final String TREE_DEFAULT_LEAF_TEXTURE = "items/standard/LeavesBroad";
    public static final float TREE_DEFAULT_LOG_LENGTH_BLOCKS = 1f;
    public static final int TREE_DEFAULT_MAX_BRANCH_COUNT = 6;
    public static final float TREE_DEFAULT_MAX_HEIGHT_BLOCKS = 16f;
    public static final int TREE_DEFAULT_MIN_BRANCH_COUNT = 4;
    public static final float TREE_DEFAULT_MIN_HEIGHT_BLOCKS = 10f;
    public static final float TREE_DEFAULT_SEED_CHANCE = 0.2f;
    public static final String TREE_DEFAULT_TOOL = "Standard/Axe";
    public static final float TREE_DEFAULT_TRUNK_FLARE = 0.3f;
    public static final float TREE_DEFAULT_TRUNK_LEADER = 0.6f;
    public static final float TREE_DEFAULT_TRUNK_LEAN_DEGREES = 4f;
    public static final float TREE_DEFAULT_TRUNK_RADIUS_BLOCKS = 0.45f;
    public static final int TREE_DEFAULT_TRUNK_STEMS = 1;
    public static final float TREE_DEFAULT_TRUNK_STEM_SPREAD_BLOCKS = 0f;
    public static final float TREE_DEFAULT_TRUNK_TAPER = 0.4f;
    public static final float TREE_DEFAULT_TRUNK_WOBBLE = 0.04f;
    public static final float TREE_DEFAULT_WILD_MAX_AGE = 1f;
    public static final float TREE_DEFAULT_WILD_MIN_AGE = 0.6f;
    public static final String TREE_DEFAULT_WOOD_TEXTURE = "items/standard/OakHeartwood";

    // Tree Limits
    public static final int TREE_COLOR_HEX_DIGITS = 6;
    public static final int TREE_MAX_BRANCH_CHILDREN = 8;
    public static final int TREE_MAX_BRANCH_COUNT = 48;
    public static final int TREE_MAX_BRANCH_LEVELS = 5;
    public static final int TREE_MAX_BRANCH_SEGMENTS = 12;
    public static final float TREE_MAX_HEIGHT_BLOCKS = 240f;
    public static final int TREE_MAX_STEMS = 12;
    public static final float TREE_MAX_TRUNK_RADIUS_BLOCKS = 16f;
    public static final float TREE_NO_SPREAD = 0f;
    public static final float TREE_REACH_MARGIN_BLOCKS = 1f;
    public static final int TREE_MAX_LEAF_CLUSTERS = 4096;
    public static final int TREE_MAX_SEGMENTS = 8192;

    // Tree Growth — how a species grows its skeleton, every share of its height or length
    public static final float TREE_ALONG_LEAF_TIP_SHARE = 0.35f;
    public static final float TREE_ANGLE_JITTER_MAX = 1.15f;
    public static final float TREE_ANGLE_JITTER_MIN = 0.85f;
    public static final float TREE_BRANCH_GROW_SPAN = 0.06f;
    public static final float TREE_BRANCH_TIP_RADIUS_SHARE = 0.35f;
    public static final float TREE_BRANCH_WANDER = 0.12f;
    public static final float TREE_CHILD_FIRST_SHARE = 0.3f;
    public static final float TREE_CHILD_LENGTH_FALLOFF = 0.4f;
    public static final float TREE_CONE_TIP_SHARE = 0.08f;
    public static final float TREE_FLARE_HEIGHT_RADII = 2.5f;
    public static final float TREE_FLARE_MAX_SHARE = 0.3f;
    public static final float TREE_GROWTH_CURVE = 0.7f;
    public static final float TREE_GROWTH_CHECK_SECONDS = 1f;
    public static final int TREE_GROWTH_REBUILDS_PER_CHECK = 2;
    public static final int TREE_GROWTH_STAGES = 24;
    public static final float TREE_LEAN_MIN_SHARE = 0.5f;
    public static final float TREE_LENGTH_JITTER_MAX = 1.2f;
    public static final float TREE_LENGTH_JITTER_MIN = 0.8f;
    public static final float TREE_LEVEL_BIRTH_STEP = 0.05f;
    public static final float TREE_LIMB_AZIMUTH_JITTER = 0.26f;
    public static final float TREE_LIMB_BIRTH_MAX = 0.06f;
    public static final float TREE_LIMB_BIRTH_MIN = 0.02f;
    public static final float TREE_LIMB_JITTER_MAX = 0.9f;
    public static final float TREE_LIMB_JITTER_MIN = 0.1f;
    public static final float TREE_LIMB_LENGTH_FALLOFF = 0.35f;
    public static final float TREE_LIMB_TOP_ANGLE_SHARE = 0.55f;
    public static final float TREE_LIMB_TOP_SHARE = 0.2f;
    public static final int TREE_MAX_TRUNK_SEGMENTS = 32;
    public static final int TREE_MID_LEAF_MIN_SEGMENTS = 3;
    public static final float TREE_MID_LEAF_SHARE = 0.65f;
    public static final float TREE_MIN_LEAF_RADIUS_BLOCKS = 0.125f;
    public static final float TREE_MIN_RADIUS_BLOCKS = 1f / 32f;
    public static final float TREE_MIN_SEGMENT_BLOCKS = 0.05f;
    public static final int TREE_MIN_TRUNK_SEGMENTS = 3;
    public static final float TREE_PALM_BEND = 0.06f;
    public static final float TREE_PINNATE_FIRST_SHARE = 0.12f;
    public static final float TREE_SAPLING_HEIGHT_BLOCKS = 0.75f;
    public static final float TREE_SHARE_EPSILON = 0.001f;
    public static final float TREE_SPINDLE_BASE_SHARE = 0.55f;
    public static final float TREE_STEM_MIN_HEIGHT_SHARE = 0.7f;
    public static final float TREE_STEM_MIN_SPREAD_SHARE = 0.5f;
    public static final int TREE_STRAND_DEPTH = TREE_MAX_BRANCH_LEVELS + 1;
    public static final float TREE_STRAND_LEAF_SHARE = 0.45f;
    public static final int TREE_STRAND_SEGMENTS = 3;
    public static final float TREE_STRAND_STRETCH = 2f;
    public static final float TREE_STRAND_SWAY = 0.12f;
    public static final float TREE_TRUNK_SEGMENT_BLOCKS = 3f;
    public static final float TREE_TRUNK_SINK_BLOCKS = 1f;
    public static final float TREE_TUFT_RADIUS_SHARE = 0.7f;
    public static final float TREE_UPRIGHT_LIMIT = 0.95f;
    public static final float TREE_WEEPING_DROOP = 0.35f;
    public static final int TREE_WHORL_MAX_BOUGHS = 6;
    public static final int TREE_WHORL_MIN_BOUGHS = 3;
    public static final float TREE_WHORL_TOP_SHARE = 0.96f;

    // Tree Raster — a node's radius in sub-voxels, and the salts of a leaf cluster's keep and accent rolls
    public static final float TREE_LEAF_CAST_SHARE = 0.86f;
    public static final long TREE_LEAF_ACCENT_SALT = 0x2C5E8A13F07D49B6L;
    public static final long TREE_LEAF_KEEP_SALT = 0x7A4D1C93E5B2068FL;
    public static final long TREE_LEAF_SEED_SALT = 0x51E3B7A90C4D682FL;
    public static final int TREE_MAX_NODE_RADIUS = 256;
    public static final int TREE_NODE_MAX_BOXES = 7;
    public static final int TREE_MIN_NODE_RADIUS = 1;

    // Tree Placement — the salts every placement roll is drawn with, the spacing biomes are looked for at around a
    // chunk, and the ground cache's bound and markers
    public static final long TREE_AGE_SALT = 0x3C81E5A7D20F96B4L;
    public static final int TREE_BIOME_SAMPLE_SPACING_BLOCKS = 16;
    public static final long TREE_CHANCE_SALT = 0x6F2A9D04C7B3E851L;
    public static final long TREE_CLUSTER_ANGLE_SALT = 0x0B57E3C9A1D86F24L;
    public static final long TREE_CLUSTER_COUNT_SALT = 0x94D0B62E5F1A7C38L;
    public static final long TREE_CLUSTER_SPREAD_SALT = 0x2E6C1F8A4B93D705L;
    public static final int TREE_GROUND_CACHE_MAX = 16384;
    public static final int TREE_GROUND_FLOODED = Integer.MIN_VALUE;
    public static final int TREE_GROUND_UNKNOWN = Integer.MAX_VALUE;
    public static final long TREE_OFFSET_X_SALT = 0xA7193F5C0E2D84B6L;
    public static final long TREE_OFFSET_Z_SALT = 0x5D8E2B07F6C1A943L;
    public static final long TREE_PATCH_SALT = 0xC24F6A1D93E05B78L;
    public static final long TREE_PLACEMENT_SALT = 0x1F9B4E72A8C6D035L;
    public static final long TREE_PLANTED_SEED_SALT = 0x6D03A9F1C85B2E47L;
    public static final long TREE_SEED_SALT = 0xE3A05C8B17F4296DL;

    // Tree Geometry — the blocks of wood laid around a chunk so its borders mesh as if nothing ended there, and
    // where a root's centre lies across its block
    public static final int TREE_GEOMETRY_MARGIN_BLOCKS = 1;
    public static final float TREE_ROOT_CENTER_BLOCKS = 0.5f;

    // Tree Lifetime — how long a tree nothing has touched keeps its grown shape before letting it go
    public static final float TREE_SHAPE_IDLE_SECONDS = 30f;

    // Tree Impostor — the few lumps and the trunk a mega draws for each tree rooted in it
    public static final float TREE_IMPOSTOR_COLUMNAR_TAPER = 0.45f;
    public static final float TREE_IMPOSTOR_CONIFER_TAPER = 0.9f;
    public static final int TREE_IMPOSTOR_CONE_TIERS = 3;
    public static final int TREE_IMPOSTOR_CROWN_BOTTOM = 0;
    public static final int TREE_IMPOSTOR_CROWN_FLOATS = 5;
    public static final int TREE_IMPOSTOR_CROWN_RADIUS = 2;
    public static final float TREE_IMPOSTOR_CROWN_REACH_SHARE = 0.55f;
    public static final int TREE_IMPOSTOR_CROWN_TOP = 1;
    public static final float TREE_IMPOSTOR_PALM_SQUASH = 0.35f;
    public static final long TREE_IMPOSTOR_SEED_SALT = 0x3A9E61C4F07B28D5L;
    public static final float TREE_IMPOSTOR_TIER_RADIUS_SHARE = 0.75f;
    public static final int TREE_IMPOSTOR_TRUNK_HALF_WIDTH = 4;
    public static final float TREE_IMPOSTOR_TRUNK_REACH_SHARE = 0.5f;
    public static final int TREE_IMPOSTOR_TRUNK_TOP = 3;

    // Tree Canopy — the jagged blanket distant macro terrain lays over its woods
    public static final float TREE_CANOPY_JITTER = 0.35f;
    public static final float TREE_CANOPY_MIN_COVERAGE = 0.3f;
    public static final long TREE_CANOPY_SALT = 0x6C2F94A1E8D3075BL;
    public static final float TREE_CANOPY_SIDE_SHADE = 0.7f;
    public static final float TREE_CANOPY_SINK_BLOCKS = 2f;

    // Tree Materials
    public static final String TREE_BARK_MATERIAL = "trees/TreeBarkMaterial";
    public static final String TREE_LEAF_MATERIAL = "trees/TreeLeafMaterial";
    public static final String TREE_BARK_FALLING_MATERIAL = "trees/TreeBarkFallingMaterial";
    public static final String TREE_LEAF_FALLING_MATERIAL = "trees/TreeLeafFallingMaterial";

    // Tree Fall — how a felled piece comes down, rests and breaks into its drops
    public static final long TREE_DROP_ANGLE_SALT = 0x8E2C47B05A1F96D3L;
    public static final float TREE_DROP_LIFT_BLOCKS = 0.5f;
    public static final float TREE_DROP_LIFT_SPEED = 3f;
    public static final float TREE_DROP_SPIN_RATE = 4f;
    public static final float TREE_DROP_SPREAD_SPEED = 1.5f;
    public static final float TREE_FALL_DROP_BLOCKS = 3f;
    public static final float TREE_FALL_HINGE_ACCELERATION = 1.5f;
    public static final float TREE_FALL_MIN_LENGTH_BLOCKS = 1f;
    public static final float TREE_FALL_REST_RADIANS = 1.48f;
    public static final float TREE_FALL_REST_SECONDS = 1.5f;
    public static final float TREE_FALL_START_RADIANS = 0.02f;
    public static final int TREE_MAX_LOG_DROPS = 40;
    public static final int TREE_MAX_LOG_KINDS = 8;
    public static final int TREE_MAX_SEED_DROPS = 4;
    public static final long TREE_SEED_DROP_SALT = 0x4B9D1E6372C0A85FL;

    // Tree Chop — a level stroke's wedge, in sub-voxels, and the share of a cross-section that must stand for the
    // wood to hold
    public static final float TREE_CHOP_BITE_SUB_VOXELS = 3f;
    public static final float TREE_CHOP_HALF_HEIGHT_SUB_VOXELS = 4f;
    public static final float TREE_CHOP_LEVEL_EPSILON = 0.05f;
    public static final float TREE_CHOP_MOUTH_SUB_VOXELS = 1f;
    public static final float TREE_CHOP_TIER_BITE_SUB_VOXELS = 1f;
    public static final float TREE_CHOP_WEDGE_TAPER = 0.5f;
    public static final int TREE_CHOP_WIDTH_MARGIN_SUB_VOXELS = 2;
    public static final float TREE_SEVER_REMAINING_SHARE = 0.25f;

    // Sky & Atmosphere
    public static final double DEGREES_PER_FULL_ROTATION = 360.0;
    public static final float SKY_BELT_ELEVATION_END = 0.10f;
    public static final float SKY_BELT_ELEVATION_PEAK = -0.07f;
    public static final float SKY_BELT_ELEVATION_START = -0.24f;
    public static final float SKY_CLOUD_COLOR_ACCENT_STRENGTH = 0.7f;
    public static final String SKY_COLOR_UBO = "SkyColorData";
    public static final float SKY_DAILY_BELT_HUE_DEGREES = 34.0f;
    public static final float SKY_DAILY_BELT_STRENGTH_RANGE = 0.50f;
    public static final float SKY_DAILY_CLOUD_HUE_DEGREES = 18.0f;
    public static final float SKY_DAILY_COLD_CRISP_BIAS = 0.5f;
    public static final float SKY_DAILY_COLD_HUE_BIAS_DEGREES = 12.0f;
    public static final float SKY_DAILY_CRISP_CLARITY = 0.20f;
    public static final float SKY_DAILY_DOME_HUE_DEGREES = 9.0f;
    public static final float SKY_DAILY_DUST_GLOW_BOOST = 0.45f;
    public static final float SKY_DAILY_DUST_HAZE = 0.25f;
    public static final float SKY_DAILY_GLOW_HUE_DEGREES = 28.0f;
    public static final float SKY_DAILY_GLOW_STRENGTH_RANGE = 0.40f;
    public static final float SKY_DAILY_HOT_DUST_BIAS = 0.5f;
    public static final float SKY_DAILY_HOT_HUE_BIAS_DEGREES = 6.0f;
    public static final float SKY_DAILY_SATURATION_RANGE = 0.20f;
    public static final long SKY_DAILY_STREAM_AIR = 9L;
    public static final long SKY_DAILY_STREAM_BELT_HUE = 3L;
    public static final long SKY_DAILY_STREAM_BELT_STRENGTH = 8L;
    public static final long SKY_DAILY_STREAM_CLOUD_HUE = 5L;
    public static final long SKY_DAILY_STREAM_DOME_HUE = 4L;
    public static final long SKY_DAILY_STREAM_GLOW_HUE = 2L;
    public static final long SKY_DAILY_STREAM_GLOW_STRENGTH = 7L;
    public static final long SKY_DAILY_STREAM_SATURATION = 6L;
    public static final float SKY_DAYLIGHT_ELEVATION_END = 0.35f;
    public static final float SKY_DAYLIGHT_ELEVATION_START = -0.08f;
    public static final float SKY_FOG_COLOR_LIFT = 0.04f;
    public static final float SKY_FOG_GLOW_TRANSFER = 0.35f;
    public static final float SKY_GLOW_ELEVATION_END = 0.30f;
    public static final float SKY_GLOW_ELEVATION_PEAK = -0.02f;
    public static final float SKY_GLOW_ELEVATION_START = -0.30f;
    public static final float SKY_HAZE_LIFT = 0.35f;
    public static final float SKY_HUE_AXIS_INVERSE_ROOT = 0.57735027f;
    public static final float SKY_HUMIDITY_HAZE = 0.20f;
    public static final float SKY_LUMINANCE_B = 0.0722f;
    public static final float SKY_LUMINANCE_G = 0.7152f;
    public static final float SKY_LUMINANCE_R = 0.2126f;
    public static final float SKY_OVERCAST_CLOUD_SHADE = 0.35f;
    public static final float SKY_OVERCAST_COVERAGE_WEIGHT = 0.8f;
    public static final float SKY_OVERCAST_DESATURATION = 0.75f;
    public static final float SKY_OVERCAST_DIMMING = 0.30f;
    public static final float SKY_OVERCAST_GLOW_DAMPING = 0.85f;
    public static final float SKY_OVERCAST_PRECIPITATION_WEIGHT = 0.6f;
    public static final float[] SKY_PHASE_ELEVATIONS = { -0.30f, -0.08f, 0.06f, 0.35f };
    public static final String[] SKY_PHASE_NAMES = { "night", "twilight", "golden", "day" };
    public static final float SKY_TEMPERATURE_ACCENT_STRENGTH = 0.55f;
    public static final float SKY_TEMPERATURE_COLD_ACCENT_B = 0.88f;
    public static final float SKY_TEMPERATURE_COLD_ACCENT_G = 0.75f;
    public static final float SKY_TEMPERATURE_COLD_ACCENT_R = 0.95f;
    public static final float SKY_TEMPERATURE_COLD_BELT_BOOST = 0.6f;
    public static final float SKY_TEMPERATURE_COLD_CLARITY = 0.15f;
    public static final float SKY_TEMPERATURE_COLD_REFERENCE = -10.0f;
    public static final float SKY_TEMPERATURE_HOT_ACCENT_B = 0.18f;
    public static final float SKY_TEMPERATURE_HOT_ACCENT_G = 0.55f;
    public static final float SKY_TEMPERATURE_HOT_ACCENT_R = 1.00f;
    public static final float SKY_TEMPERATURE_HOT_GLOW_BOOST = 0.35f;
    public static final float SKY_TEMPERATURE_HOT_HAZE = 0.30f;
    public static final float SKY_TEMPERATURE_HOT_REFERENCE = 30.0f;
    public static final float SKY_TEMPERATURE_MILD_REFERENCE = 12.0f;
    public static final String UNIFORM_SKY_BELT_COLOR = "u_skyBeltColor";
    public static final String UNIFORM_SKY_BLEND = "u_skyBlend";
    public static final String UNIFORM_SKY_CLOUD_COLOR = "u_skyCloudColor";
    public static final String UNIFORM_SKY_CLOUD_LIGHT_COLOR = "u_skyCloudLightColor";
    public static final String UNIFORM_SKY_CLOUD_SHADOW_COLOR = "u_skyCloudShadowColor";
    public static final String UNIFORM_SKY_FOG_COLOR = "u_skyFogColor";
    public static final String UNIFORM_SKY_GLOW_COLOR = "u_skyGlowColor";
    public static final String UNIFORM_SKY_HORIZON_COLOR = "u_skyHorizonColor";
    public static final String UNIFORM_SKY_ZENITH_COLOR = "u_skyZenithColor";

    // Weather
    public static final float DEFAULT_BIOME_WEATHER_CHANCE = 1.0f;
    public static final float DEFAULT_CLOUD_ENTRY_CHANCE = 1.0f;
    public static final float DEFAULT_CLOUD_ENTRY_DENSITY_MULTIPLIER = 1.0f;
    public static final float DEFAULT_WEATHER_CLOUD_COVERAGE = 0.0f;
    public static final float DEFAULT_WEATHER_CLOUD_DENSITY_MULTIPLIER = 1.0f;
    public static final float DEFAULT_WEATHER_FOG_DENSITY_SCALE = 1.0f;
    public static final float DEFAULT_WEATHER_HUMIDITY = 0.5f;
    public static final float DEFAULT_WEATHER_PRECIPITATION_INTENSITY = 0.0f;
    public static final float DEFAULT_WEATHER_TEMPERATURE_MODIFIER = 0.0f;
    public static final float DEFAULT_WEATHER_VISIBILITY = 1.0f;
    public static final float DEFAULT_WEATHER_WIND_SPEED_SCALE = 1.0f;
    public static final float DEFAULT_WEATHER_WIND_TURBULENCE_SCALE = 1.0f;
    public static final float KPH_TO_METERS_PER_SECOND = 1000f / 3600f;
    public static final int MAX_CLOUDS_PER_WEATHER = 3;
    public static final int WEATHER_CELL_RESOLVES_PER_FRAME = 32;
    public static final int WEATHER_CELL_SIZE_PIXELS = 2;
    public static final double WEATHER_FLOW_MEANDER_ANGLE_DEGREES = 18.0;
    public static final double WEATHER_FLOW_MEANDER_PERIOD_SECONDS = 108000.0;
    public static final double WEATHER_FLOW_MEANDER_SECONDARY_PERIOD_SECONDS = 39600.0;
    public static final double WEATHER_FLOW_MEANDER_SECONDARY_PHASE = 1.7;
    public static final double WEATHER_FLOW_MEANDER_SECONDARY_WEIGHT = 0.35;
    public static final double WEATHER_FLOW_RESYNC_SECONDS = 1.0;
    public static final double WEATHER_FLOW_SPEED_KPH = 40.0;
    public static final double WEATHER_FLOW_SYNC_RATE = 2.0;
    public static final long WEATHER_HASH_SALT_PRIMARY = 0x2545F4914F6CDD1DL;
    public static final long WEATHER_HASH_SALT_SECONDARY = 0x9E3779B97F4A7C15L;
    public static final long WEATHER_LOCAL_KEY_SEED = Long.MIN_VALUE;
    public static final float WEATHER_MAP_CHANNEL_MAX = 255f;
    public static final float WEATHER_MAP_DENSITY_SCALE_MAX = 2.5f;
    public static final int WEATHER_MAP_LAYERS_PER_COMPONENT = 4;
    public static final int WEATHER_MAP_MAX_LAYERS = 8;
    public static final int WEATHER_MAP_RESOLUTION = 24;
    public static final int WEATHER_MAP_RETAIN_MARGIN_CELLS = 1;
    public static final int WEATHER_MAP_SHAPE_PERIOD_MAX_CELLS = 16;
    public static final String WEATHER_MAP_UBO = "WeatherMapData";
    public static final float WEATHER_NOISE_CELL_SIZE = 512.0f;
    public static final double WEATHER_NOISE_CROSS_STREAM_COMPRESSION = 3.2;
    public static final double WEATHER_NOISE_DETAIL_FREQUENCY = 3.2;
    public static final float WEATHER_NOISE_DETAIL_WEIGHT = 0.26f;
    public static final int WEATHER_NOISE_DISTRIBUTION_SAMPLES_X = 128;
    public static final int WEATHER_NOISE_DISTRIBUTION_SAMPLES_Z = 64;
    public static final double WEATHER_NOISE_MACRO_FREQUENCY = 0.36;
    public static final float WEATHER_NOISE_MACRO_WEIGHT = 0.55f;
    public static final double WEATHER_NOISE_MIN_CYCLES_AROUND_WORLD = 4.0;
    public static final long WEATHER_NOISE_SEED = 0x51A5F00DCAFEBEEFL;
    public static final double WEATHER_REFERENCE_CIRCUMFERENCE_METERS = 40_075_000.0;
    public static final float WEATHER_TRANSITION_DURATION_SECONDS = 20.0f;

    // Cloud
    public static final float CLOUD_ALTITUDE_BLOCKS_PER_KILOMETER = 50.0f;
    public static final float CLOUD_ALTITUDE_FLOOR_BLOCKS = 570.0f;
    public static final float CLOUD_BLOCKS_PER_KILOMETER = 200.0f;
    public static final float CLOUD_DETAIL_FREQUENCY_RATIO = 4.0f;
    public static final int CLOUD_NOISE_CHANNELS = 3;
    public static final int CLOUD_NOISE_LOBE_CELLS = 32;
    public static final float CLOUD_NOISE_LOBE_JITTER = 0.6f;
    public static final float CLOUD_NOISE_LOBE_RADIUS = 0.72f;
    public static final long CLOUD_NOISE_SEED = 0x6C0D5EEDF1A7B33DL;
    public static final int CLOUD_NOISE_SHAPE_CELLS = 8;
    public static final int CLOUD_NOISE_SHAPE_OCTAVES = 4;
    public static final float CLOUD_NOISE_SHAPE_PERSISTENCE = 0.5f;
    public static final int CLOUD_NOISE_SIZE = 512;
    public static final int CLOUD_NOISE_TOWER_CELLS = 4;
    public static final int CLOUD_NOISE_TOWER_OCTAVES = 2;
    public static final float DEFAULT_CLOUD_BASE_ALTITUDE_KM = 1.5f;
    public static final float DEFAULT_CLOUD_COLOR_B = 1.0f;
    public static final float DEFAULT_CLOUD_COLOR_G = 1.0f;
    public static final float DEFAULT_CLOUD_COLOR_R = 1.0f;
    public static final float DEFAULT_CLOUD_COVERAGE_BIAS = 0.5f;
    public static final float DEFAULT_CLOUD_DENSITY = 0.8f;
    public static final float DEFAULT_CLOUD_DENSITY_NOISE_SCALE = 1.0f;
    public static final float DEFAULT_CLOUD_DRIFT_SPEED_SCALE = 1.0f;
    public static final float DEFAULT_CLOUD_ELONGATION = 1.5f;
    public static final float DEFAULT_CLOUD_FULLNESS = 0.7f;
    public static final float DEFAULT_CLOUD_NOISE_WARP_STRENGTH = 0.6f;
    public static final float DEFAULT_CLOUD_SATURATION = 1.0f;
    public static final float DEFAULT_CLOUD_SCALE_KM = 2.0f;
    public static final float DEFAULT_CLOUD_SILHOUETTE_SOFTNESS = 0.08f;
    public static final float DEFAULT_CLOUD_VERTICAL_THICKNESS_KM = 1.0f;
    public static final int MAX_CLOUD_TYPES = 8;
    public static final String UNIFORM_CLOUD_NOISE = "u_cloudNoise";

    // Wind
    public static final String UNIFORM_TEMPERATURE = "u_temperature";
    public static final String UNIFORM_WIND_DIRECTION = "u_windDirection";
    public static final String UNIFORM_WIND_SPEED = "u_windSpeed";
    public static final String WIND_DATA_UBO = "WindData";
    public static final float WIND_DIURNAL_PEAK_TIME = 0.65f;
    public static final float WIND_DIURNAL_STRENGTH = 0.25f;
    public static final float WIND_GLOBAL_DIRECTION_DEGREES = 45.0f;
    public static final float WIND_GLOBAL_SPEED = 1.0f;
    public static final float WIND_GUST_DIRECTION_FREQUENCY = 0.07f;
    public static final float WIND_GUST_DIRECTION_WOBBLE_DEGREES = 8.0f;
    public static final float WIND_GUST_SPEED_FREQUENCY = 0.13f;
    public static final double WIND_GUST_PRIMARY_WEIGHT = 0.6;
    public static final double WIND_GUST_SECONDARY_WEIGHT = 0.4;
    public static final double WIND_GUST_SECONDARY_PHASE = 1.7;
    public static final float WIND_GUST_SPEED_FREQUENCY_SECONDARY = 0.045f;
    public static final float WIND_MIN_SPEED_FLOOR = 0.05f;

    // Precipitation
    public static final String PRECIPITATION_DATA_UBO = "PrecipitationData";
    public static final int PRECIPITATION_HEIGHT_BITS = 16;
    public static final int PRECIPITATION_HEIGHT_MASK = 0xFFFF;
    public static final int PRECIPITATION_HEIGHTS_PER_BLOCK = SUB_VOXEL_RESOLUTION;
    public static final int PRECIPITATION_HEIGHTS_PER_INT = 2;
    public static final int PRECIPITATION_INTS_PER_VECTOR = 4;
    public static final int PRECIPITATION_MAP_SIZE = 64;
    public static final int PRECIPITATION_REFRESH_PER_FRAME = 128;
    public static final float PRECIPITATION_SNOW_BLEND_RANGE = 1.5f;
    public static final float PRECIPITATION_SNOW_TEMPERATURE = 0.5f;
    public static final int PRECIPITATION_STALE_REFRESH_LIMIT = 1024;
    public static final int PRECIPITATION_UNASSIGNED_COLUMN = Integer.MIN_VALUE;
    public static final int PRECIPITATION_UNKNOWN_HEIGHT = 0xFFFF;
    public static final float PRECIPITATION_WIND_DRIFT_SCALE = 1.0f;
    public static final String UNIFORM_PRECIPITATION_COLUMNS = "u_precipitationColumns";
    public static final String UNIFORM_PRECIPITATION_STATE = "u_precipitationState";
    public static final String UNIFORM_PRECIPITATION_WINDOW = "u_precipitationWindow";

    // Temperature
    public static final float DEFAULT_BASE_TEMPERATURE = 15.0f;
    public static final long TEMPERATURE_DAILY_STREAM = 1L;
    public static final float TEMPERATURE_DAILY_VARIANCE_SCALE = 0.6f;
    public static final float TEMPERATURE_DIURNAL_PEAK_TIME = 0.65f;
    public static final float TEMPERATURE_DRIFT_FREQUENCY = 0.02f;
    public static final float TEMPERATURE_KELVIN_OFFSET = 273.15f;
    public static final float TEMPERATURE_PRECIPITATION_COOLING = 4.0f;

    // Season
    public static final float DEFAULT_SEASON_BASE_WIND_SPEED = 3.0f;
    public static final float DEFAULT_SEASON_PRECIPITATION_CHANCE_SCALE = 1.0f;
    public static final float DEFAULT_SEASON_PREVAILING_WIND_DIRECTION_DEGREES = 0.0f;
    public static final float DEFAULT_SEASON_TEMPERATURE_VARIANCE = 5.0f;
    public static final float DEFAULT_SEASON_WIND_VARIANCE = 1.0f;
    public static final float LATITUDE_DAYLENGTH_CURVE_POWER = 1.0f;
    public static final double LATITUDE_DAYLENGTH_POLAR_GAIN = 3.0;
    public static final float LATITUDE_DAYLENGTH_POLAR_START = 0.6f;
    public static final float LATITUDE_DAYLENGTH_REFERENCE_TILT_DEGREES = 23.5f;
    public static final float SEASON_BLEND_RECOMPUTE_EPSILON = 0.00001f;

    // Time & Clock
    public static final long CLOCK_DAILY_STREAM_NOISE = 0L;
    public static final long CLOCK_DAILY_STREAM_SALT = 0x9E3779B97F4A7C15L;
    public static final float CLOCK_NOISE_MIN = 0.001f;
    public static final double CLOCK_NOON = 0.5;
    public static final double CLOCK_QUARTER = 0.25;
    public static final double CLOCK_SUNRISE_MAX = 0.45;
    public static final double CLOCK_SUNRISE_MIN = 0.05;
    public static final double CLOCK_SUNSET_MAX = 0.95;
    public static final double CLOCK_SUNSET_MIN = 0.55;
    public static final double CLOCK_THREE_QUARTERS = 0.75;
    public static final long MILLIS_PER_REAL_DAY = 86400000L;
    public static final double MILLIS_PER_SECOND = 1000.0;
    public static final float MILLIS_PER_SECOND_FLOAT = 1000f;

    // Physics & Movement
    public static final float COLLISION_SKIN_BLOCKS = 0.001f;
    public static final float FIXED_TIME_STEP = 0.02f;
    public static final int MAX_FIXED_STEPS_PER_FRAME = 5;
    public static final float GRAVITY_FORCE = 9.8f;
    public static final float GRAVITY_BLOCK_EPSILON = 0.0001f;
    public static final float GROUNDED_FALL_SPEED = 5.0f;
    public static final float JUMP_HOLD_FRACTION = 0.4f;
    public static final float JUMP_SCALE = 1.8f;
    public static final float MOVEMENT_ACCELERATION = 8.0f;
    public static final float MOVEMENT_SCALE = 1.5f;
    public static final float REACH_SCALE = 4.0f;
    public static final float STEP_UP_HEIGHT_BLOCKS = SUB_BLOCK_SIZE;

    // Entity, Rig & Player
    public static final float BLOCK_PLACEMENT_INTERVAL = 0.1f;
    public static final float BONE_WEIGHT_SUM_EPSILON = 0.001f;
    public static final float DEFAULT_BONE_SIZE = 0.25f;
    public static final float DEFAULT_ENTITY_SIZE = 1f;
    public static final float DEFAULT_ENTITY_WEIGHT = 1f;
    public static final float DEFAULT_EYE_LEVEL = 0.91f;
    public static final float DEFAULT_JUMP_DURATION = 0.5f;
    public static final float DEFAULT_JUMP_HEIGHT = 0.5f;
    public static final float DEFAULT_MOVEMENT_SPEED = 3.3f;
    public static final String DEFAULT_PLAYER_RACE = "HumanoidEntity";
    public static final float DEFAULT_REACH = 1f;
    public static final float DEFAULT_SPRINT_SPEED = 7f;
    public static final float DEFAULT_SWIM_SPEED = 2.4f;
    public static final float DEFAULT_TURN_RESPONSIVENESS = 12f;
    public static final float DEFAULT_WALK_SPEED = 1.4f;
    public static final float FACING_VELOCITY_EPSILON = 0.01f;
    public static final float FREE_CAMERA_FLIGHT_SPEED = 12f;
    public static final float FREE_CAMERA_SPRINT_MULTIPLIER = 4f;
    public static final float TURN_RATE_SMOOTHING = 8f;
    public static final int MAX_BONE_INFLUENCES = 4;
    public static final int SKINNED_BONE_TEXELS_PER_BONE = 3;
    public static final float SKINNED_HIDDEN_BONE_NONE = -1f;
    public static final int SKINNED_INSTANCE_APPEARANCE_FLOATS = 24;
    public static final int[] SKINNED_INSTANCE_ATTRIBUTE_SIZES = { 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 };
    public static final int SKINNED_INSTANCE_INITIAL_CAPACITY = 64;
    public static final int SKINNED_INSTANCE_MODEL_FLOATS = 16;
    public static final int SKINNED_INSTANCE_FLOATS = SKINNED_INSTANCE_MODEL_FLOATS
            + SKINNED_INSTANCE_APPEARANCE_FLOATS;

    // Attributes & Carrying
    public static final float CARRY_CAPACITY_PER_STRENGTH = 2f;
    public static final float DEFAULT_ARMOR = 0f;
    public static final float DEFAULT_ATTRIBUTE_VALUE = 10f;
    public static final float DEFAULT_CARRY_CAPACITY = 30f;
    public static final float DEFAULT_DAMAGE = 1f;
    public static final float DEFAULT_HEALTH = 100f;
    public static final float DEFAULT_STAMINA = 100f;

    // Equipment
    public static final String EQUIPMENT_ITEM_MATERIAL = "items/EquipmentItemMaterial";
    public static final int EQUIPMENT_RENDER_DEPTH = 0;
    public static final String UNIFORM_ITEM_MODEL = "u_model";

    // Animation
    public static final float ANIMATION_BLEND_SECONDS = 0.22f;
    public static final float ANIMATION_RATE_SCALE_MAX = 1.5f;
    public static final float ANIMATION_RATE_SCALE_MIN = 0.35f;
    public static final float DEFAULT_ANIMATION_LAYER_WEIGHT = 1f;
    public static final float DEFAULT_ANIMATION_NODE_DELAY = 0f;
    public static final float DEFAULT_ANIMATION_NODE_RATE = 1f;

    // Combat
    public static final float AIM_RAISE_SECONDS = 0.3f;
    public static final float ARMOR_MITIGATION_SCALE = 50f;
    public static final float BLOCK_ARC_COSINE = 0.5f;
    public static final float BLOCK_MITIGATION_BASE = 0.3f;
    public static final float BLOCK_MITIGATION_MAX = 0.9f;
    public static final float BLOCK_MITIGATION_PER_ARMOR = 0.05f;
    public static final float BLOCK_MITIGATION_PER_WEIGHT = 0.06f;
    public static final float BLOCK_RAISE_SECONDS = 0.2f;
    public static final float GESTURE_IMPACT = 0.5f;
    public static final float KNOCKBACK_SPEED = 4f;
    public static final float PICK_UP_GESTURE_SECONDS = 0.4f;
    public static final float PLACE_GESTURE_SECONDS = 0.3f;
    public static final float SWING_BASE_SECONDS = 0.4f;
    public static final float SWING_DAMAGE_PER_WEIGHT = 1.5f;
    public static final float SWING_IMPACT = 0.45f;
    public static final float SWING_MAX_SECONDS = 1f;
    public static final float SWING_SECONDS_PER_WEIGHT = 0.06f;

    // Throwing
    public static final float THROW_ARM_MASS = 1.5f;
    public static final float THROW_IMPULSE = 36f;
    public static final float THROW_RELEASE = 0.3f;
    public static final float THROW_RELEASE_DISTANCE = 0.6f;
    public static final float THROW_RELEASE_MARGIN = 0.05f;
    public static final float THROW_SECONDS = 0.35f;
    public static final float THROW_SPIN_RATE = 18f;
    public static final float THROW_STRENGTH_MIN_FACTOR = 0.5f;
    public static final float THROW_STRENGTH_SCALE = 0.05f;
    public static final float THROWN_DAMAGE_MIN = 1f;
    public static final float THROWN_DAMAGE_PER_MOMENTUM = 0.35f;

    // Projectiles
    public static final float PROJECTILE_AIR_DRAG = 0.05f;
    public static final float PROJECTILE_BOUNCE_FRICTION = 0.6f;
    public static final float PROJECTILE_BOUNCE_RESTITUTION = 0.25f;
    public static final float PROJECTILE_BOUNCE_SPIN_DAMPING = 0.5f;
    public static final float PROJECTILE_BREAK_MOMENTUM_PER_DURABILITY = 40f;
    public static final float PROJECTILE_DEBRIS_SCATTER = 3f;
    public static final float PROJECTILE_DEBRIS_SPEED_SHARE = 0.4f;
    public static final float PROJECTILE_DEBRIS_SPIN_RATE = 9f;
    public static final float PROJECTILE_LAND_SPEED = 2.5f;
    public static final float PROJECTILE_LIQUID_DRAG = 3f;
    public static final float PROJECTILE_MAX_FLIGHT_SECONDS = 20f;
    public static final int PROJECTILE_PIECE_CLIMB_LIMIT = 8;
    public static final int PROJECTILE_PIECE_RING_LIMIT = 2;
    public static final int PROJECTILE_RENDER_CHUNK_RADIUS = 2;
    public static final float PROJECTILE_SNAP_EPSILON = 0.001f;
    public static final float PROJECTILE_STRIKE_REBOUND = 0.2f;
    public static final float PROJECTILE_SURFACE_OFFSET = 0.001f;

    // Vehicles
    public static final float VEHICLE_AIR_DENSITY = 0.001225f;
    public static final int VEHICLE_BRACE_SAMPLES = 13;
    public static final float VEHICLE_DOOR_SWING_SECONDS = 0.5f;
    public static final float VEHICLE_DOWNFLOOD_UPRIGHTNESS = 0.75f;
    public static final float VEHICLE_DRAG_LINEAR_SPEED = 0.5f;
    public static final float VEHICLE_DRY_FLOOD_LIMIT = 0.5f;
    public static final String VEHICLE_ITEM_MATERIAL = "items/VehicleItemMaterial";
    public static final float VEHICLE_MAX_SPEED = 30f;
    public static final float VEHICLE_MAX_SPIN = 3f;
    public static final float VEHICLE_PUMP_UPRIGHTNESS = 0.85f;
    public static final int VEHICLE_RENDER_CHUNK_RADIUS = 12;
    public static final float VEHICLE_SAIL_FURLED_SCALE = 0.08f;
    public static final float VEHICLE_SWAMP_SHARE = 0.5f;
    public static final int VEHICLE_SUB_STEPS = 4;
    public static final float VEHICLE_WATER_DENSITY = 1.025f;
    public static final float VEHICLE_WHEEL_TURNS_PER_RUDDER = 6f;

    // Vehicle Categories
    public static final String VEHICLE_CATEGORY_TITLE_SHIP = "Ships";

    // Vehicle Hull
    public static final int VEHICLE_COLUMN_EXTENT_FLOATS = 5;
    public static final int VEHICLE_COLUMN_SUB_VOXELS = 32;
    public static final int VEHICLE_CONTACT_LAYER_STEP = 4;
    public static final int VEHICLE_DRY_CELL_SUB_VOXELS = 8;
    public static final int VEHICLE_HULL_MASK_BAND_CELLS = 2;
    public static final float VEHICLE_PITCH_GYRATION_RATIO = 0.26f;
    public static final float VEHICLE_ROLL_GYRATION_RATIO = 0.38f;
    public static final float VEHICLE_YAW_GYRATION_RATIO = 0.27f;

    // Vehicle Ground
    public static final float VEHICLE_GROUND_DAMPING = 20f;
    public static final int VEHICLE_GROUND_ESCAPE_STEPS = 4;
    public static final float VEHICLE_GROUND_FRICTION = 0.6f;
    public static final float VEHICLE_GROUND_FRICTION_SPEED = 0.5f;
    public static final float VEHICLE_GROUND_STIFFNESS = 200f;

    // Vehicle Riders
    public static final float VEHICLE_CLIMB_SIDESTEP_SHARE = 0.4f;
    public static final float VEHICLE_CLIMB_SPEED = 2.4f;
    public static final float VEHICLE_HELM_SLACK = 1f;
    public static final float VEHICLE_HULL_PROBE_HEIGHT = 0.25f;
    public static final int VEHICLE_LADDER_REACH_SUB_VOXELS = 8;
    public static final float VEHICLE_RIDER_REACH = 2f;

    // Vehicle Defaults
    public static final float DEFAULT_VEHICLE_ANCHOR_DROP = 6f;
    public static final float DEFAULT_VEHICLE_ANCHOR_HOLD = 0.6f;
    public static final float DEFAULT_VEHICLE_BRACE_LIMIT_DEGREES = 50f;
    public static final float DEFAULT_VEHICLE_BRACE_RATE_DEGREES = 8f;
    public static final float DEFAULT_VEHICLE_DOOR_OPEN_DEGREES = 90f;
    public static final float DEFAULT_VEHICLE_FLOOD_RATE = 0.06f;
    public static final float DEFAULT_VEHICLE_HEAVE_DRAG = 0.4f;
    public static final float DEFAULT_VEHICLE_HOIST_SECONDS = 6f;
    public static final float DEFAULT_VEHICLE_PUMP_RATE = 0.002f;
    public static final float DEFAULT_VEHICLE_RUDDER_FORCE = 3f;
    public static final float DEFAULT_VEHICLE_RUDDER_LIMIT_DEGREES = 35f;
    public static final float DEFAULT_VEHICLE_RUDDER_RATE_DEGREES = 15f;
    public static final float DEFAULT_VEHICLE_SAIL_FORCE = 20f;
    public static final float DEFAULT_VEHICLE_SURGE_DRAG = 0.004f;
    public static final float DEFAULT_VEHICLE_SWAY_DRAG = 0.1f;

    // Appearance
    public static final float DEFAULT_BUILD_FACTOR = 1f;
    public static final float DEFAULT_WEIGHT_RATIO = 0.5f;

    // Item
    public static final String DEFAULT_ITEM_MATERIAL = "items/StandardItemMaterial";
    public static final int ITEM_CLEARANCE_GRID_SPAN = 3;
    public static final String ITEM_DESCRIPTION_NONE = "";
    public static final String ITEM_DISPLAY_NAME_NONE = "";
    public static final String ITEM_NAME_WORD_BOUNDARY_PATTERN = "(?<=[a-z])(?=[A-Z])";
    public static final String ITEM_NAME_WORD_SEPARATOR = " ";
    public static final int ITEM_PLACEMENT_PUSH_LIMIT = SUB_VOXEL_RESOLUTION;
    public static final float ITEM_RAY_EPSILON = 1e-4f;
    public static final int ITEM_ROTATION_COUNT = 4;
    public static final int DEFAULT_ITEM_STACK_SIZE = 1;
    public static final String ITEM_MESH_NONE = "";
    public static final String ITEM_TOOL_NONE = "";
    public static final int MAX_ITEM_STACK_SIZE = 512;

    // Item Actions
    public static final String ITEM_ACTION_FIRE_NONE = "";
    public static final String ITEM_ACTION_HELD_ANY = "";
    public static final String ITEM_PICK_UP_AS_SELF = "";
    public static final String ITEM_PLANTS_NONE = "";

    // Tools
    public static final int DEFAULT_TOOL_TIER = 0;
    public static final short TOOL_NONE = 0;

    // Block Pieces
    public static final String BLOCK_ITEM_TEXTURE_NONE = "";
    public static final String BLOCK_PIECE_DESCRIPTION = "A piece of a block, ready to build with.";
    public static final String BLOCK_PIECE_NAME_PREFIX = "block:";
    public static final short BLOCK_PIECE_NONE = -1;
    public static final String BLOCK_PIECE_PART_NAME = "Piece";
    public static final int BLOCK_PIECE_SIZE = 2;
    public static final int BLOCK_PIECE_STACK_SIZE = MAX_ITEM_STACK_SIZE;
    public static final float BLOCK_PIECE_WEIGHT = 0.1f;

    // Item Categories
    public static final String ITEM_CATEGORY_TITLE_ARMOR = "Armor";
    public static final String ITEM_CATEGORY_TITLE_CLOTHING = "Clothing";
    public static final String ITEM_CATEGORY_TITLE_CONSUMABLE = "Consumables";
    public static final String ITEM_CATEGORY_TITLE_CONTAINER = "Containers";
    public static final String ITEM_CATEGORY_TITLE_FURNITURE = "Furniture";
    public static final String ITEM_CATEGORY_TITLE_JEWELRY = "Jewelry";
    public static final String ITEM_CATEGORY_TITLE_MATERIAL = "Materials";
    public static final String ITEM_CATEGORY_TITLE_MISC = "Miscellaneous";
    public static final String ITEM_CATEGORY_TITLE_TOOL = "Tools";
    public static final String ITEM_CATEGORY_TITLE_WEAPON = "Weapons";

    // Item Stats
    public static final String ITEM_STAT_TITLE_ARMOR = "Armor";
    public static final String ITEM_STAT_TITLE_CHARISMA = "Charisma";
    public static final String ITEM_STAT_TITLE_CONSTITUTION = "Constitution";
    public static final String ITEM_STAT_TITLE_DAMAGE = "Damage";
    public static final String ITEM_STAT_TITLE_DEXTERITY = "Dexterity";
    public static final String ITEM_STAT_TITLE_HEALTH = "Health";
    public static final String ITEM_STAT_TITLE_INTELLIGENCE = "Intelligence";
    public static final String ITEM_STAT_TITLE_STAMINA = "Stamina";
    public static final String ITEM_STAT_TITLE_STRENGTH = "Strength";
    public static final String ITEM_STAT_TITLE_WISDOM = "Wisdom";

    // Container
    public static final int CONTAINER_MAX_CELLS = 65536;
    public static final float CONTAINER_RAY_EPSILON = 1e-4f;
    public static final String CONTAINER_TEXTURE_NONE = "";

    // Sub-Voxel Model
    public static final int SUB_VOXEL_CELL_COUNT = SUB_VOXEL_RESOLUTION * SUB_VOXEL_RESOLUTION
            * SUB_VOXEL_RESOLUTION;
    public static final int SUB_VOXEL_EMPTY_CELL = 0;
    public static final int SUB_VOXEL_FACE_COUNT = 6;
    public static final float SUB_VOXEL_IMPORT_EPSILON = 1e-6f;
    public static final float SUB_VOXEL_IMPORT_RAY_Y = 0.0137f;
    public static final float SUB_VOXEL_IMPORT_RAY_Z = 0.0071f;
    public static final int SUB_VOXEL_MAX_PARTS = 255;
    public static final int SUB_VOXEL_MAX_MODEL_BLOCKS = 8;
    public static final String SUB_VOXEL_VAO = "util/vao/ItemVAO";
    public static final int SUB_VOXEL_UV_BOUNDS_FLOATS = 4;
    public static final int SUB_VOXEL_VERTEX_STRIDE = 6;
    public static final int SUB_VOXEL_AXIS_COUNT = 3;
    public static final float SUB_VOXEL_WALL_EDGE_SNAP = 0.25f;
    public static final String[] SUB_VOXEL_WALL_AXIS_KEYS = { "x", "y", "z" };

    // Font
    public static final String FONT_DEFAULT_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,!?:;'\"-+*/\\()[]{}@#$%^&=<>|~`_";
    public static final Color FONT_DEFAULT_COLOR = Color.RED;
    public static final String FONT_DEFAULT_MATERIAL = "fonts/StandardFontMaterial";
    public static final String FONT_DEFAULT_MESH = "fonts/FontMesh";
    public static final String FONT_DEFAULT_NAME = "MontserratAlternates";
    public static final String FONT_DEFAULT_SIZE_PERCENT = "50%";
    public static final float FONT_FIT_PADDING_X_PIXELS = 6f;
    public static final float FONT_FIT_PADDING_Y_PIXELS = 2f;
    public static final float FONT_LETTER_SPACING_RATIO = 0.05f;
    public static final int FONT_RASTER_SIZE = 24;
    public static final float FONT_SPACE_ADVANCE_RATIO = 0.25f;

    // Menu & UI
    public static final float DROPDOWN_COLLAPSE_TOLERANCE = 12f;
    public static final String ELEMENT_DEFAULT_MAX_SIZE = "100%";
    public static final String ELEMENT_DEFAULT_MIN_SIZE = "0%";
    public static final String ELEMENT_DEFAULT_POSITION = "0%";
    public static final String ELEMENT_DEFAULT_SIZE = "100%";
    public static final String HIERARCHY_COLLAPSED_MARKER = "+";
    public static final String HIERARCHY_ELEMENT_LABEL = "hierarchy_label";
    public static final String HIERARCHY_ELEMENT_TAB_LABEL = "hierarchy_tab_label";
    public static final String HIERARCHY_ELEMENT_TOGGLE = "hierarchy_toggle";
    public static final String HIERARCHY_ELEMENT_TOGGLE_LABEL = "hierarchy_toggle_label";
    public static final int HIERARCHY_ENTRY_ROWS = 1;
    public static final int HIERARCHY_ENTRY_TAB_BAR = 2;
    public static final int HIERARCHY_ENTRY_TABS = 0;
    public static final String HIERARCHY_EXPANDED_MARKER = "-";
    public static final float HIERARCHY_INDENT_PIXELS = 14f;
    public static final String HIERARCHY_LEAF_MARKER = "";
    public static final String HIERARCHY_ROW_SELECTED_TEMPLATE = "util/Hierarchy/hierarchy_row_selected";
    public static final float HIERARCHY_ROWS_MARGIN_PIXELS = 8f;
    public static final String HIERARCHY_ROW_TEMPLATE = "util/Hierarchy/hierarchy_row";
    public static final String HIERARCHY_TAB_ACTIVE_TEMPLATE = "util/Hierarchy/hierarchy_tab_active";
    public static final float HIERARCHY_TAB_BAR_PADDING_PIXELS = 4f;
    public static final float HIERARCHY_TAB_ROW_HEIGHT_PIXELS = 26f;
    public static final String HIERARCHY_TAB_ROW_TEMPLATE = "util/Hierarchy/hierarchy_tab_row";
    public static final float HIERARCHY_TAB_SPACING_PIXELS = 2f;
    public static final String HIERARCHY_TAB_TEMPLATE = "util/Hierarchy/hierarchy_tab";
    public static final float HIERARCHY_TAB_WIDTH_PIXELS = 96f;
    public static final float HIERARCHY_TOGGLE_WIDTH_PIXELS = 16f;
    public static final int MAX_MASK_DEPTH = 8;
    public static final int DEFAULT_RENDER_DEPTH = 0;
    public static final int SUB_BLOCK_AXIS_BIT_X = 1;
    public static final int SUB_BLOCK_AXIS_BIT_Z = 2;
    public static final int SUB_BLOCK_AXIS_BIT_Y = 4;
    public static final int FBO_LAYER_STRIDE = 10_000;
    public static final int SUB_VOXEL_FLOATS_PER_TRIANGLE = 9;
    public static final int PARTIAL_CELLS_PER_EDGE_ENTRY = 1;
    public static final int FONT_FLOATS_PER_INSTANCE = 8;
    public static final int MAIN_WINDOW_ID = 0;
    public static final int SCREEN_ORDER_BACKGROUND = 0;
    public static final int SCREEN_ORDER_FOREGROUND = 1;
    public static final float MENU_ANIMATION_MAX_STEP_SECONDS = 0.1f;
    public static final float MENU_EASE_BACK_OVERSHOOT = 1.70158f;
    public static final String MENU_HIERARCHY = "util/Hierarchy/Hierarchy";
    public static final float MENU_SCROLL_PIXELS = 48f;
    public static final float MENU_THEME_ALPHA_DEFAULT = 1.0f;

    // Screen Capture & Recording
    public static final int AVI_FLAG_HAS_INDEX = 0x00000010;
    public static final int AVI_FRAME_CHUNK_HEADER_LENGTH_BYTES = 8;
    public static final int AVI_INDEX_ENTRY_LENGTH_BYTES = 16;
    public static final int AVI_INDEX_FLAG_KEYFRAME = 0x00000010;
    public static final int AVI_MAIN_HEADER_LENGTH_BYTES = 56;
    public static final int AVI_STREAM_FORMAT_LENGTH_BYTES = 40;
    public static final int AVI_STREAM_HEADER_LENGTH_BYTES = 56;
    public static final String AVI_UNCOMPRESSED_FOURCC = "DIB ";
    public static final int BYTES_PER_PIXEL_BGRA = 4;
    public static final String CAPTURE_ROOT_DIRECTORY = "Capture";
    public static final String CAPTURE_TIMESTAMP_PATTERN = "yyyy-MM-dd_HH-mm-ss";
    public static final int RECORDING_LOSSLESS_FRAME_RATE = 60;
    public static final int RECORDING_CAPTURE_MAX_OWED_FRAMES = RECORDING_LOSSLESS_FRAME_RATE * 2;
    public static final String RECORDING_CONVERSION_TEMP_SUFFIX = ".converting";
    public static final String RECORDING_FILE_PREFIX = "Recording_";
    public static final int RECORDING_LOSSLESS_BUFFER_COUNT = 4;
    public static final String RECORDING_LOSSLESS_EXTENSION = "avi";
    public static final int RECORDING_LOSSLESS_MAX_OWED_FRAMES = RECORDING_LOSSLESS_FRAME_RATE * 2;
    public static final String RECORDING_OUTPUT_DIRECTORY = "Videos";
    public static final String RECORDING_STANDARD_EXTENSION = "mp4";
    public static final int RECORDING_STANDARD_FRAME_RATE = 30;
    public static final int RECORDING_STANDARD_FRAME_SAMPLE_STRIDE = RECORDING_LOSSLESS_FRAME_RATE
            / RECORDING_STANDARD_FRAME_RATE;
    public static final int RECORDING_STANDARD_MAX_WIDTH = 1280;
    public static final int RIFF_CHUNK_HEADER_LENGTH_BYTES = 8;
    public static final String SCREENSHOT_FILE_PREFIX = "Screenshot_";
    public static final String SCREENSHOT_LOSSLESS_EXTENSION = "tga";
    public static final String SCREENSHOT_OUTPUT_DIRECTORY = "Images";
    public static final String SCREENSHOT_STANDARD_FORMAT = "png";
    public static final String SCREEN_CAPTURE_THREAD_NAME = "ScreenCapture";
    public static final int TGA_HEADER_LENGTH_BYTES = 18;
    public static final int TGA_IMAGE_DESCRIPTOR_BOTTOM_LEFT_ALPHA = 0x08;
    public static final int TGA_IMAGE_TYPE_UNCOMPRESSED_TRUECOLOR = 2;
    public static final int TGA_PIXEL_DEPTH_BITS = 32;
    public static final String VIDEO_ENCODE_THREAD_NAME = "VideoEncode";
    public static final String VIDEO_WRITE_THREAD_NAME = "VideoWrite";
}
