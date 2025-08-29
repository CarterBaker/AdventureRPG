package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Direction2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.GridSystem.GridSystem;
import com.badlogic.gdx.graphics.g3d.Model;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class Chunk {

    // Game Manager
    public final Settings settings;
    public final WorldSystem worldSystem;
    public final ChunkSystem chunkSystem;
    public final GridSystem gridSystem;

    // Chunk
    public final long coordinate;
    public final int coordinateX, coordinateY;

    // Position
    public long position;
    public int positionX, positionY;

    // Neighbors
    private boolean hasCardinalNeighbors;
    private boolean hasAllNeighbors;
    private final Long2IntOpenHashMap coordToIndex;
    public final long[] neighborCoordinates;
    private Chunk[] neighbors;

    // Data
    private boolean hasData;
    public SubChunk[] subChunks;

    // Mesh
    public Model model;

    // Base \\

    public Chunk(WorldSystem worldSystem, long coordinate) {

        // Game Manager
        this.settings = worldSystem.settings;
        this.worldSystem = worldSystem;
        this.chunkSystem = worldSystem.chunkSystem;
        this.gridSystem = worldSystem.gridSystem;

        // Chunk
        this.coordinate = coordinate;
        this.coordinateX = Coordinate2Int.unpackX(coordinate);
        this.coordinateY = Coordinate2Int.unpackY(coordinate);

        // Neighbors
        this.hasCardinalNeighbors = false;
        this.hasAllNeighbors = false;
        coordToIndex = new Long2IntOpenHashMap(8);
        neighborCoordinates = new long[8];
        neighbors = new Chunk[8];

        for (Direction2Int direction : Direction2Int.values()) {
            long neighborCoordinate = getWrappedNeighborCoordinate(direction);
            neighborCoordinates[direction.index] = neighborCoordinate;
            coordToIndex.put(neighborCoordinate, direction.index);
        }

        // Data
        this.hasData = false;
    }

    private long getWrappedNeighborCoordinate(Direction2Int direction) {

        long neighborCoordinate = Coordinate2Int.add(coordinate, direction.packed);
        worldSystem.wrapAroundWorld(neighborCoordinate);

        return neighborCoordinate;
    }

    public void dispose() {
    }

    // Position \\

    public void moveTo(long position) {

        this.position = position;
        this.positionX = Coordinate2Int.unpackX(position);
        this.positionY = Coordinate2Int.unpackY(position);
    }

    // Data \\

    public void generate(SubChunk[] subChunks) {

        this.subChunks = subChunks;
        hasData = true;
    }

    public boolean hasData() {
        return hasData;
    }

    public SubChunk getSubChunk(int index) {
        return subChunks[index];
    }

    // Mesh \\

    public void build() {

        for (int subChunkIndex = 0; subChunkIndex < settings.WORLD_HEIGHT; subChunkIndex++)
            rebuild(subChunkIndex);
    }

    public void rebuild(int subChunkIndex) {

        chunkSystem.chunkBuilder.build(this, subChunkIndex);
    }

    public void buildChunkMesh(Model model) {

        assignModel(model);
    }

    public void shiftChunkMesh(Model model) {

        assignModel(model);
    }

    private void assignModel(Model model) {
        this.model = model;
    }

    // Neighbors \\

    public boolean hasCardinalNeighbors() {
        return hasCardinalNeighbors;
    }

    public boolean hasAllNeighbors() {
        return hasAllNeighbors;
    }

    public long getNeighborCoordinate(Direction2Int direction) {
        return neighborCoordinates[direction.index];
    }

    public Chunk getNeighborChunk(Direction2Int direction) {
        return neighbors[direction.index];
    }

    public boolean assessNeighbors() {

        for (Direction2Int direction : Direction2Int.values()) {

            long neighborCoordinate = neighborCoordinates[direction.index];
            Chunk neighbor = gridSystem.getChunkFromCoordinate(neighborCoordinate);

            if (neighbor != null)
                neighbors[direction.index] = neighbor;
        }

        updateNeighborStatus();

        return hasAllNeighbors;
    }

    private void updateNeighborStatus() {

        if (!hasCardinalNeighbors) {

            for (int i = 0; i < 4; i++)

                if (neighbors[i] == null)
                    return;

            hasCardinalNeighbors = true;
            gridSystem.addToBuildQueue(this);
        }

        else
            gridSystem.addToAssessmentQueue(this);

        if (!hasAllNeighbors) {

            for (int i = 4; i < 8; i++)

                if (neighbors[i] == null)
                    return;

            hasAllNeighbors = true;
            gridSystem.addToBuildQueue(this);
        }

        else
            gridSystem.addToAssessmentQueue(this);
    }

    // Gameplay \\

    public void placeBlock() {

    }

    public void breakBlock() {

    }
}
