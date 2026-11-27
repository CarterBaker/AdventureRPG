package editor.runtime.architecturebuilder;

import editor.runtime.characterview.panel.CharacterViewTemplateStruct;

public class ArchitectureBuilderSetting {

    /*
     * Constants used only by ArchitectureBuilderContext — how its preview
     * lays slots out, its menu, the row templates the
     * Palette tab lists architectures and slot fillings with, and the text of
     * its status line.
     */

    // Preview — a block slot shown as a square wall of its block's pieces, every slot shown a gap apart along x
    public static final int PREVIEW_WALL_PIECES = 8;
    public static final float PREVIEW_GAP_BLOCKS = 1f;

    // Menus — entry points in the order the menu lists them
    public static final String MENU_VIEWER = "editor/ArchitectureBuilder/Viewer";
    public static final int ENTRY_STATUS = 0;

    // Rows — the templates of the builder's menu the Palette tab lists its rows with
    public static final String TEMPLATE_HEADER_ROW = "editor/ArchitectureBuilder/architecture_builder_header_row";
    public static final String TEMPLATE_ITEM_ROW = "editor/ArchitectureBuilder/architecture_builder_item_row";
    public static final String TEMPLATE_ITEM_ROW_SELECTED =
            "editor/ArchitectureBuilder/architecture_builder_item_row_selected";
    public static final String ELEMENT_ROW_LABEL = "architecture_builder_row_label";
    public static final CharacterViewTemplateStruct ROW_TEMPLATES = new CharacterViewTemplateStruct(
            TEMPLATE_HEADER_ROW,
            TEMPLATE_ITEM_ROW,
            TEMPLATE_ITEM_ROW_SELECTED,
            null,
            null,
            null,
            null,
            null,
            ELEMENT_ROW_LABEL,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    // Layers — the title the Layers tab gives the builder's slots
    public static final String LAYER_TITLE = "Architecture Builder";

    // Text
    public static final String STATUS_PREFIX = "Architecture Builder  |  ";
    public static final String STATUS_NONE = "No architecture chosen — pick one in the Palette tab or the Hierarchy";
    public static final String STATUS_SEPARATOR = "  ·  ";
}
