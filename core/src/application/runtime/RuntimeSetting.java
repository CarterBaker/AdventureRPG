package application.runtime;

import engine.graphics.color.Color;
import engine.util.mathematics.vectors.Vector4;

public class RuntimeSetting {

    /*
     * Compile-time constants for the runtime layer. FBO target keys mirror
     * EngineSetting so runtime code has a single import. Composite layers
     * control blit order — lower renders first, higher composites on top.
     * The character creator's, settings menu's, and inventory's menus, text,
     * ranges, and colors live here too.
     */

    // Full Screen Passes
    public static final String PASS_SKY = "Sky";
    public static final String PASS_SSAO = "SSAO";
    public static final String PASS_LIGHTING = "Lighting";
    public static final String PASS_WEATHER = "Weather";
    public static final String PASS_PRECIPITATION = "Precipitation";
    public static final String PASS_UNDERWATER = "Underwater";
    public static final String PASS_SQUINT = "Squint";
    public static final String PASS_POST_PREFILTER = "PostPrefilter";
    public static final String PASS_POST_BLUR = "PostBlur";
    public static final String PASS_POST_COMPOSITE = "PostComposite";

    // FBO Targets
    public static final String FBO_SKY = "SkyScene";
    public static final String FBO_WORLD = "MainScene";
    public static final String FBO_LIT = "LitScene";
    public static final String FBO_SSAO = "SSAOScene";
    public static final String FBO_UI = "UIScene";
    public static final String FBO_WEATHER = "WeatherScene";
    public static final String FBO_PRECIPITATION = "PrecipitationScene";
    public static final String FBO_INVENTORY = "InventoryScene";
    public static final String FBO_WATER = "WaterScene";
    public static final String FBO_SQUINT = "SquintScene";
    public static final String FBO_POST_SCENE = "PostScene";
    public static final String FBO_POST_PREFILTER = "PostPrefilterScene";
    public static final String FBO_POST_BLUR = "PostBlurScene";
    public static final String FBO_POST_COMPOSITE = "PostCompositeScene";

    // Composite Layers
    public static final int LAYER_SKY = -10;
    public static final int LAYER_WEATHER = -5;
    public static final int LAYER_WORLD = 0;
    public static final int LAYER_WATER = 1;
    public static final int LAYER_PRECIPITATION = 5;
    public static final int LAYER_SQUINT = 7;
    public static final int LAYER_UI = 10;
    public static final int LAYER_INVENTORY = 15;

    // Pass Uniforms
    public static final String UNIFORM_SCENE_DEPTH = "u_sceneDepth";
    public static final String UNIFORM_WATER_DEPTH = "u_waterDepth";
    public static final int PASS_DRAW_DEPTH = 0;
    public static final int UNDERWATER_DRAW_DEPTH = -1;

    // Sky Bodies
    public static final String SPRITE_SKY_SUN = "sky/Sun";
    public static final String SPRITE_SKY_SUN_FLAMES = "sky/SunFlames";
    public static final String SPRITE_SKY_MOON = "sky/Moon";
    public static final String SPRITE_SKY_STAR = "sky/Star";
    public static final String UNIFORM_SKY_SUN_TEXTURE = "u_sunTexture";
    public static final String UNIFORM_SKY_SUN_FLAME_TEXTURE = "u_sunFlameTexture";
    public static final String UNIFORM_SKY_MOON_TEXTURE = "u_moonTexture";
    public static final String UNIFORM_SKY_STAR_TEXTURE = "u_starTexture";

    // Squint
    public static final String UNIFORM_SQUINT = "u_squint";
    public static final float SQUINT_LOOK_INNER_DEGREES = 6f;
    public static final float SQUINT_LOOK_OUTER_DEGREES = 28f;
    public static final float SQUINT_SUN_ELEVATION_START = 0f;
    public static final float SQUINT_SUN_ELEVATION_END = 0.25f;
    public static final float SQUINT_OVERCAST_RELIEF = 0.9f;
    public static final float SQUINT_OCCLUSION_DISTANCE = 96f;
    public static final float SQUINT_CLOSE_RATE = 6f;
    public static final float SQUINT_OPEN_RATE = 2.5f;
    public static final float SQUINT_VISIBLE_THRESHOLD = 0.005f;

