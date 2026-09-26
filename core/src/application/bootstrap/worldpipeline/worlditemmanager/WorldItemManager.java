package application.bootstrap.worldpipeline.worlditemmanager;

import engine.root.ManagerPackage;

public class WorldItemManager extends ManagerPackage {

    /*
     * Owns the world item systems. WorldItemRenderSystem keeps each item's
     * composite buffer and pushes it into its grid's world target every
     * frame; WorldItemPlacementSystem places, removes and raycasts items per
     * chunk and forwards every change to the render system.
     */

    // Base \\

    @Override
    protected void create() {
        create(WorldItemRenderSystem.class);
        create(WorldItemPlacementSystem.class);
    }
}
