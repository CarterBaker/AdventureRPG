package application.runtime;

public class RuntimeSetting {

    /*
     * Compile-time constants for the runtime layer. FBO target keys mirror
     * EngineSetting so runtime code has a single import. Composite layers
     * control blit order — lower renders first, higher composites on top.
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
}