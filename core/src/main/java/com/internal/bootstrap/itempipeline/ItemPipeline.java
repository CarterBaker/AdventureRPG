package com.internal.bootstrap.itempipeline;

import com.internal.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import com.internal.core.engine.ManagerPackage;

public class ItemPipeline extends ManagerPackage {

    @Override
    protected void create() {
        create(ItemDefinitionManager.class);
    }

}