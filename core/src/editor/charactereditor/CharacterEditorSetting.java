package editor.charactereditor;

public class CharacterEditorSetting {

    /*
     * Constants used only by CharacterEditorContext — its toolbar menu, the
     * row templates its two panels are filled from, and the size of the box
     * shown for a held anchor.
     */

    // Menus
    public static final String MENU_TOOLBAR = "editor/CharacterEditor/Toolbar";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_LIST = 1;
    public static final int ENTRY_PROPERTIES = 2;

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/CharacterEditor/character_editor_header_row";
    public static final String TEMPLATE_ITEM_ROW = "editor/CharacterEditor/character_editor_item_row";
    public static final String TEMPLATE_ITEM_ROW_SELECTED = "editor/CharacterEditor/character_editor_item_row_selected";
    public static final String TEMPLATE_NUMBER_ROW = "editor/CharacterEditor/character_editor_number_row";
    public static final String TEMPLATE_CHOICE_ROW = "editor/CharacterEditor/character_editor_choice_row";
    public static final String TEMPLATE_ACTION_ROW = "editor/CharacterEditor/character_editor_action_row";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "character_editor_row_label";
    public static final String ELEMENT_ROW_VALUE = "character_editor_row_value";
    public static final String ELEMENT_ROW_VALUE_BUTTON = "character_editor_row_value_button";
    public static final String ELEMENT_ROW_DECREASE = "character_editor_row_decrease";
    public static final String ELEMENT_ROW_INCREASE = "character_editor_row_increase";

    // Markers — the box shown for a held anchor, in blocks
    public static final float HELD_ANCHOR_MARKER_SIZE = 0.06f;
}
