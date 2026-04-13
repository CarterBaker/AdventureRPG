package application.bootstrap.itempipeline;

import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.itemrotationmanager.ItemRotationManager;
import application.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import engine.root.PipelinePackage;

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