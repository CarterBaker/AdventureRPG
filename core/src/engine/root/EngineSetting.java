package engine.root;

import engine.graphics.color.Color;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class EngineSetting {

        // OpenGL Constants \\

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
        public static final int GL_TRIANGLES = 0x0004;
        public static final int GL_UNIFORM_BUFFER = 0x8A11;
        public static final int GL_UNSIGNED_BYTE = 0x1401;
        public static final int GL_UNSIGNED_SHORT = 0x1403;
        public static final int GL_VERTEX_SHADER = 0x8B31;

        // Sentinel & Utility Values \\

        public static final int AXIS_X = 0;
        public static final int AXIS_Y = 1;
        public static final int AXIS_Z = 2;
        public static final int COLOR_CHANNEL_COUNT = 4;
        public static final float COLOR_CHANNEL_MAX = 1.0f;
        public static final float COLOR_CHANNEL_MIN = 0.0f;
        public static final int FNV_OFFSET_BASIS = 0x811c9dc5;
        public static final int FNV_PRIME = 0x01000193;
        public static final int GL_HANDLE_NONE = 0;
        public static final int GL_INVALID_INDEX = 0xFFFFFFFF;
        public static final long HASH_FINALIZER_MULTIPLIER_1 = 0xff51afd7ed558ccdL;
        public static final long HASH_FINALIZER_MULTIPLIER_2 = 0xc4ceb9fe1a85ec53L;
        public static final int INDEX_NOT_FOUND = -1;
        public static final double NOISE_SEAM_BLEND_WAVELENGTHS = 3.0;
        public static final short REGISTRY_RESERVED_ID = 0;
        public static final int REGISTRY_SHORT_ID_COUNT = 0x8000;

        // Engine & Application \\

        public static final String BIN_DIRECTORY = "bin";
        public static final String CHARACTER_FILE_EXTENSION = "json";
        public static final int CHARACTER_NAME_FIRST_NUMBER = 1;
        public static final String CHARACTER_NAME_PREFIX = "Character ";
        public static final String CHARACTER_SAVE_DIRECTORY = "Characters";
        public static final String EDITOR_LAYOUT_DIRECTORY = "editorLayout";
        public static final String EDITOR_SETTINGS_FILE_NAME = "EditorSettings.json";
        public static final String GAME_DIRECTORY = "AdventureRPG";
        public static final String GAME_DOCUMENTS_SUBPATH = "Documents/My Games";
        public static final int LOADER_BATCH_SIZE = 32;
        public static final String SAVE_DIRECTORY = "Saves";
        public static final String SETTINGS_FILE_NAME = "Settings.json";
        public static final String SETTINGS_UBO = "SettingsData";
        public static final String VERSION = "0.0.0.1a";

        // Logging \\

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

        // File Paths & Extensions \\

        public static final String ANIMATION_JSON_PATH = "animations";
        public static final String ANIMATION_TREE_JSON_PATH = "animationtrees";
        public static final String BEHAVIOR_JSON_PATH = "behaviors";
        public static final String BIOME_JSON_PATH = "biomes";
        public static final String BLOCK_JSON_PATH = "blocks";
        public static final String BLOCK_TEXTURE_ALIAS_PATH = "texturealiases";
        public static final String BLOCK_TEXTURE_PATH = "textures";
        public static final String CALENDAR_JSON_PATH = "calendars";
        public static final String CLOUD_JSON_PATH = "clouds";
        public static final String ENTITY_JSON_PATH = "entities";
        public static final String FEATURE_JSON_PATH = "features";
        public static final String FBO_CATALOG_JSON_PATH = "application/fbos";
        public static final ObjectArraySet<String> FONT_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "ttf",
                "otf" });
        public static final String FONT_PATH = "fonts";
        public static final ObjectArraySet<String> FRAG_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "fsh",
                "frag", "fs", "fragment", "pixel" });
        public static final ObjectArraySet<String> INCLUDE_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "glsl",
                "inc", "glslinc" });
        public static final String ITEM_JSON_PATH = "items";
        public static final ObjectArraySet<String> JSON_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "json" });
        public static final String MATERIAL_JSON_PATH = "materials";
        public static final String MENU_JSON_PATH = "menus";
        public static final String MESH_JSON_PATH = "mesh";
        public static final String PASS_JSON_PATH = "processingpasses";
        public static final String RIG_JSON_PATH = "rigs";
        public static final String SEASON_JSON_PATH = "seasons";
        public static final String SHADER_PATH = "shaders";
        public static final String SPRITE_PATH = "sprites";
        public static final String STRUCTURE_JSON_PATH = "structures";
        public static final ObjectArraySet<String> TCS_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "tcs",
                "tesc" });
        public static final ObjectArraySet<String> TES_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "tes",
                "tese" });
        public static final ObjectArraySet<String> TEXTURE_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "png",
                "jpg", "jpeg", "tga", "bmp" });
        public static final String THREAD_CATALOG_PATH = "application/threads";
        public static final String TOOL_TYPE_JSON_PATH = "tools";
        public static final String UBO_JSON_PATH = "ubos";
        public static final ObjectArraySet<String> VERT_FILE_EXTENSIONS = new ObjectArraySet<>(new String[] { "vsh",
                "vert", "vs", "vertex" });
        public static final String WEATHER_JSON_PATH = "weathers";
        public static final String WORLD_TEXTURE_PATH = "worlds";

        // Threading & Frame Rate \\

        public static final int AUTO_THREAD_POOL_RESERVED_CORES = 2;
        public static final int DEFAULT_IN_FLIGHT_MULTIPLIER = 3;
        public static final long FRAME_PACING_SLEEP_CHUNK_NANOS = 1_000_000L;
        public static final long FRAME_PACING_SLEEP_THRESHOLD_NANOS = 2_000_000L;
        public static final int MAX_THREAD_POOL_SIZE = 32;
        public static final int MIN_AUTO_THREAD_POOL_SIZE = 2;
        public static final long NANOS_PER_MILLI = 1_000_000L;
        public static final long NANOS_PER_SECOND = 1_000_000_000L;
        public static final int TARGET_FRAME_RATE = 60;

        // Window & Display \\

        public static final int CURSOR_RESIZE_H = 1;
        public static final int CURSOR_RESIZE_V = 2;
        public static final int MIN_WINDOW_DIMENSION = 64;
        public static final int WINDOW_POSITION_UNSET = Integer.MIN_VALUE;
        public static final String WINDOW_TITLE = "TerraArcana";

        // World Scale \\

        public static final int BIOME_SIZE = 4;
        public static final int BLOCK_PALETTE_THRESHOLD = 512;
        public static final float BLOCK_SIZE = 1.0f;
        public static final int CHUNKS_PER_PIXEL = 32;
        public static final int CHUNK_SIZE = 16;
        public static final int MEGA_CHUNK_SIZE = 4;
        public static final int SUB_VOXEL_RESOLUTION = 16;
        public static final int WORLD_HEIGHT = 64;

        // Natural Noise \\

        public static final float NATURAL_GROUND_OFFSET_SMOOTHING = 10.0f;
        public static final float NATURAL_NOISE_COLLISION_GRADIENT_PROBE_BLOCKS = 0.05f;
        public static final float NATURAL_NOISE_DISTANT_RISE_MARGIN_BLOCKS = 512.0f;
        public static final float NATURAL_NOISE_HASH_DOT_X = 127.1f;
        public static final float NATURAL_NOISE_HASH_DOT_Z = 311.7f;
        public static final float NATURAL_NOISE_HASH_SCALE = 43758.5453f;
        public static final float NATURAL_NOISE_JITTER_HORIZONTAL_BLOCKS = 0.4f;
        public static final float NATURAL_NOISE_JITTER_VERTICAL_BLOCKS = 0.08f;
        public static final float NATURAL_NOISE_SEED_SCALE = 0.5f;
        public static final int NATURAL_NOISE_LATTICE_PERIOD = (int) (CHUNK_SIZE * NATURAL_NOISE_SEED_SCALE);
        public static final int NATURAL_NOISE_LATTICE_SIZE = NATURAL_NOISE_LATTICE_PERIOD * NATURAL_NOISE_LATTICE_PERIOD;
        public static final int NATURAL_NOISE_LATTICE_VEC4_COUNT = (NATURAL_NOISE_LATTICE_SIZE + 3) / 4;
        public static final float NATURAL_NOISE_OFFSET_X_X = 17.3f;
        public static final float NATURAL_NOISE_OFFSET_X_Z = 0.0f;
        public static final float NATURAL_NOISE_OFFSET_Y_X = 53.1f;
        public static final float NATURAL_NOISE_OFFSET_Y_Z = 83.2f;
        public static final float NATURAL_NOISE_OFFSET_Z_X = 0.0f;
        public static final float NATURAL_NOISE_OFFSET_Z_Z = 31.7f;
        public static final String NATURAL_NOISE_UBO = "NaturalNoiseData";

        // Rendering Pipeline \\

        public static final String BLIT_PREMULTIPLIED_UNIFORM = "u_premultiplied";
        public static final String BLIT_RESOLVE_UNIFORM = "u_resolve";
        public static final String CAMERA_DATA_UBO = "CameraData";
        public static final int COMPOSITE_BUFFER_INITIAL_CAPACITY = 64;
        public static final int COMPOSITE_UPLOAD_BUFFER_GROWTH_FACTOR = 2;
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
        public static final String UBO_TIME_DATA_NAME = "TimeData";

        // Camera \\

        public static final float CAMERA_FAR_PLANE = 1000f;
        public static final float CAMERA_FIRST_PERSON_THRESHOLD = 0.1f;
        public static final float CAMERA_MAX_PITCH_DEGREES = 89f;
        public static final float CAMERA_NEAR_PLANE = 0.1f;
        public static final float CAMERA_ZOOM_DEFAULT = 4f;
        public static final float CAMERA_ZOOM_MAX = 6f;
        public static final float CAMERA_ZOOM_MIN = 0f;
        public static final float CAMERA_ZOOM_SCROLL_SPEED = 0.75f;
        public static final float CAMERA_ZOOM_SMOOTHING = 10f;
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

        // Post Processing \\

        public static final String SSAO_DATA_UBO = "SSAOData";
        public static final int SSAO_KERNEL_SIZE = 64;

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
        public static final float SUN_HORIZON_CUTOFF = 0.85f;
        public static final String UNIFORM_MOON_COLOR = "u_moonColor";
        public static final String UNIFORM_MOON_DIRECTION = "u_moonDirection";
        public static final String UNIFORM_MOON_INTENSITY = "u_moonIntensity";
        public static final String UNIFORM_SUN_COLOR = "u_sunColor";
        public static final String UNIFORM_SUN_DIRECTION = "u_sunDirection";
        public static final String UNIFORM_SUN_INTENSITY = "u_sunIntensity";

        // Block & World \\

        public static final String AIR_BLOCK_NAME = "TerraArcanaBlocks/Air";
        public static final float BLOCK_VISCOSITY_UNDEFINED = -1.0f;
        public static final int CHUNK_POOL_MAX_OVERFLOW = 32;
        public static final String CHUNK_VAO = "util/vao/ChunkVAO";
        public static final int COMPLEX_TICK_INTERVAL_FRAMES = 60;
        public static final int COMPLEX_TICK_PHASE_FRAMES = 30;
        public static final float DEFAULT_AXIAL_TILT_DEGREES = 23.5f;
        public static final short DEFAULT_BLOCK_ORIENTATION = (short) (DEFAULT_BLOCK_DIRECTION * 4);
        public static final String DEFAULT_CALENDAR_NAME = "standard/Default";
        public static final float DEFAULT_GRAVITY_MULTIPLIER = 1.0f;
        public static final float DEFAULT_GRAVITY_X = 0.0f;
        public static final float DEFAULT_GRAVITY_Y = -1.0f;
        public static final float DEFAULT_GRAVITY_Z = 0.0f;
        public static final float DEFAULT_PLANETARY_OFFSET = 0.0f;
        public static final float DEFAULT_WORLD_ROTATION_SPEED = 1.0f;
        public static final int ENCODED_FACE_NATURAL_FULL_OFFSET = 24;
        public static final int FULL_TICK_INTERVAL_FRAMES = 60;
        public static final int FULL_TICK_PHASE_FRAMES = 0;
        public static final int GRID_SLOTS_SCAN_PER_FRAME = 32;
        public static final int LIQUID_TICK_INTERVAL_FRAMES = 6;
        public static final int LIQUID_TICK_PHASE_FRAMES = 3;
        public static final int MAX_CHUNK_GPU_UPLOADS_PER_FRAME = 16;
        public static final int MAX_CHUNK_STREAM_PER_BATCH = 32;
        public static final int MAX_CHUNK_STREAM_PER_FRAME = 128;
        public static final int MAX_CHUNK_STREAM_PER_QUEUE = 1024;
        public static final int MAX_MEGA_GPU_UPLOADS_PER_FRAME = 4;
        public static final int MEGA_ASSESS_PER_FRAME = GRID_SLOTS_SCAN_PER_FRAME / MEGA_CHUNK_SIZE;
        public static final int MEGA_POOL_MAX_OVERFLOW = 8;
        public static final int PARTIAL_TICK_INTERVAL_FRAMES = 60;
        public static final int PARTIAL_TICK_PHASE_FRAMES = 15;
        public static final String STARTING_WORLD = "TerraArcana";

        // Liquid & Swimming \\

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
        public static final float SWIM_TREAD_RESPONSIVENESS = 6.0f;
        public static final float SWIM_TREAD_SPEED = 2.0f;
        public static final float SWIM_UP_SPEED = 2.4f;
        public static final float SWIM_VERTICAL_RESPONSIVENESS = 4.0f;
        public static final float SWIM_VERTICAL_STATE_SPEED = 0.8f;
        public static final float SWIM_VISCOSITY_DRAG_SCALE = 0.12f;
        public static final float SWIM_VISCOSITY_REFERENCE = 1.0f;
        public static final float WADE_DEEP_SPEED_MULTIPLIER = 0.55f;
        public static final float WADE_RUN_SPEED_MULTIPLIER = 0.4f;
        public static final float WADE_SHALLOW_DEPTH_FACTOR = 0.4f;
        public static final float WATER_ENTRY_LEAP_MAX_FALL_SPEED = 5.0f;
        public static final float WATER_ENTRY_LEAP_MIN_DEPTH_FACTOR = 0.3f;
        public static final float WATER_ENTRY_LEAP_MULTIPLIER = 0.6f;
        public static final float WATER_JUMP_DEEP_MULTIPLIER = 0.4f;
        public static final float WATER_JUMP_MIN_HEIGHT = 0.35f;

        // Biome \\

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
        public static final int BIOME_PATCH_CELLS_PER_PIXEL = 1;
        public static final float BIOME_PATCH_CELL_JITTER = 0.4f;
        public static final float BIOME_PATCH_FALLOFF_POWER = 2.5f;
        public static final float BIOME_PATCH_KERNEL_RADIUS_CELLS = 1.25f;
        public static final long BIOME_PATCH_SEED = 0x4F1D2C6E9A7B31F5L;
        public static final int BIOME_PATCH_SAMPLE_COUNT = 9;
        public static final float BIOME_SHORE_BUFFER_FULL_OCEAN_SHARE = 0.12f;

        // Terrain Generation \\

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

        // Ocean & Tide \\

        public static final String OCEAN_DATA_UBO = "OceanData";
        public static final int OCEAN_SPILL_CHUNKS = 2;
        public static final float OCEAN_REACH_THRESHOLD = BIOME_OCEAN_FLOOD_THRESHOLD
                        - OCEAN_SPILL_CHUNKS / (BIOME_BLEND_BAND_PIXELS * CHUNKS_PER_PIXEL);
        public static final float OCEAN_TIDE_AMPLITUDE_BLOCKS = 3.0f;
        public static final int OCEAN_TIDE_CHUNKS_PER_TICK = 24;
        public static final int OCEAN_TIDE_LEVEL_STEP = 4;
        public static final float OCEAN_TIDE_NEAP_AMPLITUDE_RATIO = 0.6f;
        public static final double OCEAN_TIDE_PEAK_TIME_OF_DAY = 0.0;
        public static final int OCEAN_TIDE_RANGE_CHUNKS = 16;
        public static final double OCEAN_TIDE_SPRING_NEAP_PERIOD_DAYS = 14.0;
        public static final int OCEAN_TIDE_UNAPPLIED = Integer.MIN_VALUE;

        // Ocean Turbulence & Waves \\

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
        public static final float OCEAN_WAVE_AMPLITUDE_PER_TURBULENCE_BLOCKS = 0.18f;
        public static final float[] OCEAN_WAVE_AMPLITUDE_RATIOS = { 1.0f, 0.55f, 0.35f, 0.2f };
        public static final float[] OCEAN_WAVE_ANGLES_DEGREES = { 0.0f, 28.0f, -35.0f, 62.0f };
        public static final float OCEAN_WAVE_BASE_WAVELENGTH_BLOCKS = 24.0f;
        public static final int OCEAN_WAVE_COUNT = 4;
        public static final float OCEAN_WAVE_MAX_AMPLITUDE_BLOCKS = 0.9f;
        public static final float OCEAN_WAVE_SPEED_SCALE = 1.0f;
        public static final double OCEAN_WAVE_TIME_WRAP_SECONDS = 3600.0;
        public static final float[] OCEAN_WAVE_WAVELENGTH_RATIOS = { 1.0f, 0.62f, 0.41f, 0.27f };
        public static final String UNIFORM_OCEAN_SURFACE = "u_oceanSurface";
        public static final String UNIFORM_OCEAN_TURBULENCE_CELLS = "u_oceanTurbulenceCells";
        public static final String UNIFORM_OCEAN_TURBULENCE_COUNT = "u_oceanTurbulenceCount";
        public static final String UNIFORM_OCEAN_TURBULENCE_STRENGTHS = "u_oceanTurbulenceStrengths";
        public static final String UNIFORM_OCEAN_WAVES = "u_oceanWaves";
        public static final String UNIFORM_OCEAN_WAVE_SCALE = "u_oceanWaveScale";

        // Structure Generation \\

        public static final int DEFAULT_STRUCTURE_SEPARATION_BLOCKS = 0;
        public static final int DEFAULT_STRUCTURE_Y_OFFSET_BLOCKS = 0;
        public static final long STRUCTURE_CHANCE_SALT = 0x3E9A71C45B0D82F6L;
        public static final long STRUCTURE_ID_HASH_MULTIPLIER = 0xD6E8FEB86659FD93L;
        public static final int STRUCTURE_MAX_BLOCK_COUNT = 262144;
        public static final int STRUCTURE_MAX_EXTENT_BLOCKS = 1024;
        public static final long STRUCTURE_OFFSET_X_SALT = 0x5C1F8B2A7E94D063L;
        public static final long STRUCTURE_OFFSET_Z_SALT = 0x8D47E0B3169AC52FL;
        public static final int STRUCTURE_ORIENTATION_SPIN_COUNT = 4;
        public static final long STRUCTURE_PLACEMENT_SEED = 0x1B7F3D95C28E46A0L;
        public static final int STRUCTURE_QUARTER_TURN_COUNT = 4;
        public static final long STRUCTURE_ROTATION_SALT = 0xA60C9E4F2D7B1853L;

        // Sky & Atmosphere \\

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

        // Weather \\

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
        public static final double METERS_PER_KILOMETER = 1000.0;
        public static final int WEATHER_CELL_RESOLVES_PER_FRAME = 32;
        public static final int WEATHER_CELL_SIZE_PIXELS = 2;
        public static final double WEATHER_FLOW_MEANDER_ANGLE_DEGREES = 18.0;
        public static final double WEATHER_FLOW_MEANDER_PERIOD_SECONDS = 5400.0;
        public static final double WEATHER_FLOW_MEANDER_SECONDARY_PERIOD_SECONDS = 1980.0;
        public static final double WEATHER_FLOW_MEANDER_SECONDARY_PHASE = 1.7;
        public static final double WEATHER_FLOW_MEANDER_SECONDARY_WEIGHT = 0.35;
        public static final double WEATHER_FLOW_SPEED_KPH = 40.0;
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

        // Cloud \\

        public static final float CLOUD_DETAIL_FREQUENCY_RATIO = 4.0f;
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

        // Wind \\

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
        public static final float WIND_GUST_SPEED_FREQUENCY_SECONDARY = 0.045f;
        public static final float WIND_MIN_SPEED_FLOOR = 0.05f;

        // Precipitation \\

        public static final String PRECIPITATION_DATA_UBO = "PrecipitationData";
        public static final int PRECIPITATION_HEIGHT_BITS = 16;
        public static final int PRECIPITATION_HEIGHT_MASK = 0xFFFF;
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

        // Temperature \\

        public static final float DEFAULT_BASE_TEMPERATURE = 15.0f;
        public static final long TEMPERATURE_DAILY_STREAM = 1L;
        public static final float TEMPERATURE_DAILY_VARIANCE_SCALE = 0.6f;
        public static final float TEMPERATURE_DIURNAL_PEAK_TIME = 0.65f;
        public static final float TEMPERATURE_DRIFT_FREQUENCY = 0.02f;
        public static final float TEMPERATURE_KELVIN_OFFSET = 273.15f;
        public static final float TEMPERATURE_PRECIPITATION_COOLING = 4.0f;

        // Season \\

        public static final float DEFAULT_SEASON_BASE_WIND_SPEED = 3.0f;
        public static final float DEFAULT_SEASON_PRECIPITATION_CHANCE_SCALE = 1.0f;
        public static final float DEFAULT_SEASON_PREVAILING_WIND_DIRECTION_DEGREES = 0.0f;
        public static final float DEFAULT_SEASON_TEMPERATURE_VARIANCE = 5.0f;
        public static final float DEFAULT_SEASON_WIND_VARIANCE = 1.0f;
        public static final float LATITUDE_DAYLENGTH_CURVE_POWER = 1.0f;
        public static final float LATITUDE_DAYLENGTH_REFERENCE_TILT_DEGREES = 23.5f;
        public static final float SEASON_BLEND_RECOMPUTE_EPSILON = 0.00001f;

        // Time & Clock \\

        public static final long CLOCK_DAILY_STREAM_NOISE = 0L;
        public static final long CLOCK_DAILY_STREAM_SALT = 0x9E3779B97F4A7C15L;
        public static final float CLOCK_NOISE_MIN = 0.001f;
        public static final double CLOCK_NOON = 0.5;
        public static final double CLOCK_QUARTER = 0.25;
        public static final double CLOCK_SUNRISE_MAX = 0.40;
        public static final double CLOCK_SUNRISE_MIN = 0.05;
        public static final double CLOCK_SUNSET_MAX = 0.95;
        public static final double CLOCK_SUNSET_MIN = 0.60;
        public static final double CLOCK_THREE_QUARTERS = 0.75;
        public static final long MILLIS_PER_REAL_DAY = 86400000L;
        public static final double MILLIS_PER_SECOND = 1000.0;

        // Physics & Movement \\

        public static final float FIXED_TIME_STEP = 0.02f;
        public static final float GRAVITY_FORCE = 9.8f;
        public static final float GROUNDED_FALL_SPEED = 5.0f;
        public static final float JUMP_HOLD_FRACTION = 0.4f;
        public static final float JUMP_SCALE = 1.8f;
        public static final float MOVEMENT_ACCELERATION = 8.0f;
        public static final float MOVEMENT_SCALE = 1.5f;
        public static final float REACH_SCALE = 4.0f;

        // Entity, Rig & Player \\

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

        // Animation \\

        public static final float ANIMATION_BLEND_SECONDS = 0.22f;
        public static final float ANIMATION_RATE_SCALE_MAX = 1.5f;
        public static final float ANIMATION_RATE_SCALE_MIN = 0.35f;
        public static final float DEFAULT_ANIMATION_LAYER_WEIGHT = 1f;
        public static final float DEFAULT_ANIMATION_NODE_DELAY = 0f;
        public static final float DEFAULT_ANIMATION_NODE_RATE = 1f;

        // Appearance \\

        public static final float DEFAULT_BUILD_FACTOR = 1f;
        public static final float DEFAULT_WEIGHT_RATIO = 0.5f;

        // Item \\

        public static final String DEFAULT_ITEM_MATERIAL = "items/StandardItemMaterial";
        public static final short TOOL_NONE = 0;

        // Sub-Voxel Model \\

        public static final int SUB_VOXEL_CELL_COUNT = SUB_VOXEL_RESOLUTION * SUB_VOXEL_RESOLUTION
                        * SUB_VOXEL_RESOLUTION;
        public static final int SUB_VOXEL_EMPTY_CELL = 0;
        public static final int SUB_VOXEL_FACE_COUNT = 6;
        public static final float SUB_VOXEL_IMPORT_EPSILON = 1e-6f;
        public static final float SUB_VOXEL_IMPORT_RAY_Y = 0.0137f;
        public static final float SUB_VOXEL_IMPORT_RAY_Z = 0.0071f;
        public static final int SUB_VOXEL_MAX_PARTS = 255;
        public static final String SUB_VOXEL_VAO = "util/vao/ItemVAO";
        public static final int SUB_VOXEL_VERTEX_STRIDE = 6;

        // Font \\

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

        // Menu & UI \\

        public static final float DIVIDER_HIT_TOLERANCE = 6f;
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
        public static final float MENU_ANIMATION_MAX_STEP_SECONDS = 0.1f;
        public static final float MENU_EASE_BACK_OVERSHOOT = 1.70158f;
        public static final String MENU_HIERARCHY = "util/Hierarchy/Hierarchy";
        public static final float MENU_SCROLL_PIXELS = 48f;
        public static final String MENU_TAB_GHOST = "editor/TabFrame/TabGhost";
        public static final String MENU_TAB_SHELL = "editor/TabFrame/TabFrame";
        public static final float MENU_THEME_ALPHA_DEFAULT = 1.0f;
        public static final float RATIO_DEFAULT = 0.5f;
        public static final float RATIO_MAX = 0.9f;
        public static final float RATIO_MIN = 0.1f;
        public static final float TAB_DRAG_EDGE_FRACTION = 0.25f;
        public static final int TAB_DRAG_PREVIEW_H = 144;
        public static final int TAB_DRAG_PREVIEW_W = 256;
        public static final int TAB_ENTRY_TITLE = 0;
        public static final String TAB_TITLE_DEV = "Dev Mode";
        public static final String TAB_TITLE_PREVIEW = "Preview";
        public static final String WINDOW_TITLE_EDITOR_SECONDARY = "Secondary";

        // Screen Capture & Recording \\

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
