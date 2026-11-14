package application.bootstrap.entitypipeline.pathfindingmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.pathfinding.PathStruct;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;

public class PathfindingManager extends ManagerPackage {

    /*
     * Finds the way through the block grid for whatever walks it without a
     * player behind it. findPath() is the one place a way is found: it
     * searches from where an entity stands toward a goal on the ground of a
     * chunk and writes the way into that walker's PathStruct.
     * PathTerrainBranch reads the world into where a body of the entity's
     * height can stand, climb and drop, and PathSearchBranch runs A* over it,
     * bounded in reach and in the nodes it may open so no search costs more
     * than its share of a frame. At most PATH_SEARCHES_PER_FRAME searches run
     * in a frame; a walker refused one keeps walking what it has and asks
     * again the next.
     */

    // Branches
    private PathSearchBranch pathSearchBranch;

    // Budget
    private int searchesLeft;

    // Base \\

    @Override
    protected void create() {

        // Branches
        create(PathTerrainBranch.class);
        this.pathSearchBranch = create(PathSearchBranch.class);
    }

    // Update \\

    @Override
    protected void update() {
        searchesLeft = EngineSetting.PATH_SEARCHES_PER_FRAME;
    }

    // Search \\

    // False when no search is left this frame — the path is left as it was
    public boolean findPath(
            EntityInstance entity,
            long goalChunkCoordinate,
            float goalX,
            float goalY,
            float goalZ,
            PathStruct path) {

        if (searchesLeft <= 0)
            return false;

        searchesLeft--;
        pathSearchBranch.search(entity, goalChunkCoordinate, goalX, goalY, goalZ, path);

        return true;
    }
}
