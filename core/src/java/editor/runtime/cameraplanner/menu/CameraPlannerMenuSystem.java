package editor.runtime.cameraplanner.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.cameraplannerpipeline.cameraplannermanager.CameraPlannerManager;
import editor.runtime.EditorSetting;
import editor.runtime.cameraplanner.CameraPlannerSetting;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CameraPlannerMenuSystem extends SystemPackage {

    /*
     * Opens the Camera Planner toolbar and its two columns. The camera
     * column lists every camera of the active world, one row each with a
     * swatch in the camera icon's color and its name and coordinates, under
     * how many the main menu picks from; the view column shows the picked
     * camera and the design's view above the buttons that step and type it.
     * The picked row is highlighted and every row carries its index. The
     * list is rebuilt only when the planner's rows change, and the labels
     * only when the planner changes.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private CameraPlannerManager cameraPlannerManager;

    // Menus
    private MenuInstance toolbarMenu;

    // Rows
    private ObjectArrayList<ElementInstance> cameraElements;
    private MenuColorStruct cameraSwatch;
    private int shownRowRevision;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.cameraElements = new ObjectArrayList<>();
        this.cameraSwatch = new MenuColorStruct(new Color(
                EditorSetting.PLACEMENT_ICON_COLOR_CAMERA[0],
                EditorSetting.PLACEMENT_ICON_COLOR_CAMERA[1],
                EditorSetting.PLACEMENT_ICON_COLOR_CAMERA[2],
                EditorSetting.PLACEMENT_ICON_COLOR_CAMERA[3]));
        this.shownRowRevision = EngineSetting.INDEX_NOT_FOUND;

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.cameraPlannerManager = get(CameraPlannerManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(CameraPlannerSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        refreshStatus();

        if (cameraPlannerManager.getRowRevision() == shownRowRevision)
            return;

        shownRowRevision = cameraPlannerManager.getRowRevision();
        refreshCameras();
    }

    // Status \\

    private void refreshStatus() {

        if (cameraPlannerManager.getRevision() == shownRevision)
            return;

        shownRevision = cameraPlannerManager.getRevision();

        setEntryText(CameraPlannerSetting.ENTRY_STATUS, cameraPlannerManager.getStatusText());
        setEntryText(CameraPlannerSetting.ENTRY_COUNT, cameraPlannerManager.describeCount());
        setEntryText(CameraPlannerSetting.ENTRY_DESIGN, cameraPlannerManager.describeView());
        setEntryText(CameraPlannerSetting.ENTRY_EDITED, cameraPlannerManager.describeEdited());
    }

    private void setEntryText(int entry, String text) {

        ElementInstance label = toolbarMenu.getEntryPoint(entry);

        if (label != null)
            label.setFontText(text);
    }

    // Rows \\

    private void refreshCameras() {

        for (int i = 0; i < cameraElements.size(); i++)
            menuManager.eject(toolbarMenu, CameraPlannerSetting.ENTRY_CAMERAS, cameraElements.get(i));

        cameraElements.clear();

        ObjectArrayList<WorldPlacementStruct> cameras = cameraPlannerManager.getCameras();

        for (int i = 0; i < cameras.size(); i++)
            cameraElements.add(injectCamera(cameras.get(i), i));
    }

    private ElementInstance injectCamera(WorldPlacementStruct camera, int index) {

        String template = cameraPlannerManager.isEdited(camera)
                ? CameraPlannerSetting.TEMPLATE_CAMERA_ROW_SELECTED
                : CameraPlannerSetting.TEMPLATE_CAMERA_ROW;
        String text = cameraPlannerManager.describePlacement(camera);

        return menuManager.inject(
                toolbarMenu, CameraPlannerSetting.ENTRY_CAMERAS, template,
                element -> {
                    element.setActionArgOverride(Integer.toString(index));

                    ElementInstance swatch = element.findChildById(CameraPlannerSetting.ELEMENT_CAMERA_SWATCH);
                    ElementInstance label = element.findChildById(CameraPlannerSetting.ELEMENT_CAMERA_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(cameraSwatch);

                    if (label != null)
                        label.setFontText(text);
                });
    }
}