    // Deferred Attachments
    public static final String ATTACHMENT_ALBEDO = "albedo";
    public static final String ATTACHMENT_NORMAL = "normal";
    public static final String ATTACHMENT_MATERIAL = "material";
    public static final String ATTACHMENT_AO = "ao";
    public static final String ATTACHMENT_COLOR = "color";
    public static final String ATTACHMENT_DISTANCE = "distance";

    // Deferred Uniforms
    public static final String UNIFORM_G_ALBEDO = "u_gAlbedo";
    public static final String UNIFORM_G_NORMAL = "u_gNormal";
    public static final String UNIFORM_G_MATERIAL = "u_gMaterial";
    public static final String UNIFORM_G_DEPTH = "u_gDepth";
    public static final String UNIFORM_SSAO_TEXTURE = "u_ssaoTex";
    public static final String UNIFORM_CLOUD_COLOR = "u_cloudColor";
    public static final String UNIFORM_CLOUD_DISTANCE = "u_cloudDistance";

    // SSAO
    public static final String SSAO_DATA_UBO = "SSAOData";
    public static final String UNIFORM_SSAO_NOISE = "u_texNoise";
    public static final String UNIFORM_SSAO_SAMPLES = "u_samples";
    public static final String UNIFORM_SSAO_KERNEL_SIZE = "u_kernelSize";
    public static final String UNIFORM_SSAO_RADIUS = "u_radius";
    public static final String UNIFORM_SSAO_BIAS = "u_bias";
    public static final int SSAO_KERNEL_SIZE = 64;
    public static final float SSAO_KERNEL_MIN_SCALE = 0.1f;
    public static final float SSAO_RADIUS = 0.3f;
    public static final float SSAO_BIAS = 0.025f;
    public static final int SSAO_NOISE_SIZE = 4;
    public static final int SSAO_NOISE_CHANNELS = 3;

    // Post Effects
    public static final String ATTACHMENT_FOCUS = "focus";
    public static final String ATTACHMENT_BLOOM = "bloom";
    public static final String ATTACHMENT_DEPTH_OF_FIELD = "depthOfField";
    public static final String UNIFORM_SCENE_COLOR = "u_sceneColor";
    public static final String UNIFORM_FOCUS_INVERSE = "u_focusInverse";
    public static final String UNIFORM_PREFILTER_FOCUS = "u_prefilterFocus";
    public static final String UNIFORM_PREFILTER_BLOOM = "u_prefilterBloom";
    public static final String UNIFORM_BLUR_DEPTH_OF_FIELD = "u_blurDepthOfField";
    public static final String UNIFORM_BLUR_BLOOM = "u_blurBloom";
    public static final String UNIFORM_GRAIN_TIME = "u_grainTime";
    public static final float GRAIN_TIME_PERIOD = 60f;

    // Auto Focus
    public static final float FOCUS_REACH = 256f;
    public static final float FOCUS_MIN_DISTANCE = 0.25f;
    public static final float FOCUS_INFINITY_INVERSE = 0f;
    public static final float FOCUS_RATE = 6f;
    public static final float FOCUS_SUBJECT_CENTER = 0.5f;

    // Load Menu
    public static final String MENU_LOAD = "MainMenu/Load";
    public static final String MENU_LOAD_CHARACTER_SLOT = "MainMenu/character_slot";
    public static final String MENU_LOAD_EMPTY_NOTICE = "MainMenu/character_list_empty";
    public static final String ELEMENT_CHARACTER_SLOT_LABEL = "character_label";
    public static final int ENTRY_CHARACTER_LIST = 0;

    // Pause Menu
    public static final String MENU_PAUSE = "PauseMenu/Pause";

    // HUD
    public static final String MENU_HUD_CROSSHAIR = "HUD/Crosshair";
    public static final String MENU_HUD_ACTIVATE = "HUD/Activate";
    public static final int ENTRY_HUD_ACTIVATE_KEY = 0;

    // Character Creator Menus
    public static final String MENU_CREATOR = "CharacterCreator/Creator";
    public static final String MENU_CREATOR_TAB = "CharacterCreator/creator_tab";
    public static final String MENU_CREATOR_SECTION_HEADER = "CharacterCreator/section_header";
    public static final String MENU_CREATOR_FEATURE_ROW = "CharacterCreator/feature_row";
    public static final String MENU_CREATOR_SWATCH_ROW = "CharacterCreator/swatch_row";
    public static final String MENU_CREATOR_SWATCH = "CharacterCreator/swatch";
    public static final String MENU_CREATOR_SLIDER_ROW = "CharacterCreator/slider_row";
    public static final String MENU_CREATOR_PLACEHOLDER = "CharacterCreator/placeholder_note";
    public static final String MENU_CREATOR_STAT_ROW = "CharacterCreator/stat_row";

