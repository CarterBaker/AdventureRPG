package editor.runtime.animationtreeeditor;

public class AnimationTreeEditorSetting {

    /*
     * Constants used only by AnimationTreeEditorContext — its toolbar menu
     * and the row templates its two panels are filled from.
     */

    // Menus
    public static final String MENU_TOOLBAR = "editor/AnimationTreeEditor/Toolbar";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_LIST = 1;
    public static final int ENTRY_PROPERTIES = 2;

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/AnimationTreeEditor/animation_tree_editor_header_row";
    public static final String TEMPLATE_ITEM_ROW = "editor/AnimationTreeEditor/animation_tree_editor_item_row";
    public static final String TEMPLATE_ITEM_ROW_SELECTED =
            "editor/AnimationTreeEditor/animation_tree_editor_item_row_selected";
    public static final String TEMPLATE_NUMBER_ROW = "editor/AnimationTreeEditor/animation_tree_editor_number_row";
    public static final String TEMPLATE_CHOICE_ROW = "editor/AnimationTreeEditor/animation_tree_editor_choice_row";
    public static final String TEMPLATE_ACTION_ROW = "editor/AnimationTreeEditor/animation_tree_editor_action_row";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "animation_tree_editor_row_label";
    public static final String ELEMENT_ROW_VALUE = "animation_tree_editor_row_value";
    public static final String ELEMENT_ROW_VALUE_BUTTON = "animation_tree_editor_row_value_button";
    public static final String ELEMENT_ROW_DECREASE = "animation_tree_editor_row_decrease";
    public static final String ELEMENT_ROW_INCREASE = "animation_tree_editor_row_increase";
}
