package application.bootstrap.mappipeline.mapmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.mappipeline.map.MapMarkerStruct;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class MapMarkerBranch extends BranchPackage {

    /*
     * Main thread — resolves where the players stand for any map: one marker
     * per streaming grid with a focal entity, at that entity's position and
     * facing its window's camera across the ground. Every preview and Dev
     * window shares one world, so every map marks every player the same way.
     * A marker stands at the world's own position, on a globe the face cell
     * the grid's chart lays the entity on, its facing turned into that face's
     * axes.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private PlayerManager playerManager;

    // Scratch
    private final WorldPositionStruct canonicalScratch = new WorldPositionStruct();
    private final GlobeFrameStruct frameScratch = new GlobeFrameStruct();

    // Base \\

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.playerManager = get(PlayerManager.class);
    }

    // Markers \\

    // Fills the markers from the front, growing the list as needed, and returns how many it filled
    int resolveMarkers(WorldHandle worldHandle, ObjectArrayList<MapMarkerStruct> markers) {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        int count = 0;

        for (int i = 0; i < grids.size(); i++) {

            GridInstance grid = grids.get(i);

            if (grid.getFocalEntity() == null || grid.getWorldHandle() != worldHandle)
                continue;

            if (count == markers.size())
                markers.add(new MapMarkerStruct());

            resolveMarker(grid, markers.get(count++));
        }

        return count;
    }

    void resolveMarker(GridInstance grid, MapMarkerStruct marker) {

        EntityInstance focalEntity = grid.getFocalEntity();

        WorldPositionUtility.toCanonical(grid, focalEntity.getWorldPositionStruct(), frameScratch, canonicalScratch);

        long chunkCoordinate = canonicalScratch.getChunkCoordinate();
        Vector3 local = canonicalScratch.getPosition();
        int turns = frameScratch.getTurns();

        marker.setPosition(
                grid,
                (double) Coordinate2Long.unpackX(chunkCoordinate) * EngineSetting.CHUNK_SIZE + local.x,
                (double) Coordinate2Long.unpackY(chunkCoordinate) * EngineSetting.CHUNK_SIZE + local.z);

        WindowInstance gridWindow = grid.getWindowInstance();

        if (gridWindow == null || !playerManager.hasPlayerForWindow(gridWindow.getWindowID()))
            return;

        Vector3 direction = playerManager.getCameraForWindow(gridWindow.getWindowID()).getDirection();
        float length = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        if (length > 0f)
            marker.setHeading(
                    (float) GlobeUtility.turnX(direction.x / length, direction.z / length, turns),
                    (float) GlobeUtility.turnZ(direction.x / length, direction.z / length, turns));
    }
}
