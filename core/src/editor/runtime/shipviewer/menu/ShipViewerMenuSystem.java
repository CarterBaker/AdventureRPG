package editor.runtime.shipviewer.menu;

import java.util.Arrays;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.shipviewer.ShipViewerSetting;
import editor.runtime.shipviewer.view.ShipViewerSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class ShipViewerMenuSystem extends SystemPackage {

    /*
     * Opens the Ship Viewer's menu over the viewport: the status line names
     * the ship shown, whether it is the one on the Shipwright's table, and
     * how many parts it is built of, or why none is shown; the deck label
     * says which decks the cutaway leaves; both are rewritten only when what
     * is shown changes. The axis gizmo turns with the view as a scene gizmo
     * does: every axis of the ship an arm of dots from its middle out to a
     * cone where the axis points on screen, shortening as the axis turns
     * toward or away from the view, the cones of the length, up and across
     * red, green and blue and their opposites grey. Each dot moves only when
     * it crosses a step, to positions made once, so the gizmo costs nothing
     * while the view rests.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ShipViewerSystem shipViewerSystem;
    private ViewCameraSystem viewCameraSystem;

    // Menus
    private MenuInstance viewerMenu;

    // Status
    private int shownRevision;

    // Gizmo — every step's position, x step then y step, and the step each cone and arm dot was last put at
    private DimensionVector2Struct[] gizmoPositions;
    private int[] coneSteps;
    private int[] armSteps;

    // Base \\

    @Override
    protected void create() {

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;

        // Gizmo
        int steps = ShipViewerSetting.GIZMO_STEPS;
        float middle = (steps - 1) * 0.5f;
        float pitch = ShipViewerSetting.GIZMO_RADIUS_PIXELS / middle;

        this.gizmoPositions = new DimensionVector2Struct[steps * steps];
        this.coneSteps = new int[ShipViewerSetting.VIEW_AXES.length];
        this.armSteps = new int[ShipViewerSetting.VIEW_AXES.length * ShipViewerSetting.GIZMO_ARM_DOTS];

        for (int y = 0; y < steps; y++)
            for (int x = 0; x < steps; x++)
                gizmoPositions[x + y * steps] = new DimensionVector2Struct(
                        DimensionValueStruct.ofAbsolute((x - middle) * pitch),
                        DimensionValueStruct.ofAbsolute((y - middle) * pitch));

        Arrays.fill(coneSteps, EngineSetting.INDEX_NOT_FOUND);
        Arrays.fill(armSteps, EngineSetting.INDEX_NOT_FOUND);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.shipViewerSystem = get(ShipViewerSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.viewerMenu = menuManager.openMenu(ShipViewerSetting.MENU_VIEWER, window);
    }

    // Update \\

    @Override
    protected void update() {

        placeGizmo();

        int revision = shipViewerSystem.getRevision();

        if (revision == shownRevision)
            return;

        shownRevision = revision;

        setLabel(ShipViewerSetting.ENTRY_STATUS, ShipViewerSetting.STATUS_PREFIX + describeShip());
        setLabel(ShipViewerSetting.ENTRY_DECK, describeDeck());
    }

    // The ship's name, whether it is the one on the table, and its part count, or why no ship is shown
    private String describeShip() {

        String vehicleName = shipViewerSystem.getVehicleName();

        if (shipViewerSystem.isShown())
            return shipViewerSystem.getDisplayName()
                    + (shipViewerSystem.isDraft()
                            ? ShipViewerSetting.STATUS_SEPARATOR + ShipViewerSetting.STATUS_DRAFT
                            : "")
                    + ShipViewerSetting.STATUS_SEPARATOR
                    + shipViewerSystem.getPartCount()
                    + ShipViewerSetting.STATUS_PARTS;

        return vehicleName == null
                ? ShipViewerSetting.STATUS_NONE
                : vehicleName + ShipViewerSetting.STATUS_UNREGISTERED;
    }

    // Every deck, or the deck the cutaway leaves with those under it
    private String describeDeck() {

        if (shipViewerSystem.getDeck() == ShipViewerSetting.DECK_ALL)
            return ShipViewerSetting.DECK_TEXT_ALL;

        return String.format(ShipViewerSetting.DECK_TEXT_FORMAT,
                shipViewerSystem.getDeck() + 1, shipViewerSystem.getDeckCount());
    }

    // Gizmo \\

    // Every axis's cone moved to where that axis points on screen and its arm's dots spaced out to it
    private void placeGizmo() {

        Vector3 right = viewCameraSystem.getRight();
        Vector3 up = viewCameraSystem.getUp();
        int dots = ShipViewerSetting.GIZMO_ARM_DOTS;

        for (int i = 0; i < ShipViewerSetting.VIEW_AXES.length; i++) {

            float[] axis = ShipViewerSetting.VIEW_AXIS_VECTORS[i];
            float screenX = axis[EngineSetting.AXIS_X] * right.x + axis[EngineSetting.AXIS_Y] * right.y
                    + axis[EngineSetting.AXIS_Z] * right.z;
            float screenY = axis[EngineSetting.AXIS_X] * up.x + axis[EngineSetting.AXIS_Y] * up.y
                    + axis[EngineSetting.AXIS_Z] * up.z;

            placeDot(ShipViewerSetting.ENTRY_GIZMO_CONES + i, coneSteps, i, screenX, screenY);

            for (int dot = 0; dot < dots; dot++) {

                float share = (dot + 1f) / (dots + 1f);

                placeDot(ShipViewerSetting.ENTRY_GIZMO_ARMS + i * dots + dot, armSteps, i * dots + dot,
                        screenX * share, screenY * share);
            }
        }
    }

    // One of the gizmo's elements moved to the step nearest a point across it, from -1 to 1 either way, only when
    // that step changes
    private void placeDot(int entryPoint, int[] shownSteps, int slot, float x, float y) {

        int steps = ShipViewerSetting.GIZMO_STEPS;
        float middle = (steps - 1) * 0.5f;
        int stepX = Math.clamp(Math.round(middle + x * middle), 0, steps - 1);
        int stepY = Math.clamp(Math.round(middle + y * middle), 0, steps - 1);
        int step = stepX + stepY * steps;

        if (step == shownSteps[slot])
            return;

        ElementInstance element = viewerMenu.getEntryPoint(entryPoint);

        if (element == null)
            return;

        shownSteps[slot] = step;
        element.setPositionOverride(gizmoPositions[step]);
    }

    // Utility \\

    private void setLabel(int entryPoint, String value) {

        ElementInstance label = viewerMenu.getEntryPoint(entryPoint);

        if (label != null)
            label.setFontText(value);
    }
}
