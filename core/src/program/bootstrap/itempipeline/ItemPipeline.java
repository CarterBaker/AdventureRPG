package program.bootstrap.itempipeline;

import program.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import program.bootstrap.itempipeline.itemrotationmanager.ItemRotationManager;
import program.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import program.core.engine.PipelinePackage;

public class ItemPipeline extends PipelinePackage {

    /*
     * Registers all item pipeline managers in dependency order. ToolTypeManager
     * is registered first since item definitions may reference tool type IDs
     * during their build pass.
     */

    @Override
    protected void create() {
        create(ToolTypeManager.class);
        create(ItemDefinitionManager.class);
        create(ItemRotationManager.class);
    }
}