package application.bootstrap.itempipeline.itemrotationmanager;

import engine.root.ManagerPackage;

public class ItemRotationManager extends ManagerPackage {

    @Override
    protected void create() {
        create(InternalBufferSystem.class);
    }
}