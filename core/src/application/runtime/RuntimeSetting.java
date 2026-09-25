package application.runtime;

import engine.graphics.color.Color;

public class RuntimeSetting {

    /*
     * Compile-time constants for the runtime layer. FBO target keys mirror
     * EngineSetting so runtime code has a single import. Composite layers
     * control blit order — lower renders first, higher composites on top.
     * The character creator's and settings menu's menus, text, ranges, and
     * colors live here too.
     */

    // Full Screen Passes
    public static final String PASS_SKY = "Sky";
    public static final String PASS_SSAO = "SSAO";
    public static final String PASS_LIGHTING = "Lighting";
    public static final String PASS_WEATHER = "Weather";
    public static final String PASS_PRECIPITATION = "Precipitation";

    // FBO Targets
    public static final String FBO_SKY = "SkyScene";
    public static final String FBO_WORLD = "MainScene";
    public static final String FBO_LIT = "LitScene";
    public static final String FBO_SSAO = "SSAOScene";
    public static final String FBO_UI = "UIScene";
    public static final String FBO_WEATHER = "WeatherScene";
    public static final String FBO_PRECIPITATION = "PrecipitationScene";

    // Composite Layers
    public static final int LAYER_SKY = -10;
    public static final int LAYER_WEATHER = -5;
    public static final int LAYER_WORLD = 0;
    public static final int LAYER_PRECIPITATION = 5;
    public static final int LAYER_UI = 10;

    // Pass Uniforms
    public static final String UNIFORM_SCENE_DEPTH = "u_sceneDepth";

    // Load Menu
    public static final String MENU_LOAD = "MainMenu/Load";
    public static final String MENU_LOAD_CHARACTER_SLOT = "MainMenu/character_slot";
    public static final String MENU_LOAD_EMPTY_NOTICE = "MainMenu/character_list_empty";
    public static final String ELEMENT_CHARACTER_SLOT_LABEL = "character_label";
    public static final int ENTRY_CHARACTER_LIST = 0;

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
    public static final String SETTINGS_NOTE_WORLD_APPLY = "World changes apply when you leave Settings";

    // Settings Options
    public static final String SETTINGS_OPTION_FULLSCREEN = "Fullscreen";
    public static final String SETTINGS_OPTION_VSYNC = "VSync";
    public static final String SETTINGS_OPTION_FIELD_OF_VIEW = "Field of View";
    public static final String SETTINGS_OPTION_RENDER_DISTANCE = "Distance";
    public static final String SETTINGS_OPTION_TERRAIN_DETAIL = "Detail";
    public static final String SETTINGS_SLIDER_MOUSE_SENSITIVITY = "Sensitivity";
    public static final String SETTINGS_VALUE_ON = "On";
    public static final String SETTINGS_VALUE_OFF = "Off";
    public static final String SETTINGS_FORMAT_CHUNKS = "%d chunks";
    public static final String SETTINGS_FORMAT_FIELD_OF_VIEW = "%.0f";
    public static final String SETTINGS_FORMAT_SENSITIVITY = "%.2f";
    public static final int SETTINGS_RENDER_DISTANCE_STEP = 8;
    public static final int SETTINGS_RENDER_DISTANCE_PER_RADIUS = 2;
    public static final int SETTINGS_TERRAIN_DETAIL_STEP = 1;
    public static final float SETTINGS_FIELD_OF_VIEW_STEP = 5f;
    public static final float SETTINGS_SLIDER_PERCENT_SCALE = 100f;

    // Settings Bindings
    public static final String SETTINGS_BINDING_MOVE_FORWARD = "Move Forward";
    public static final String SETTINGS_BINDING_MOVE_BACK = "Move Back";
    public static final String SETTINGS_BINDING_MOVE_LEFT = "Move Left";
    public static final String SETTINGS_BINDING_MOVE_RIGHT = "Move Right";
    public static final String SETTINGS_BINDING_JUMP = "Jump";
    public static final String SETTINGS_BINDING_WALK = "Walk";
    public static final String SETTINGS_BINDING_SPRINT = "Sprint";
    public static final String SETTINGS_BINDING_USE = "Use";
    public static final String SETTINGS_BINDING_INVENTORY = "Inventory";
    public static final String SETTINGS_BINDING_SCREENSHOT = "Screenshot";
    public static final String SETTINGS_BINDING_RECORD_VIDEO = "Record Video";
    public static final String SETTINGS_BINDING_CAPTURE_PROMPT = "Press a key";
    public static final String SETTINGS_ACTION_RESET_BINDINGS = "Reset Controls";

    // Settings Colors
    public static final Color SETTINGS_TAB_ACTIVE_COLOR = new Color(1f, 1f, 1f, 1f);
    public static final Color SETTINGS_TAB_ACTIVE_LABEL_COLOR = new Color(0.463f, 0.086f, 0.071f, 1f);
    public static final Color SETTINGS_BINDING_CAPTURE_COLOR = new Color(1f, 0.84f, 0.42f, 1f);
}