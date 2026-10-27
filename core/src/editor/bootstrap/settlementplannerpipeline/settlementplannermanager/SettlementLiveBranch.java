package editor.bootstrap.settlementplannerpipeline.settlementplannermanager;

import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;

class SettlementLiveBranch extends BranchPackage {

    /*
     * Carries settlement type edits from the Info Panel into the engine as
     * they are made, saved or not: an edited or reverted type is rebuilt live,
     * every settlement of the world is settled and planned again, the maps
     * and preview terrain restream, and the planner shows its design anew.
     * An edit the engine refuses leaves the type as it was and reports why,
     * in the planner's status and the log. The planner's Edit Type opens the
     * designed settlement or structure in the hierarchy, where it is edited.
     */

    // Internal
    private InfoManager infoManager;
    private SettlementManager settlementManager;
    private WorldEditorManager worldEditorManager;
    private SettlementPlannerManager settlementPlannerManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_SETTLEMENTS, this::rebuildSettlement);
    }

    // Selection \\

    void selectInHierarchy(String schemaName, String entryName) {
        infoManager.selectEntry(schemaName, entryName, entryName);
    }

    // Live Edit \\

    private void rebuildSettlement(InfoEntryStruct entry) {

        try {
            settlementManager.rebuildSettlement(entry.getEntryName(), entry.getArpg());
        } catch (InternalException e) {
            errorLog(e.getMessage());
            settlementPlannerManager.setStatusMessage(
                    EditorSetting.SETTLEMENT_PLANNER_MESSAGE_TYPE_REFUSED + e.getMessage());
            return;
        }

        if (settlementPlannerManager.getWorldHandle() != null)
            worldEditorManager.restreamWorld(settlementPlannerManager.getWorldHandle());

        settlementPlannerManager.refreshCatalogue();
        settlementPlannerManager.setStatusMessage(
                EditorSetting.SETTLEMENT_PLANNER_MESSAGE_TYPE_LIVE + entry.getEntryName());
    }
}
