package application.runtime;

public class RuntimeSetting {

    /*
     * Compile-time constants for the runtime layer. FBO target keys mirror
     * EngineSetting so runtime code has a single import. Composite layers
     * control blit order — lower renders first, higher composites on top.
     */

    // FBO Targets
    public static final String FBO_SKY = "SkyScene";
    public static final String FBO_WORLD = "MainScene";
    public static final String FBO_UI = "UIScene";

    // Composite Layers
    public static final int LAYER_SKY = -10;
    public static final int LAYER_WORLD = 0;
    public static final int LAYER_UI = 10;
}