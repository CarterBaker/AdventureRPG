package com.AdventureRPG.WorldSystem.QueueSystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.GlobalConstant;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class Grid {

    // Debug
    private final boolean debug = false; // TODO: Debug line

    // Game Manager
    private final Settings settings;
    private final QueueSystem gridSystem;

    // Settings
    private int maxRenderDistance;

    // Chunk Tracking
    private int totalChunks;
    private long[] loadOrder;
    private LongOpenHashSet gridCoordinates;
    private Long2ObjectOpenHashMap<BoundingBox> chunkBounds;

    // Temp
    private List<ChunkDistance> list;

    // Base \\

    public Grid(QueueSystem gridSystem) {

        // Chunk System
        this.settings = gridSystem.settings;
        this.gridSystem = gridSystem;

        // Settings
        this.maxRenderDistance = settings.maxRenderDistance;

        // Grid
        this.totalChunks = maxRenderDistance * maxRenderDistance;
        this.loadOrder = new long[totalChunks];
        this.gridCoordinates = new LongOpenHashSet(totalChunks);
        this.chunkBounds = new Long2ObjectOpenHashMap<>(totalChunks);

        // Temp
        this.list = new ArrayList<>();
    }

    // Grid \\

    // Main rebuild method
    public void buildGrid() {

        maxRenderDistance = settings.maxRenderDistance;
        float radius = calculateRadius();

        List<ChunkDistance> tempList = collectChunkDistances(radius);

        totalChunks = tempList.size();
        initializeDataStructures(totalChunks);
        fillGridCoordinates(tempList);
        assignLoadOrder(tempList);
        createBoundingBoxes();

        list.clear();
    }

    // Calculate radius
    private float calculateRadius() {
        return settings.maxRenderDistance / 2f;
    }

    // Collect all coordinates inside a circle
    private List<ChunkDistance> collectChunkDistances(float radius) {

        float radiusSquared = radius * radius;
        list.clear();

        for (int x = -(maxRenderDistance / 2); x < maxRenderDistance / 2; x++) {

            for (int y = -(maxRenderDistance / 2); y < maxRenderDistance / 2; y++) {

                float distSquared = (x * x) + (y * y);

                if (distSquared <= radiusSquared)
                    list.add(new ChunkDistance(Coordinate2Int.pack(x, y), distSquared));
            }
        }

        return list;
    }

    // Inner class
    private static class ChunkDistance {

        long coord;
        float distSquared;

        ChunkDistance(long c, float d2) {

            coord = c;
            distSquared = d2;
        }
    }

    // Initialize all data structures
    private void initializeDataStructures(int totalChunks) {

        // Chunk Tracking
        this.loadOrder = new long[totalChunks];
        this.gridCoordinates = new LongOpenHashSet(totalChunks);
        this.chunkBounds = new Long2ObjectOpenHashMap<>(totalChunks);
    }

    // Fill gridCoordinates set
    private void fillGridCoordinates(List<ChunkDistance> tempList) {

        for (ChunkDistance cd : tempList)
            gridCoordinates.add(cd.coord);
    }

    // Sort by distance and assign load order
    private void assignLoadOrder(List<ChunkDistance> tempList) {

        tempList.sort(Comparator.comparingDouble(cd -> cd.distSquared));

        for (int i = 0; i < tempList.size(); i++)
            loadOrder[i] = tempList.get(i).coord;
    }

    private void createBoundingBoxes() {

        int CHUNK_SIZE = GlobalConstant.CHUNK_SIZE;
        int WORLD_HEIGHT = GlobalConstant.WORLD_HEIGHT;

        for (Long gridCoordinate : gridCoordinates) {

            int gridX = Coordinate2Int.unpackX(gridCoordinate);
            int gridZ = Coordinate2Int.unpackY(gridCoordinate);

            int worldX = gridX * CHUNK_SIZE;
            int worldZ = gridZ * CHUNK_SIZE;

            Vector3 min = new Vector3(
                    worldX,
                    0,
                    worldZ);
            Vector3 max = new Vector3(
                    worldX + CHUNK_SIZE,
                    WORLD_HEIGHT * CHUNK_SIZE,
                    worldZ + CHUNK_SIZE);

            BoundingBox boundingBox = new BoundingBox(min, max);
            chunkBounds.putIfAbsent(gridCoordinate, boundingBox);
        }
    }

    // Accessible \\

    public int totalChunks() {
        return totalChunks;
    }

    public long loadOrder(int i) {
        return loadOrder[i];
    }

    public BoundingBox getChunkBounds(long gridCoordinate) {
        return chunkBounds.get(gridCoordinate);
    }

    // Debug \\

    private void debug() { // TODO: Debug line

    }
}
