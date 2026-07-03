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

    // FBO Targets
    public static final String FBO_SKY = "SkyScene";
    public static final String FBO_WORLD = "MainScene";
    public static final String FBO_LIT = "LitScene";
    public static final String FBO_SSAO = "SSAOScene";
    public static final String FBO_UI = "UIScene";
    public static final String FBO_OVERHEAD = "OverheadScene";

    // Composite Layers
    public static final int LAYER_SKY = -10;
    public static final int LAYER_OVERHEAD = -5;
    public static final int LAYER_WORLD = 0;
    public static final int LAYER_UI = 10;
}