package application.bootstrap.itempipeline.itemrotationmanager;

import application.core.engine.ManagerPackage;

public class ItemRotationManager extends ManagerPackage {

    @Override
    protected void create() {
        create(InternalBufferSystem.class);
    }
}