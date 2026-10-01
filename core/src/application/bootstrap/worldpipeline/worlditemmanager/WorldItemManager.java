package application.bootstrap.worldpipeline.worlditemmanager;

import engine.root.ManagerPackage;

public class WorldItemManager extends ManagerPackage {

    /*
     * Owns the world item systems. WorldItemRenderSystem keeps each item's
     * composite buffer and pushes it into its grid's world target every
     * frame; WorldItemSpaceSystem answers every question about the space
     * items claim — fit, placement, collision, raycasts and shelter;
     * WorldItemPlacementSystem places, removes and opens items per chunk and
     * forwards every change to the render system.
     */

    // Base \\

    @Override
    protected void create() {
        create(WorldItemRenderSystem.class);
        create(WorldItemSpaceSystem.class);
        create(WorldItemPlacementSystem.class);
    }
}