    // Character Creator Entry Points
    public static final int ENTRY_CREATOR_TABS = 0;
    public static final int ENTRY_CREATOR_OPTIONS = 1;
    public static final int ENTRY_CREATOR_STATS = 2;
    public static final int ENTRY_CREATOR_NAME = 3;
    public static final int ENTRY_CREATOR_STATUS = 4;

    // Character Creator Elements
    public static final String ELEMENT_CREATOR_TAB_LABEL = "tab_label";
    public static final String ELEMENT_CREATOR_ROW_LABEL = "row_label";
    public static final String ELEMENT_CREATOR_ROW_VALUE = "row_value";
    public static final String ELEMENT_CREATOR_ROW_PREVIOUS = "row_previous";
    public static final String ELEMENT_CREATOR_ROW_NEXT = "row_next";
    public static final String ELEMENT_CREATOR_SWATCH_RING = "swatch_ring";
    public static final String ELEMENT_CREATOR_SLIDER_TRACK = "slider_track";
    public static final String ELEMENT_CREATOR_SLIDER_KNOB = "slider_knob";
    public static final String ELEMENT_CREATOR_SLIDER_LABEL = "slider_label";
    public static final String ELEMENT_CREATOR_SLIDER_VALUE = "slider_value";
    public static final String ELEMENT_CREATOR_PLACEHOLDER_TITLE = "placeholder_title";
    public static final String ELEMENT_CREATOR_PLACEHOLDER_TEXT = "placeholder_text";
    public static final String ELEMENT_CREATOR_STAT_NAME = "stat_name";
    public static final String ELEMENT_CREATOR_STAT_VALUE = "stat_value";

    // Character Creator Tabs
    public static final String CREATOR_TAB_APPEARANCE = "Appearance";
    public static final String CREATOR_TAB_HAIR = "Hair";
    public static final String CREATOR_TAB_BODY = "Body";
    public static final String CREATOR_TAB_CLASS = "Class";
    public static final String CREATOR_TAB_PROFESSION = "Profession";
    public static final String CREATOR_TAB_SKILLS = "Skills";
    public static final String CREATOR_PLACEHOLDER_CLASS = "Your calling will be chosen here.";
    public static final String CREATOR_PLACEHOLDER_PROFESSION = "Your trade will be chosen here.";
    public static final String CREATOR_PLACEHOLDER_SKILLS = "Your starting skills will be chosen here.";

    // Character Creator Sections
    public static final String CREATOR_SECTION_FEATURES = "Features";
    public static final String CREATOR_SECTION_SKIN = "Skin Tone";
    public static final String CREATOR_SECTION_HAIRSTYLE = "Hairstyle";
    public static final String CREATOR_SECTION_HAIR_COLOR = "Hair Color";
    public static final String CREATOR_SECTION_BUILD = "Build";
    public static final String CREATOR_SECTION_HEAD_SHAPE = "Head Shape";
    public static final String CREATOR_FEATURE_NONE = "None";
    public static final String CREATOR_FEATURE_PATH_SEPARATOR = "/";
    public static final String CREATOR_WORD_BOUNDARY_PATTERN = "(?<=[a-z])(?=[A-Z])";
    public static final String CREATOR_WORD_SEPARATOR = " ";
    public static final int CREATOR_SWATCHES_PER_ROW = 6;
    public static final String CREATOR_ARGUMENT_SEPARATOR = ":";
    public static final float CREATOR_COLOR_MATCH_EPSILON = 0.002f;

    // Character Creator Sliders
    public static final String CREATOR_SLIDER_HEIGHT = "Height";
    public static final String CREATOR_SLIDER_BUILD = "Weight";
    public static final String CREATOR_SLIDER_HEAD_WIDTH = "Width";
    public static final String CREATOR_SLIDER_HEAD_HEIGHT = "Length";
    public static final String CREATOR_SLIDER_HEAD_DEPTH = "Depth";
    public static final String CREATOR_FORMAT_HEIGHT = "%.2f m";
    public static final String CREATOR_FORMAT_WEIGHT = "%.0f kg";
    public static final String CREATOR_FORMAT_PERCENT = "%.0f%%";
    public static final float CREATOR_HEAD_PROPORTION_MIN = 0.85f;
    public static final float CREATOR_HEAD_PROPORTION_MAX = 1.15f;
    public static final float CREATOR_PERCENT_SCALE = 100f;

