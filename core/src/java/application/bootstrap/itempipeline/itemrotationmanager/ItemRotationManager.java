package application.bootstrap.itempipeline.itemrotationmanager;

import engine.root.ManagerPackage;

public class ItemRotationManager extends ManagerPackage {

    /*
     * Owns the item rotation table, pushed once to the GPU by
     * ItemRotationBufferSystem.
     */

    @Override
    protected void create() {
        create(ItemRotationBufferSystem.class);
    }
}