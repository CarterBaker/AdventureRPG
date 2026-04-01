package program.bootstrap.itempipeline.itemrotationmanager;

import program.core.engine.ManagerPackage;

public class ItemRotationManager extends ManagerPackage {

    @Override
    protected void create() {
        create(InternalBufferSystem.class);
    }
}