    // Character Creator Name
    public static final int CREATOR_NAME_MAX_LENGTH = 24;
    public static final String CREATOR_NAME_CARET = "|";
    public static final float CREATOR_CARET_BLINK_SECONDS = 0.5f;
    public static final String CREATOR_STATUS_NAME_EMPTY = "Give your character a name";
    public static final String CREATOR_STATUS_NAME_TAKEN = "That name is already taken";

    // Character Creator Preview
    public static final float CREATOR_ROTATE_DEGREES_PER_PIXEL = 0.6f;

    // Character Creator Colors
    public static final Color CREATOR_TAB_ACTIVE_COLOR = new Color(1f, 1f, 1f, 1f);
    public static final Color CREATOR_TAB_ACTIVE_LABEL_COLOR = new Color(0.463f, 0.086f, 0.071f, 1f);
    public static final Color CREATOR_SWATCH_SELECTED_COLOR = new Color(1f, 0.84f, 0.42f, 1f);
    public static final Color CREATOR_SWATCH_UNSELECTED_COLOR = new Color(1f, 1f, 1f, 0f);

    // Character Creator Stats
    public static final String[] CREATOR_STAT_NAMES = {
            "Strength", "Dexterity", "Constitution", "Intelligence", "Wisdom", "Charisma" };
    public static final String CREATOR_STAT_PLACEHOLDER_VALUE = "10";

    // Settings Menus
    public static final String MENU_SETTINGS = "Settings/Settings";
    public static final String MENU_SETTINGS_TAB = "Settings/settings_tab";
    public static final String MENU_SETTINGS_SECTION_HEADER = "Settings/section_header";
    public static final String MENU_SETTINGS_OPTION_ROW = "Settings/option_row";
    public static final String MENU_SETTINGS_SLIDER_ROW = "Settings/slider_row";
    public static final String MENU_SETTINGS_BINDING_ROW = "Settings/binding_row";
    public static final String MENU_SETTINGS_ACTION_ROW = "Settings/action_row";
    public static final String MENU_SETTINGS_NOTE = "Settings/settings_note";
    public static final String MENU_SETTINGS_PLACEHOLDER = "Settings/placeholder_note";

    // Settings Entry Points
    public static final int ENTRY_SETTINGS_TABS = 0;
    public static final int ENTRY_SETTINGS_OPTIONS = 1;

    // Settings Elements
    public static final String ELEMENT_SETTINGS_TAB_LABEL = "tab_label";
    public static final String ELEMENT_SETTINGS_ROW_LABEL = "row_label";
    public static final String ELEMENT_SETTINGS_ROW_VALUE = "row_value";
    public static final String ELEMENT_SETTINGS_ROW_PREVIOUS = "row_previous";
    public static final String ELEMENT_SETTINGS_ROW_NEXT = "row_next";
    public static final String ELEMENT_SETTINGS_SLIDER_TRACK = "slider_track";
    public static final String ELEMENT_SETTINGS_SLIDER_KNOB = "slider_knob";
    public static final String ELEMENT_SETTINGS_SLIDER_LABEL = "slider_label";
    public static final String ELEMENT_SETTINGS_SLIDER_VALUE = "slider_value";
    public static final String ELEMENT_SETTINGS_BINDING_LABEL = "binding_label";
    public static final String ELEMENT_SETTINGS_BINDING_VALUE = "binding_value";
    public static final String ELEMENT_SETTINGS_ACTION_LABEL = "action_label";
    public static final String ELEMENT_SETTINGS_PLACEHOLDER_TITLE = "placeholder_title";
    public static final String ELEMENT_SETTINGS_PLACEHOLDER_SUBTITLE = "placeholder_subtitle";
    public static final String ELEMENT_SETTINGS_PLACEHOLDER_TEXT = "placeholder_text";

