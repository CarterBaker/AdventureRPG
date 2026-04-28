package application.runtime;

import engine.root.EngineSetting;

public class RuntimeSetting {

    // FBO Targets — string keys passed to FboManager.getFbo()

    public static final String FBO_SKY = EngineSetting.FBO_SKY;
    public static final String FBO_WORLD = EngineSetting.FBO_WORLD;
    public static final String FBO_UI = EngineSetting.FBO_UI;

    // Composite Layers — lower renders first, higher composites on top

    public static final int LAYER_SKY = -10;
    public static final int LAYER_WORLD = 0;
    public static final int LAYER_UI = 10;
}
