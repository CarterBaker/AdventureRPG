package editor.runtime.menueventsmanager.menus;

import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.runtime.EditorSetting;
import editor.runtime.worldeditor.WorldEditorContext;
import engine.input.Buttons;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettlementPlannerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Settlement Planner toolbar and lists. Every
     * action works on the shared design and the placement it edits.
     * Catalogue rows are taken hold of rather than clicked: the press chooses
     * the entry and carries it, and letting go over a World Editor stands it
     * where it is dropped, anywhere else only chooses it. Architecture and
     * placed rows carry their index in their list, and the coordinate buttons
     * open the text dialog over the planner they were clicked in.
     */

    // Internal
    private SettlementPlannerManager settlementPlannerManager;
    private NameDialogBranch nameDialogBranch;
    private InputManager inputManager;
    private WindowManager windowManager;

    // Base \\

    @Override
    protected void get() {
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void update() {
        updateDrag();
    }

    // A carry ends when its button is let go, dropping its design on the World Editor under the pointer, and is
    // dropped when its window leaves
    private void updateDrag() {

        if (!settlementPlannerManager.isDragging())
            return;

        WindowInstance dragWindow = settlementPlannerManager.getDragWindow();

        if (!windowManager.getWindows().contains(dragWindow)) {
            settlementPlannerManager.endDrag();
            return;
        }

        if (inputManager.getRawInput(dragWindow).isMouseDown(Buttons.LEFT))
            return;

        settlementPlannerManager.endDrag();

        ObjectArrayList<WindowInstance> hoveredWindows = windowManager.getHoveredWindows();

        for (int i = 0; i < hoveredWindows.size(); i++) {

            if (!(hoveredWindows.get(i).getContext() instanceof WorldEditorContext worldEditorContext))
                continue;

            worldEditorContext.dropDesign();
            return;
        }
    }

    // Catalogue \\

    public void dragEntry(String index, WindowInstance window) {
        settlementPlannerManager.beginDrag(Integer.parseInt(index), window);
    }

    public void previousEntry() {
        settlementPlannerManager.cycleEntry(-1);
    }

    public void nextEntry() {
        settlementPlannerManager.cycleEntry(1);
    }

    public void editType() {
        settlementPlannerManager.editType();
    }

    // Architecture \\

    public void selectArchitecture(String index) {
        settlementPlannerManager.selectArchitecture(Integer.parseInt(index));
    }

    public void previousArchitecture() {
        settlementPlannerManager.cycleArchitecture(-1);
    }

    public void nextArchitecture() {
        settlementPlannerManager.cycleArchitecture(1);
    }

    // Layout \\

    public void previousLayout() {
        settlementPlannerManager.stepLayout(-1);
    }

    public void nextLayout() {
        settlementPlannerManager.stepLayout(1);
    }

    public void randomLayout() {
        settlementPlannerManager.randomLayout();
    }

    public void turn() {
        settlementPlannerManager.turn();
    }

    // Placed \\

    public void selectPlaced(String index) {
        settlementPlannerManager.selectPlacedHere(Integer.parseInt(index));
    }

    public void detach() {
        settlementPlannerManager.detach();
    }

    public void setX(WindowInstance window) {

        if (!settlementPlannerManager.requireEditedPlacement())
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_PLACEMENT_X,
                settlementPlannerManager.formatEditedX(),
                settlementPlannerManager::isCoordinateValid,
                settlementPlannerManager::setEditedX);
    }

    public void setZ(WindowInstance window) {

        if (!settlementPlannerManager.requireEditedPlacement())
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_PLACEMENT_Z,
                settlementPlannerManager.formatEditedZ(),
                settlementPlannerManager::isCoordinateValid,
                settlementPlannerManager::setEditedZ);
    }

    public void removePlaced() {
        settlementPlannerManager.removeEditedPlacement();
    }
}