    // Settings Tabs
    public static final String SETTINGS_TAB_DISPLAY = "Display";
    public static final String SETTINGS_TAB_GRAPHICS = "Graphics";
    public static final String SETTINGS_TAB_CONTROLS = "Controls";
    public static final String SETTINGS_TAB_AUDIO = "Audio";
    public static final String SETTINGS_TAB_GAMEPLAY = "Gameplay";
    public static final String SETTINGS_TAB_ACCESSIBILITY = "Accessibility";
    public static final String SETTINGS_PLACEHOLDER_AUDIO = "Volume and sound options will be set here.";
    public static final String SETTINGS_PLACEHOLDER_GAMEPLAY = "Difficulty and interface options will be set here.";
    public static final String SETTINGS_PLACEHOLDER_ACCESSIBILITY = "Text size and color options will be set here.";
    public static final String SETTINGS_PLACEHOLDER_COMING_SOON = "Coming soon";
    public static final String SETTINGS_PLACEHOLDER_GAME_WINDOW_ONLY = "Game window only";
    public static final String SETTINGS_PLACEHOLDER_DISPLAY_UNAVAILABLE = "Display settings change in the game window.";

    // Settings Sections
    public static final String SETTINGS_SECTION_WINDOW = "Window";
    public static final String SETTINGS_SECTION_VIEW = "View";
    public static final String SETTINGS_SECTION_WORLD = "World";
    public static final String SETTINGS_SECTION_MOUSE = "Mouse";
    public static final String SETTINGS_SECTION_MOVEMENT = "Movement";
    public static final String SETTINGS_SECTION_ACTIONS = "Actions";
    public static final String SETTINGS_SECTION_CAPTURE = "Capture";
    public static final String SETTINGS_SECTION_OUTLINES = "Outlines";
    public static final String SETTINGS_SECTION_DEPTH_OF_FIELD = "Depth of Field";
    public static final String SETTINGS_SECTION_IMAGE = "Image";
    public static final String SETTINGS_SECTION_COLOR = "Color";
    public static final String SETTINGS_SECTION_LENS = "Lens";
    public static final String SETTINGS_NOTE_WORLD_APPLY = "World changes apply when you leave Settings";
    public static final String SETTINGS_NOTE_AUTO_FOCUS = "Focus follows whatever you look at";

    // Settings Options
    public static final String SETTINGS_OPTION_FULLSCREEN = "Fullscreen";
    public static final String SETTINGS_OPTION_VSYNC = "VSync";
    public static final String SETTINGS_OPTION_FIELD_OF_VIEW = "Field of View";
    public static final String SETTINGS_OPTION_RENDER_DISTANCE = "Distance";
    public static final String SETTINGS_OPTION_TERRAIN_DETAIL = "Detail";
    public static final String SETTINGS_OPTION_OUTLINES = "Outlines";
    public static final String SETTINGS_OPTION_OUTLINE_THICKNESS = "Thickness";
    public static final String SETTINGS_OPTION_ANTI_ALIASING = "Anti-Aliasing";
    public static final String SETTINGS_OPTION_DEPTH_OF_FIELD = "Depth of Field";
    public static final String SETTINGS_OPTION_BLOOM = "Bloom";
    public static final String SETTINGS_SLIDER_MOUSE_SENSITIVITY = "Sensitivity";
    public static final String SETTINGS_SLIDER_OUTLINE_STRENGTH = "Strength";
    public static final String SETTINGS_SLIDER_DEPTH_OF_FIELD_STRENGTH = "Strength";
    public static final String SETTINGS_SLIDER_DEPTH_OF_FIELD_BLUR = "Blur Size";
    public static final String SETTINGS_SLIDER_BLOOM_INTENSITY = "Bloom Intensity";
    public static final String SETTINGS_SLIDER_BRIGHTNESS = "Brightness";
    public static final String SETTINGS_SLIDER_CONTRAST = "Contrast";
    public static final String SETTINGS_SLIDER_SATURATION = "Saturation";
    public static final String SETTINGS_SLIDER_VIGNETTE = "Vignette";
    public static final String SETTINGS_SLIDER_CHROMATIC_ABERRATION = "Chromatic Aberration";
    public static final String SETTINGS_SLIDER_FILM_GRAIN = "Film Grain";
    public static final String SETTINGS_VALUE_ON = "On";
    public static final String SETTINGS_VALUE_OFF = "Off";
    public static final String SETTINGS_FORMAT_BLOCKS = "%d blocks";
    public static final String SETTINGS_FORMAT_CHUNKS = "%d chunks";
    public static final String SETTINGS_FORMAT_FIELD_OF_VIEW = "%.0f";
    public static final String SETTINGS_FORMAT_SENSITIVITY = "%.2f";
    public static final String SETTINGS_FORMAT_PIXELS = "%d px";
    public static final String SETTINGS_FORMAT_PERCENT = "%.0f%%";
    public static final int SETTINGS_OUTLINE_THICKNESS_STEP = 1;
    public static final int SETTINGS_RENDER_DISTANCE_STEP = 8;
    public static final int SETTINGS_RENDER_DISTANCE_PER_RADIUS = 2;
    public static final int SETTINGS_TERRAIN_DETAIL_STEP = 8;
    public static final float SETTINGS_FIELD_OF_VIEW_STEP = 5f;
    public static final float SETTINGS_SLIDER_PERCENT_SCALE = 100f;

