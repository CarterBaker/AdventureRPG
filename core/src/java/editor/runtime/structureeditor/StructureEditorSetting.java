package editor.runtime.structureeditor;

import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import engine.input.Buttons;

public class StructureEditorSetting {

    /*
     * Constants used only by StructureEditorContext — the button its tools
     * apply with, how far the pointer's ray is walked through the cells, the
     * width of an NPC's marker, its menu, the row templates the Palette tab lists its
     * choices with, and the text of its status line.
     */

    // Input
    public static final int BUTTON_APPLY = Buttons.LEFT;

    // Cast — the most cells the pointer's ray walks before it gives up
    public static final int CAST_MAX_STEPS = 512;

    // Shown — an NPC's marker post is this many sub-voxels across
    public static final int SPAWN_MARKER_WIDTH_SUB_VOXELS = 4;

    // Menus — entry points in the order the menu lists them
    public static final String MENU_VIEWER = "editor/StructureEditor/Viewer";
    public static final int ENTRY_STATUS = 0;

    // Rows — the templates of the editor's menu the Palette tab lists its rows with
    public static final String TEMPLATE_HEADER_ROW = "editor/StructureEditor/structure_editor_header_row";
    public static final String TEMPLATE_ITEM_ROW = "editor/StructureEditor/structure_editor_item_row";
    public static final String TEMPLATE_ITEM_ROW_SELECTED =
            "editor/StructureEditor/structure_editor_item_row_selected";
    public static final String ELEMENT_ROW_LABEL = "structure_editor_row_label";
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

    // Layers — the title the Layers tab gives the editor's layers
    public static final String LAYER_TITLE = "Structure Editor";

    // Text
    public static final String STATUS_PREFIX = "Structure Editor  |  ";
    public static final String STATUS_NONE = "No structure chosen — pick one in the Palette tab or the Hierarchy";
    public static final String STATUS_SEPARATOR = "  ·  ";
    public static final String STATUS_BLOCKS = " blocks";
    public static final String STATUS_LAYER_SEPARATOR = " / ";
    public static final String STATUS_AS_AUTHORED = "as authored";
    public static final String STATUS_BUILT_IN = "built in ";
}
