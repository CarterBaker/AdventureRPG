package editor.runtime.cameraplanner;

public class CameraPlannerSetting {

    /*
     * Constants used only by CameraPlannerContext — its toolbar menu with the
     * camera list and the view panel and the labels beside them. The design
     * and its cameras are the editor's.
     */

    // Menus
    public static final String MENU_TOOLBAR = "editor/CameraPlanner/Toolbar";
    public static final String TEMPLATE_CAMERA_ROW = "editor/CameraPlanner/camera_planner_camera_row";
    public static final String TEMPLATE_CAMERA_ROW_SELECTED =
            "editor/CameraPlanner/camera_planner_camera_row_selected";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_CAMERAS = 1;
    public static final int ENTRY_COUNT = 2;
    public static final int ENTRY_DESIGN = 3;
    public static final int ENTRY_EDITED = 4;

    // Elements
    public static final String ELEMENT_CAMERA_LABEL = "camera_planner_camera_label";
    public static final String ELEMENT_CAMERA_SWATCH = "camera_planner_camera_swatch";
}