    // Settings Bindings
    public static final String SETTINGS_BINDING_MOVE_FORWARD = "Move Forward";
    public static final String SETTINGS_BINDING_MOVE_BACK = "Move Back";
    public static final String SETTINGS_BINDING_MOVE_LEFT = "Move Left";
    public static final String SETTINGS_BINDING_MOVE_RIGHT = "Move Right";
    public static final String SETTINGS_BINDING_JUMP = "Jump";
    public static final String SETTINGS_BINDING_WALK = "Walk";
    public static final String SETTINGS_BINDING_SPRINT = "Sprint / Aim";
    public static final String SETTINGS_BINDING_ACTIVATE = "Activate / Place";
    public static final String SETTINGS_BINDING_BLOCK = "Block";
    public static final String SETTINGS_BINDING_INVENTORY = "Inventory";
    public static final String SETTINGS_BINDING_ROTATE_ITEM = "Rotate Item";
    public static final String SETTINGS_BINDING_SCREENSHOT = "Screenshot";
    public static final String SETTINGS_BINDING_RECORD_VIDEO = "Record Video";
    public static final String SETTINGS_BINDING_CAPTURE_PROMPT = "Press a key";
    public static final String SETTINGS_ACTION_RESET_BINDINGS = "Reset Controls";

    // Settings Colors
    public static final Color SETTINGS_TAB_ACTIVE_COLOR = new Color(1f, 1f, 1f, 1f);
    public static final Color SETTINGS_TAB_ACTIVE_LABEL_COLOR = new Color(0.463f, 0.086f, 0.071f, 1f);
    public static final Color SETTINGS_BINDING_CAPTURE_COLOR = new Color(1f, 0.84f, 0.42f, 1f);

    // Inventory Menus
    public static final String MENU_INVENTORY_SCENE = "Inventory/Scene";
    public static final String MENU_INVENTORY_EQUIPMENT = "Inventory/Equipment";
    public static final String MENU_INVENTORY_BAG = "Inventory/Bag";
    public static final String MENU_INVENTORY_BAG_LIST = "Inventory/BagList";
    public static final String MENU_INVENTORY_CHEST = "Inventory/Chest";
    public static final String MENU_INVENTORY_CHEST_LIST = "Inventory/ChestList";
    public static final String MENU_INVENTORY_EQUIPMENT_SLOT = "Inventory/equipment_slot";
    public static final String MENU_INVENTORY_RING_SLOT = "Inventory/ring_slot";
    public static final String MENU_INVENTORY_LIST_HEADER = "Inventory/list_header";
    public static final String MENU_INVENTORY_LIST_ROW = "Inventory/list_row";
    public static final String MENU_INVENTORY_LIST_EMPTY = "Inventory/list_empty";
    public static final String MENU_INVENTORY_STAT_HEADER = "Inventory/stat_header";
    public static final String MENU_INVENTORY_STAT_ROW = "Inventory/stat_row";
    public static final String MENU_INVENTORY_DETAIL_TITLE = "Inventory/detail_title";
    public static final String MENU_INVENTORY_DETAIL_LINE = "Inventory/detail_line";

    // Inventory Entry Points — Equipment
    public static final int ENTRY_INVENTORY_BACKPACK_SLOT = 0;
    public static final int ENTRY_INVENTORY_SLOT_COLUMN_A = 1;
    public static final int ENTRY_INVENTORY_SLOT_COLUMN_B = 2;
    public static final int ENTRY_INVENTORY_RING_ROW_A = 3;
    public static final int ENTRY_INVENTORY_STATS = 4;
    public static final int ENTRY_INVENTORY_DETAILS = 5;
    public static final int ENTRY_INVENTORY_PREVIEW = 6;
    public static final int ENTRY_INVENTORY_CLOSE_HINT = 7;
    public static final int ENTRY_INVENTORY_RING_ROW_B = 8;

