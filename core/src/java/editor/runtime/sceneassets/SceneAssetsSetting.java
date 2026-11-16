package editor.runtime.sceneassets;

public class SceneAssetsSetting {

    /*
     * Constants used only by SceneAssetsContext — the row templates its panel
     * is filled from and their elements, its entry points, and every word the
     * tab shows.
     */

    // Templates
    public static final String TEMPLATE_SECTION_ROW = "editor/SceneAssets/scene_assets_section_row";
    public static final String TEMPLATE_ROW = "editor/SceneAssets/scene_assets_row";
    public static final String TEMPLATE_LINK_ROW = "editor/SceneAssets/scene_assets_link_row";
    public static final String TEMPLATE_TEXT_ROW = "editor/SceneAssets/scene_assets_text_row";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "scene_assets_row_label";
    public static final String ELEMENT_ROW_VALUE = "scene_assets_row_value";

    // Entry Points
    public static final int ENTRY_TITLE = 0;
    public static final int ENTRY_ROWS = 1;

    // Text
    public static final String TEXT_SECTION_OPENED = "- ";
    public static final String TEXT_SECTION_CLOSED = "+ ";
    public static final String TEXT_HINT_OPEN_DEV = "Open a Dev window: this tab lists what it shows as you play";

    // Formats
    public static final String FORMAT_TITLE = "Scene Assets: %s";
    public static final String FORMAT_ROW_KEY = "%d:%d";
    public static final String ROW_KEY_SEPARATOR = ":";
}
