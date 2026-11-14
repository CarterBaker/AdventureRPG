package editor.runtime.biomeviewer;

public class BiomeViewerSetting {

    /*
     * Constants used only by BiomeViewerContext — its status, pause and
     * minimap menus, the entry points and templates the minimap is drawn
     * with, and how finely its dot follows the camera. The world it flies
     * over is the biome planner's.
     */

    // Menus
    public static final String MENU_STATUS = "editor/BiomeViewer/Status";
    public static final String MENU_PAUSED = "editor/BiomeViewer/Paused";
    public static final String MENU_MINIMAP = "editor/BiomeViewer/Minimap";
    public static final String TEMPLATE_MINIMAP_COLUMN = "editor/BiomeViewer/biome_viewer_minimap_column";
    public static final String TEMPLATE_MINIMAP_ROW = "editor/BiomeViewer/biome_viewer_minimap_row";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_MINIMAP_NET = 0;
    public static final int ENTRY_MINIMAP_CARRIER = 1;

    // Elements
    public static final String ELEMENT_MINIMAP_DOT = "biome_viewer_minimap_dot";

    // Minimap — one step per pixel of the minimap's side
    public static final int MINIMAP_STEPS = 96;
    public static final float MINIMAP_PERCENT = 100f;

    // Text
    public static final String STATUS_PREFIX = "Biome Viewer  |  ";
}
