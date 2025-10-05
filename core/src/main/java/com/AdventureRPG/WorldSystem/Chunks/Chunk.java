package com.AdventureRPG.WorldSystem.Chunks;

import java.util.concurrent.atomic.AtomicBoolean;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Direction2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.GridSystem.GridSystem;
import com.AdventureRPG.WorldSystem.SubChunks.SubChunk;
import com.AdventureRPG.WorldSystem.Util.MeshPacket;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class Chunk {

    // Game Manager
    public final Settings settings;
    public final WorldSystem worldSystem;
    public final GridSystem gridSystem;

    // Settings
    private final int WORLD_HEIGHT;

    // Build
    private final ChunkBuilder chunkBuilder;
    private final ChunkMesh chunkMesh;

    // Chunk
    public final long coordinate;
    public final int coordinateX, coordinateY;

    private ChunkState state;

    private final AtomicBoolean enqueueGenerate;
    private final AtomicBoolean enqueueAssessment;
    private final AtomicBoolean enqueueBuild;

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

    // Base \\

    public Chunk(WorldSystem worldSystem, long coordinate) {
        this(worldSystem, coordinate, ChunkState.NEEDS_GENERATION_DATA);
    }

    public Chunk(WorldSystem worldSystem, long coordinate, ChunkState state) {

        // Game Manager
        this.settings = worldSystem.settings;
        this.worldSystem = worldSystem;
        this.gridSystem = worldSystem.gridSystem;

        // Settings
        this.WORLD_HEIGHT = settings.WORLD_HEIGHT;

        // Build
        this.chunkBuilder = new ChunkBuilder(worldSystem, this);
        this.chunkMesh = new ChunkMesh(this);

        // Chunk
        this.coordinate = coordinate;
        this.coordinateX = Coordinate2Int.unpackX(coordinate);
        this.coordinateY = Coordinate2Int.unpackY(coordinate);

        this.state = state;

        this.enqueueGenerate = new AtomicBoolean(false);
        this.enqueueAssessment = new AtomicBoolean(false);
        this.enqueueBuild = new AtomicBoolean(false);

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
        neighborCoordinate = worldSystem.wrapAroundWorld(neighborCoordinate);

        return neighborCoordinate;
    }

    public void dispose() {

        if (subChunks == null)
            return;

        for (int i = 0; i < settings.WORLD_HEIGHT; i++)
            if (subChunks[i] != null)
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

        enqueueGenerate.set(false);

        if (needsGenerationData())
            return;

        setState(ChunkState.NEEDS_ASSESSMENT_DATA);
    }

    public SubChunk getSubChunk(int index) {
        return subChunks[index];
    }

    private boolean needsGenerationData() {

        if (subChunks == null) {

            setState(ChunkState.NEEDS_GENERATION_DATA);

            return true;
        }

        return false;
    }

    // Build \\

    public void build() {

        if (needsGenerationData())
            return;

        chunkMesh.Clear();

        for (int subChunkIndex = 0; subChunkIndex < settings.WORLD_HEIGHT; subChunkIndex++) {

            buildSubChunk(subChunkIndex);
            chunkMesh.merge(subChunkIndex);
        }
    }

    private void buildSubChunk(int subChunkIndex) {

        chunkBuilder.build(subChunkIndex);
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

    public void assessNeighbors() {

        for (Direction2Int direction : Direction2Int.values()) {

            long neighborCoordinate = neighborCoordinates[direction.index];
            Chunk neighbor = gridSystem.getChunkFromCoordinate(neighborCoordinate);

            if (neighbor == null || neighbor.state == ChunkState.NEEDS_GENERATION_DATA)
                continue;

            if (neighbors[direction.index] != neighbor) {

                neighbors[direction.index] = neighbor;
                neighbor.assessChunk(this);
            }
        }

        enqueueAssessment.set(false);

        updateNeighborStatus();
    }

    public void assessChunk(Chunk chunk) {

        if (neighborStatus == NeighborStatus.COMPLETE)
            return;

        int index = coordToIndex.getOrDefault(chunk.coordinate, -1);

        if (index == -1)
            return;

        if (neighbors[index] != chunk) {

            neighbors[index] = chunk;
            updateNeighborStatus();
        }
    }

    private void updateNeighborStatus() {

        // Keep track so there is no chance of calling setState() twice
        boolean needsBuildData = false;

        if (neighborStatus == NeighborStatus.INCOMPLETE) {

            for (int i = 0; i < 4; i++)
                if (neighbors[i] == null || neighbors[i].getState() == ChunkState.NEEDS_GENERATION_DATA)
                    return;

            needsBuildData = true;
            neighborStatus = NeighborStatus.PARTIAL;
        }

        if (neighborStatus == NeighborStatus.PARTIAL) {

            for (int i = 4; i < 8; i++)
                if (neighbors[i] == null || neighbors[i].getState() == ChunkState.NEEDS_GENERATION_DATA)
                    return;

            needsBuildData = true;
            neighborStatus = NeighborStatus.COMPLETE;
        }

        if (needsBuildData)
            setState(ChunkState.NEEDS_BUILD_DATA);
    }

    public enum NeighborStatus {
        INCOMPLETE,
        PARTIAL,
        COMPLETE
    }

    // Accessible \\

    public void enqueue() {

        switch (state) {

            case NEEDS_GENERATION_DATA:

                if (enqueueGenerate.compareAndSet(false, true))
                    gridSystem.addToGenerateQueue(coordinate);

                break;

            case NEEDS_ASSESSMENT_DATA:

                if (enqueueAssessment.compareAndSet(false, true))
                    gridSystem.addToAssessmentQueue(coordinate);

                break;

            case NEEDS_BUILD_DATA:

                if (enqueueBuild.compareAndSet(false, true))
                    gridSystem.addToBuildQueue(coordinate);

                break;

            case FINALIZED:
                break;

            default:
                break;
        }
    }

    public MeshPacket meshPacket() {
        return chunkMesh.meshPacket();
    }

    // Gameplay \\

    public void placeBlock() {

    }

    public void breakBlock() {

    }
}