    // Inventory Entry Points — Container panels, shared by the bag's and the chest's
    public static final int ENTRY_CONTAINER_TITLE = 0;
    public static final int ENTRY_CONTAINER_WEIGHT = 1;
    public static final int ENTRY_CONTAINER_TOGGLE_LABEL = 2;
    public static final int ENTRY_CONTAINER_HINT = 3;
    public static final int ENTRY_CONTAINER_AREA = 4;

    // Inventory Entry Points — Lists, shared by the bag's and the chest's
    public static final int ENTRY_LIST_TITLE = 0;
    public static final int ENTRY_LIST_WEIGHT = 1;
    public static final int ENTRY_LIST_ROWS = 2;
    public static final int ENTRY_LIST_TOGGLE_LABEL = 3;

    // Inventory Elements
    public static final String ELEMENT_INVENTORY_SLOT_LABEL = "slot_label";
    public static final String ELEMENT_INVENTORY_SLOT_EYE = "slot_eye";
    public static final String ELEMENT_INVENTORY_EYE_OPEN = "eye_open";
    public static final String ELEMENT_INVENTORY_EYE_CLOSED = "eye_closed";
    public static final String ELEMENT_INVENTORY_ROW_NAME = "row_name";
    public static final String ELEMENT_INVENTORY_ROW_WEIGHT = "row_weight";
    public static final String ELEMENT_INVENTORY_STAT_NAME = "stat_name";
    public static final String ELEMENT_INVENTORY_STAT_VALUE = "stat_value";

    // Inventory Slot Titles
    public static final String INVENTORY_SLOT_BACKPACK = "Pack";
    public static final String INVENTORY_SLOT_HEAD = "Head";
    public static final String INVENTORY_SLOT_CLOAK = "Cloak";
    public static final String INVENTORY_SLOT_CHEST = "Chest";
    public static final String INVENTORY_SLOT_SHIRT = "Shirt";
    public static final String INVENTORY_SLOT_BELT = "Belt";
    public static final String INVENTORY_SLOT_PANTS = "Pants";
    public static final String INVENTORY_SLOT_RIGHT_SHOULDER = "R. Shoulder";
    public static final String INVENTORY_SLOT_LEFT_SHOULDER = "L. Shoulder";
    public static final String INVENTORY_SLOT_RIGHT_ARM = "R. Arm";
    public static final String INVENTORY_SLOT_LEFT_ARM = "L. Arm";
    public static final String INVENTORY_SLOT_RIGHT_GLOVE = "R. Glove";
    public static final String INVENTORY_SLOT_LEFT_GLOVE = "L. Glove";
    public static final String INVENTORY_SLOT_RIGHT_LEG = "R. Leg";
    public static final String INVENTORY_SLOT_LEFT_LEG = "L. Leg";
    public static final String INVENTORY_SLOT_RIGHT_FOOT = "R. Boot";
    public static final String INVENTORY_SLOT_LEFT_FOOT = "L. Boot";
    public static final String INVENTORY_SLOT_MAIN_HAND = "Main Hand";
    public static final String INVENTORY_SLOT_OFF_HAND = "Off Hand";
    public static final String INVENTORY_SLOT_RING = "Ring";

    // Inventory Text
    public static final String INVENTORY_ARGUMENT_SEPARATOR = ":";
    public static final String INVENTORY_FORMAT_WEIGHT = "%.1f kg";
    public static final String INVENTORY_FORMAT_STACK_NAME = "%s  x%d";
    public static final String INVENTORY_FORMAT_HOLDING = "Holding %.1f kg";
    public static final String INVENTORY_FORMAT_LOAD = "%.1f / %.0f kg";
    public static final String INVENTORY_FORMAT_STAT = "%.0f";
    public static final String INVENTORY_FORMAT_STAT_BONUS = "%.0f (%+.0f)";
    public static final String INVENTORY_FORMAT_ITEM_STAT = "%s %+.0f";
    public static final String INVENTORY_FORMAT_ITEM_KIND = "%s  -  %s";
    public static final String INVENTORY_FORMAT_ITEM_SPACE = "Holds %d x %d x %d";
    public static final String INVENTORY_FORMAT_ITEM_SIZE = "Size %d x %d x %d";
    public static final String INVENTORY_FORMAT_ITEM_WEIGHT = "Weight %.1f kg";
    public static final String INVENTORY_FORMAT_ITEM_STACK = "Stack %d / %d";
    public static final String INVENTORY_FORMAT_ITEM_TOOL = "%s  -  tier %d";
    public static final String INVENTORY_TEXT_NONE = "";
    public static final String INVENTORY_FORMAT_CLOSE_HINT = "%s or %s to close";
    public static final String INVENTORY_FORMAT_CHEST_HINT = "Drag beside it to tilt and turn it  -  scroll to zoom"
            + "  -  %s, %s or %s to close";
    public static final String INVENTORY_FORMAT_VIEW_HINT = "Drag items to move them  -  %s turns a held item"
            + "  -  drag beside the bag to tilt and turn it  -  scroll to zoom";
    public static final String INVENTORY_TEXT_TWO_HANDED = "Two-handed";
    public static final String INVENTORY_TEXT_NO_SELECTION = "Point at an item to see it.";
    public static final String INVENTORY_TEXT_SHOW_LIST = "Show List";
    public static final String INVENTORY_TEXT_HIDE_LIST = "Hide List";
    public static final String INVENTORY_STAT_SECTION_ATTRIBUTES = "Attributes";
    public static final String INVENTORY_STAT_SECTION_COMBAT = "Combat";
    public static final String INVENTORY_STAT_SECTION_LOAD = "Load";
    public static final String INVENTORY_STAT_CARRIED = "Carried";
    public static final String INVENTORY_STAT_ITEMS_WORN = "Items Worn";

