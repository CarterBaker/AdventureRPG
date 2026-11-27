package editor.runtime.roadplanner;

import editor.runtime.characterview.panel.CharacterViewTemplateStruct;

public class RoadPlannerSetting {

    /*
     * Constants used only by RoadPlannerContext — its menu, the entry points
     * its grid cells and status line are shown in, the row templates the
     * Palette tab lists its road types and biomes with, and the text of its
     * cells and status line.
     */

    // Menus
    public static final String MENU_PLANNER = "editor/RoadPlanner/Planner";
    public static final int ENTRY_FIRST_CELL = 0;
    public static final int ENTRY_STATUS = 9;

    // Rows — the templates of the planner's menu the Palette tab lists its rows with
    public static final String TEMPLATE_HEADER_ROW = "editor/RoadPlanner/road_planner_header_row";
    public static final String TEMPLATE_ITEM_ROW = "editor/RoadPlanner/road_planner_item_row";
    public static final String TEMPLATE_ITEM_ROW_SELECTED = "editor/RoadPlanner/road_planner_item_row_selected";
    public static final String ELEMENT_ROW_LABEL = "road_planner_row_label";
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

    // Text
    public static final String CELL_WAYPOINT_PREFIX = "  (road ";
    public static final String CELL_WAYPOINT_SUFFIX = ")";
    public static final String STATUS_PREFIX = "Road Planner  |  ";
    public static final String STATUS_NONE = "No road chosen — pick one in the Palette tab or the Hierarchy";
    public static final String STATUS_FEW = "Place the road through two cells or more";
    public static final String STATUS_SEPARATOR = "  ·  ";
    public static final String STATUS_LENGTH = " blocks long";
    public static final String STATUS_BRIDGES = " bridges";
    public static final String STATUS_TUNNELS = " tunnels";
}
