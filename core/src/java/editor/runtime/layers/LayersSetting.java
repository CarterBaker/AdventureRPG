package editor.runtime.layers;

public class LayersSetting {

    /*
     * Constants used only by LayersContext — its menu, the layer row
     * templates with the element ids they are filled through, its entry
     * points, how rows are indented, how a hidden layer's eye and a folded
     * group holding the active layer are drawn, and the title shown while no
     * window works on layers.
     */

    // Menus
    public static final String MENU_LAYERS = "editor/Layers/Layers";
    public static final String TEMPLATE_LAYER_ROW = "editor/Layers/layer_row";
    public static final String TEMPLATE_LAYER_ROW_ACTIVE = "editor/Layers/layer_row_active";

    // Entry Points
    public static final int ENTRY_ROWS = 0;
    public static final int ENTRY_TITLE = 1;

    // Elements
    public static final String ELEMENT_TOGGLE = "layer_toggle";
    public static final String ELEMENT_TOGGLE_LABEL = "layer_toggle_label";
    public static final String ELEMENT_EYE = "layer_eye";
    public static final String ELEMENT_EYE_ICON = "layer_eye_icon";
    public static final String ELEMENT_LABEL = "layer_label";

    // Layout
    public static final float INDENT_PIXELS = 14f;
    public static final float TOGGLE_WIDTH_PIXELS = 16f;
    public static final float EYE_WIDTH_PIXELS = 20f;

    // Colors — a hidden layer's eye is drawn this faint
    public static final float EYE_HIDDEN_ALPHA = 0.25f;

    // Text
    public static final String TITLE_NO_LAYERS = "No window with layers";
}
