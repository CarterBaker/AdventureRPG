package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.runtime.settlementplanner.SettlementPlannerContext;
import engine.root.BranchPackage;

public class SettlementPlannerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Settlement Planner toolbar and lists. Every
     * action works on the shared design, except Preview Here, which takes the
     * centre of the map in the planner its button was clicked in; catalogue
     * and architecture rows carry their index in their list.
     */

    // Internal
    private SettlementPlannerManager settlementPlannerManager;

    // Base \\

    @Override
    protected void get() {
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
    }

    // Catalogue \\

    public void selectEntry(String index) {
        settlementPlannerManager.selectEntry(Integer.parseInt(index));
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

    // Site \\

    public void detach() {
        settlementPlannerManager.detach();
    }

    public void previewHere(WindowInstance window) {
        if (window.getContext() instanceof SettlementPlannerContext settlementPlannerContext)
            settlementPlannerContext.previewHere();
    }
}
