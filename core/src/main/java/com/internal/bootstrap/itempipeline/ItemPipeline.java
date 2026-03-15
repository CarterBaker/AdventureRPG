package com.internal.bootstrap.itempipeline;

import com.internal.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import com.internal.bootstrap.itempipeline.itemrotationmanager.ItemRotationManager;
import com.internal.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import com.internal.core.engine.PipelinePackage;

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