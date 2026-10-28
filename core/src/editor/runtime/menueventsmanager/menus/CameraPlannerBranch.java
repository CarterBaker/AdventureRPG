package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.cameraplannerpipeline.cameraplannermanager.CameraPlannerManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class CameraPlannerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Camera Planner toolbar, camera list and
     * view panel. Every action works on the shared design and the camera it
     * edits. Camera rows carry their index in the list, and the name, view
     * and coordinate buttons open the text dialog over the planner they were
     * clicked in, starting from the value as it stands.
     */

    // Internal
    private CameraPlannerManager cameraPlannerManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.cameraPlannerManager = get(CameraPlannerManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Toolbar \\

    public void place() {
        cameraPlannerManager.place();
    }

    public void preview(WindowInstance window) {
        cameraPlannerManager.preview(window);
    }

    public void detach() {
        cameraPlannerManager.detach();
    }

    public void removeCamera() {
        cameraPlannerManager.removeEditedPlacement();
    }

    // Cameras \\

    public void selectCamera(String index) {
        cameraPlannerManager.selectCamera(Integer.parseInt(index));
    }

    // View \\

    public void lowerHeight() {
        cameraPlannerManager.stepHeight(-1);
    }

    public void raiseHeight() {
        cameraPlannerManager.stepHeight(1);
    }

    public void turnLeft() {
        cameraPlannerManager.stepYaw(-1);
    }

    public void turnRight() {
        cameraPlannerManager.stepYaw(1);
    }

    public void lookDown() {
        cameraPlannerManager.stepPitch(-1);
    }

    public void lookUp() {
        cameraPlannerManager.stepPitch(1);
    }

    public void setName(WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_CAMERA_NAME,
                cameraPlannerManager.getName(),
                cameraPlannerManager::isNameValid,
                cameraPlannerManager::setName);
    }

    public void setHeight(WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_CAMERA_HEIGHT,
                cameraPlannerManager.formatHeight(),
                cameraPlannerManager::isValueValid,
                cameraPlannerManager::setHeight);
    }

    public void setYaw(WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_CAMERA_YAW,
                cameraPlannerManager.formatYaw(),
                cameraPlannerManager::isValueValid,
                cameraPlannerManager::setYaw);
    }

    public void setPitch(WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_CAMERA_PITCH,
                cameraPlannerManager.formatPitch(),
                cameraPlannerManager::isValueValid,
                cameraPlannerManager::setPitch);
    }

    // Placement \\

    public void setX(WindowInstance window) {

        if (!cameraPlannerManager.requireEditedPlacement())
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_PLACEMENT_X,
                cameraPlannerManager.formatEditedX(),
                cameraPlannerManager::isCoordinateValid,
                cameraPlannerManager::setEditedX);
    }

    public void setZ(WindowInstance window) {

        if (!cameraPlannerManager.requireEditedPlacement())
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_PLACEMENT_Z,
                cameraPlannerManager.formatEditedZ(),
                cameraPlannerManager::isCoordinateValid,
                cameraPlannerManager::setEditedZ);
    }
}
