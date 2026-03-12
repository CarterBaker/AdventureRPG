package com.internal.bootstrap.itempipeline.itemrotationmanager;

import com.internal.core.engine.ManagerPackage;

public class ItemRotationManager extends ManagerPackage {

    @Override
    protected void create() {
        create(InternalBufferSystem.class);
    }
}