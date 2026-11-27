package editor.runtime.layoutplanner;

import editor.runtime.characterview.panel.CharacterViewTemplateStruct;

public class LayoutPlannerSetting {

    /*
     * Constants used only by LayoutPlannerContext — how long its design must
     * rest before the preview is planned again, the margin round the preview,
     * its render order, its menu, the row templates the Palette tab lists its
     * choices with and the Properties tab its node's fields with, and the
     * text of its status line.
     */

    // Planning — frames the design rests unchanged before it is planned again, so a dragged slider plans once
    public static final int PLAN_REST_FRAMES = 12;

    // Shown — blocks of margin round everything the plan lays, and the least the view frames
    public static final int MARGIN_BLOCKS = 2;
    public static final float MIN_RADIUS_BLOCKS = 1f;

    // Render Order
    public static final int DEPTH_LAYOUT = 0;

    // Menus — entry points in the order the menu lists them
    public static final String MENU_VIEWER = "editor/LayoutPlanner/Viewer";
    public static final int ENTRY_STATUS = 0;

    // Rows — the templates of the planner's menu the Palette and Properties tabs list their rows with
    public static final String TEMPLATE_HEADER_ROW = "editor/LayoutPlanner/layout_planner_header_row";
    public static final String TEMPLATE_ITEM_ROW = "editor/LayoutPlanner/layout_planner_item_row";
    public static final String TEMPLATE_ITEM_ROW_SELECTED = "editor/LayoutPlanner/layout_planner_item_row_selected";
    public static final String TEMPLATE_NUMBER_ROW = "editor/LayoutPlanner/layout_planner_number_row";
    public static final String TEMPLATE_CHOICE_ROW = "editor/LayoutPlanner/layout_planner_choice_row";
    public static final String ELEMENT_ROW_LABEL = "layout_planner_row_label";
    public static final String ELEMENT_ROW_VALUE = "layout_planner_row_value";
    public static final String ELEMENT_ROW_VALUE_BUTTON = "layout_planner_row_value_button";
    public static final String ELEMENT_ROW_DECREASE = "layout_planner_row_decrease";
    public static final String ELEMENT_ROW_INCREASE = "layout_planner_row_increase";
    public static final String ELEMENT_ROW_TRACK = "layout_planner_row_track";
    public static final String ELEMENT_ROW_KNOB = "layout_planner_row_knob";
    public static final CharacterViewTemplateStruct ROW_TEMPLATES = new CharacterViewTemplateStruct(
            TEMPLATE_HEADER_ROW,
            TEMPLATE_ITEM_ROW,
            TEMPLATE_ITEM_ROW_SELECTED,
            TEMPLATE_NUMBER_ROW,
            null,
            TEMPLATE_CHOICE_ROW,
            null,
            null,
            ELEMENT_ROW_LABEL,
            ELEMENT_ROW_VALUE,
            ELEMENT_ROW_VALUE_BUTTON,
            ELEMENT_ROW_DECREASE,
            ELEMENT_ROW_INCREASE,
            ELEMENT_ROW_TRACK,
            ELEMENT_ROW_KNOB,
            null,
            null,
            null);

    // Layers and Properties — the titles the shared tabs give the planner
    public static final String LAYER_TITLE = "Layout Planner";
    public static final String PROPERTIES_TITLE = "Layout Planner";

    // Text
    public static final String STATUS_PREFIX = "Layout Planner  |  ";
    public static final String STATUS_NONE = "No settlement chosen — pick one in the Palette tab or the Hierarchy";
    public static final String STATUS_NO_WORLD = "No world is open to plan the preview on";
    public static final String STATUS_SEPARATOR = "  ·  ";
    public static final String STATUS_LAYOUT = "Layout ";
    public static final String STATUS_BUILDINGS = " buildings";
    public static final String STATUS_ROADS = " roads";
}
