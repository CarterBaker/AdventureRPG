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
    private ChunkState state;

    // Position
    public long position;
    public int positionX, positionY;

    // Neighbors
    private NeighborStatus neighborStatus;
    private final Long2IntOpenHashMap coordToIndex;
    public final long[] neighborCoordinates;
    private Chunk[] neighbors;

    // Data
    public SubChunk[] subChunks;

    // Mesh
    private Model model;

    // Base \\

    public Chunk(WorldSystem worldSystem, long coordinate) {
        this(worldSystem, coordinate, ChunkState.NEEDS_GENERATION_DATA);
    }

    public Chunk(WorldSystem worldSystem, long coordinate, ChunkState state) {

        // Game Manager
        this.settings = worldSystem.settings;
        this.worldSystem = worldSystem;
        this.chunkSystem = worldSystem.chunkSystem;
        this.gridSystem = worldSystem.gridSystem;

        // Chunk
        this.coordinate = coordinate;
        this.coordinateX = Coordinate2Int.unpackX(coordinate);
        this.coordinateY = Coordinate2Int.unpackY(coordinate);
        this.state = state;

        // Neighbors
        this.neighborStatus = NeighborStatus.INCOMPLETE;
        coordToIndex = new Long2IntOpenHashMap(8);
        neighborCoordinates = new long[8];
        neighbors = new Chunk[8];

        for (Direction2Int direction : Direction2Int.values()) {
            long neighborCoordinate = getWrappedNeighborCoordinate(direction);
            neighborCoordinates[direction.index] = neighborCoordinate;
            coordToIndex.put(neighborCoordinate, direction.index);
        }
    }

    private long getWrappedNeighborCoordinate(Direction2Int direction) {

        long neighborCoordinate = Coordinate2Int.add(coordinate, direction.packed);
        worldSystem.wrapAroundWorld(neighborCoordinate);

        return neighborCoordinate;
    }

    public void dispose() {

        if (subChunks != null)
            for (int i = 0; i < settings.WORLD_HEIGHT; i++)
                subChunks[i].dispose();
    }

    // Chunk \\

    public ChunkState getState() {
        return state;
    }

    private void setState(ChunkState state) {

        this.state = state;
        enqueue();
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
        setState(ChunkState.NEEDS_ASSESSMENT_DATA);
    }

    public SubChunk getSubChunk(int index) {
        return subChunks[index];
    }

    // Mesh \\

    public void build() {

        for (int subChunkIndex = 0; subChunkIndex < settings.WORLD_HEIGHT; subChunkIndex++)
            rebuild(subChunkIndex);

        setState(ChunkState.FINALIZED);
    }

    public void rebuild(int subChunkIndex) {

        chunkSystem.chunkBuilder.build(this, subChunkIndex);
    }

    public void buildChunkMesh(Model model) {

        this.model = model;

        for (int i = 0; i < settings.WORLD_HEIGHT; i++) {
            rebuildSubChunk(i);
            subChunks[i].build(model);
        }
    }

    public void shiftChunkMesh(Model model) {

        this.model = model;

        for (int i = 0; i < settings.WORLD_HEIGHT; i++)
            subChunks[i].build(model);
    }

    private void rebuildSubChunk(int subChunkIndex) {
        subChunks[subChunkIndex].build();
    }

    // Neighbors \\

    public NeighborStatus getNeighborStatus() {
        return neighborStatus;
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

        return neighborStatus == NeighborStatus.COMPLETE;
    }

    private void updateNeighborStatus() {

        if (neighborStatus == NeighborStatus.INCOMPLETE) {

            for (int i = 0; i < 4; i++)

                if (neighbors[i] == null)
                    return;

            neighborStatus = NeighborStatus.PARTIAL;
            setState(ChunkState.NEEDS_BUILD_DATA);
        }

        if (neighborStatus == NeighborStatus.PARTIAL) {

            for (int i = 4; i < 8; i++)

                if (neighbors[i] == null)
                    return;

            neighborStatus = NeighborStatus.COMPLETE;
        }
    }

    // Accessible \\

    public void enqueue() {

        switch (state) {

            case NEEDS_GENERATION_DATA:

                gridSystem.addToGenerateQueue(this);

                break;

            case NEEDS_ASSESSMENT_DATA:

                gridSystem.addToAssessmentQueue(this);

                break;

            case NEEDS_BUILD_DATA:

                gridSystem.addToBuildQueue(this);

                if (neighborStatus != NeighborStatus.COMPLETE)
                    gridSystem.addToAssessmentQueue(this);

                break;

            case FINALIZED:
                break;

            default:
                break;
        }
    }

    // Gameplay \\

    public void placeBlock() {

    }

    public void breakBlock() {

    }
}
