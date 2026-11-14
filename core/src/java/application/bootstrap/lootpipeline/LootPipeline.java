package application.bootstrap.lootpipeline;

import application.bootstrap.lootpipeline.loottablemanager.LootTableManager;
import engine.root.PipelinePackage;

public class LootPipeline extends PipelinePackage {

    /*
     * Registers the loot pipeline. LootTableManager owns every loot table, so
     * a chest found in a house, the stores of a ship, the gear a resident
     * wears and what it drops when it falls are all drawn the same way. The
     * pipeline is created after the item pipeline, whose definitions every
     * table names, and ahead of the vehicle, entity and world pipelines that
     * draw from it.
     */

    @Override
    protected void create() {
        create(LootTableManager.class);
    }
}
