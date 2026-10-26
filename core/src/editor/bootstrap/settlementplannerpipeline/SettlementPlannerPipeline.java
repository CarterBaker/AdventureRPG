package editor.bootstrap.settlementplannerpipeline;

import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import engine.root.PipelinePackage;

public class SettlementPlannerPipeline extends PipelinePackage {

    /*
     * Registers the settlement planner's shared state, so every Settlement
     * Planner tab designs the same settlement or structure, every map shows
     * its preview, and the World Editor places exactly what it designs.
     */

    // Internal \\

    @Override
    protected void create() {
        create(SettlementPlannerManager.class);
    }
}