    // Inventory View
    public static final float INVENTORY_VIEW_DEFAULT_YAW_DEGREES = 0f;
    public static final float INVENTORY_VIEW_TURN_DEGREES_PER_PIXEL = 0.5f;
    public static final float INVENTORY_VIEW_PITCH_DEFAULT_DEGREES = 62f;
    public static final float INVENTORY_VIEW_PITCH_MIN_DEGREES = 30f;
    public static final float INVENTORY_VIEW_PITCH_MAX_DEGREES = 89f;
    public static final float INVENTORY_VIEW_FOV_DEGREES = 30f;
    public static final float INVENTORY_VIEW_FILL = 1.15f;
    public static final float INVENTORY_VIEW_DEPTH_MARGIN = 1.5f;
    public static final float INVENTORY_VIEW_NEAR_MIN = 0.01f;
    public static final float INVENTORY_VIEW_ZOOM_MIN = 1f;
    public static final float INVENTORY_VIEW_ZOOM_MAX = 2f;
    public static final float INVENTORY_VIEW_ZOOM_STEP = 1.15f;
    public static final float INVENTORY_ICON_FILL = 0.74f;
    public static final float INVENTORY_ICON_PITCH_DEGREES = 22f;
    public static final float INVENTORY_ICON_YAW_DEGREES = -34f;
    public static final float INVENTORY_HELD_ICON_SIZE = 72f;
    public static final float INVENTORY_DEPTH_RANGE = 8192f;
    public static final float INVENTORY_PREVIEW_FILL = 0.84f;
    public static final float INVENTORY_ROTATE_DEGREES_PER_PIXEL = 0.6f;
    public static final float INVENTORY_PREVIEW_ZOOM_MIN = 1f;
    public static final float INVENTORY_PREVIEW_ZOOM_MAX = 3.5f;
    public static final float INVENTORY_PREVIEW_ZOOM_STEP = 1.15f;
    public static final float INVENTORY_PREVIEW_PAN_PER_PIXEL = 0.0025f;
    public static final int INVENTORY_DRAW_DEPTH = 0;
    public static final int INVENTORY_RING_ROW_LENGTH = 5;

    // Inventory Render
    public static final String MATERIAL_INVENTORY_ITEM = "items/InventoryItemMaterial";
    public static final String UNIFORM_INVENTORY_PROJECTION = "u_projection";
    public static final String UNIFORM_INVENTORY_MODEL = "u_model";
    public static final String UNIFORM_INVENTORY_TINT = "u_tint";

    // Inventory Colors
    public static final Color INVENTORY_EYE_SHOWN_COLOR = new Color(1f, 1f, 1f, 1f);
    public static final Color INVENTORY_EYE_HIDDEN_COLOR = new Color(1f, 1f, 1f, 0f);
    public static final Vector4 INVENTORY_TINT_NONE = new Vector4(1f, 1f, 1f, 0f);
    public static final Vector4 INVENTORY_TINT_HIDDEN = new Vector4(0.35f, 0.35f, 0.35f, 0.55f);
    public static final Vector4 INVENTORY_TINT_VALID = new Vector4(0.45f, 1f, 0.5f, 0.45f);
    public static final Vector4 INVENTORY_TINT_INVALID = new Vector4(1f, 0.35f, 0.3f, 0.55f);
}
