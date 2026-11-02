package editor.runtime.shipviewer;

import engine.input.Buttons;

public class ShipViewerSetting {

    /*
     * Constants used only by ShipViewerContext — its material and render
     * order, its orbit camera and mouse binding, its menu and the steps its
     * buttons take through the vehicles, and the text of its status line.
     */

    // Materials
    public static final String MATERIAL_SHIP = "editor/ShipViewerMaterial";

    // Render Order
    public static final int DEPTH_SHIP = 0;

    // Camera — the frame's centre the middle of the ship's bounds, the distances in its bounding radius
    public static final float ORBIT_DISTANCE_DEFAULT = 2.2f;
    public static final float ORBIT_DISTANCE_MIN = 0.3f;
    public static final float ORBIT_DISTANCE_MAX = 8f;
    public static final float ORBIT_ZOOM_STEP = 0.1f;
    public static final float ORBIT_INITIAL_YAW_DEGREES = 35f;
    public static final float ORBIT_INITIAL_PITCH_DEGREES = 18f;

    // Mouse
    public static final int BUTTON_ORBIT = Buttons.RIGHT;

    // Menus
    public static final String MENU_VIEWER = "editor/ShipViewer/Viewer";
    public static final int ENTRY_STATUS = 0;

    // Text
    public static final String STATUS_PREFIX = "Ship Viewer  |  ";
    public static final String STATUS_NONE = "No ship registered — save one at the Shipwright";
    public static final String STATUS_SEPARATOR = "  ·  ";
    public static final String STATUS_OF = " of ";
    public static final String STATUS_PARTS = " parts";
    public static final String STATUS_FOLLOWING = "shows each ship as it is saved";
}
