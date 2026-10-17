package editor.animationeditor;

public class AnimationEditorSetting {

    /*
     * Constants used only by AnimationEditorContext — its toolbar menu, the
     * row templates its two panels are filled from, and its timeline's key
     * markers and playhead, placed in fixed steps along the track so moving
     * them never allocates.
     */

    // Menus
    public static final String MENU_TOOLBAR = "editor/AnimationEditor/Toolbar";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_LIST = 1;
    public static final int ENTRY_PROPERTIES = 2;
    public static final int ENTRY_KEYS = 3;
    public static final int ENTRY_PLAYHEAD = 4;

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/AnimationEditor/animation_editor_header_row";
    public static final String TEMPLATE_ITEM_ROW = "editor/AnimationEditor/animation_editor_item_row";
    public static final String TEMPLATE_ITEM_ROW_SELECTED = "editor/AnimationEditor/animation_editor_item_row_selected";
    public static final String TEMPLATE_NUMBER_ROW = "editor/AnimationEditor/animation_editor_number_row";
    public static final String TEMPLATE_CHOICE_ROW = "editor/AnimationEditor/animation_editor_choice_row";
    public static final String TEMPLATE_ACTION_ROW = "editor/AnimationEditor/animation_editor_action_row";
    public static final String TEMPLATE_KEY_MARKER = "editor/AnimationEditor/animation_editor_key_marker";
    public static final String TEMPLATE_OTHER_KEY_MARKER = "editor/AnimationEditor/animation_editor_other_key_marker";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "animation_editor_row_label";
    public static final String ELEMENT_ROW_VALUE = "animation_editor_row_value";
    public static final String ELEMENT_ROW_VALUE_BUTTON = "animation_editor_row_value_button";
    public static final String ELEMENT_ROW_DECREASE = "animation_editor_row_decrease";
    public static final String ELEMENT_ROW_INCREASE = "animation_editor_row_increase";

    // Timeline — positions along the track, as a percentage of it
    public static final int TIMELINE_STEPS = 1000;
    public static final float TIMELINE_PERCENT = 100f;
}
