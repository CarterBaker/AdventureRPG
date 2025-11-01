package com.AdventureRPG.WorldSystem.Chunks;

import java.util.concurrent.atomic.AtomicBoolean;

import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Direction2Int;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.WorldSystem.WorldGenerator;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.BatchSystem.BatchSystem;
import com.AdventureRPG.WorldSystem.QueueSystem.QueueSystem;
import com.AdventureRPG.WorldSystem.QueueSystem.Queue.QueueProcess;
import com.AdventureRPG.WorldSystem.SubChunks.SubChunk;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class Chunk {

    // Game Manager
    public final WorldSystem worldSystem;
    public final WorldGenerator worldGenerator;
    private final BatchSystem batchSystem;
    public final QueueSystem queueSystem;

    // Settings
    private final int WORLD_HEIGHT;

    // Build
    public final ChunkMesh chunkMesh;
    private final Builder chunkBuilder;

    // Chunk
    public final long coordinate;
    public final int coordinateX, coordinateY;

    private ChunkState chunkState;

    // Queue
    private final AtomicBoolean[] processLocks;

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

    public Chunk(WorldSystem worldSystem, long coordinate, ChunkState chunkState) {

        // Game Manager
        this.worldSystem = worldSystem;
        this.worldGenerator = worldSystem.worldGenerator;
        this.batchSystem = worldSystem.batchSystem;
        this.queueSystem = worldSystem.queueSystem;

        // Settings
        this.WORLD_HEIGHT = GlobalConstant.WORLD_HEIGHT;

        // Build
        this.chunkBuilder = new Builder(worldSystem, this);
        this.chunkMesh = new ChunkMesh(this);

        // Chunk
        this.coordinate = coordinate;
        this.coordinateX = Coordinate2Int.unpackX(coordinate);
        this.coordinateY = Coordinate2Int.unpackY(coordinate);

        this.chunkState = chunkState;

        // Queue
        this.processLocks = new AtomicBoolean[ChunkState.values().length];
        processLocks[ChunkState.NEEDS_GENERATION_DATA.ordinal()] = new AtomicBoolean(false);
        processLocks[ChunkState.NEEDS_ASSESSMENT_DATA.ordinal()] = new AtomicBoolean(false);
        processLocks[ChunkState.NEEDS_BUILD_DATA.ordinal()] = new AtomicBoolean(false);
        processLocks[ChunkState.NEEDS_BATCH_DATA.ordinal()] = new AtomicBoolean(false);

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

        enqueue();
    }

    private long getWrappedNeighborCoordinate(Direction2Int direction) {

        long neighborCoordinate = Coordinate2Int.add(coordinate, direction.packed);

        neighborCoordinate = worldSystem.wrapAroundWorld(neighborCoordinate);

        return neighborCoordinate;
    }

    public void dispose() {

        if (subChunks == null)
            return;

        for (int i = 0; i < WORLD_HEIGHT; i++)
            if (subChunks[i] != null)
                subChunks[i].dispose();
    }

    // Position \\

    public void moveTo(long position) {

        this.position = position;
        this.positionX = Coordinate2Int.unpackX(position);
        this.positionY = Coordinate2Int.unpackY(position);
    }

    // Queue \\

    public void queueProcess(QueueProcess process) {

        AtomicBoolean lock = processLocks[chunkState.ordinal()];

        if (checkQueue(process, lock))
            finalizeQueue(executeQueue(process), process, lock);
    }

    private boolean checkQueue(QueueProcess process, AtomicBoolean lock) {

        if (!lock.compareAndSet(false, true) ||
                (process.previousState != null &&
                        chunkState != process.corrospondingState))
            return false;

        return true;
    }

    private boolean executeQueue(QueueProcess process) {

        switch (process) {

            case Generate:
                return processGenerationData();

            case Assessment:
                return processAssessmentData();

            case Build:
                return processBuildData();

            case Batch:
                return processBatchData();

            default:
                return false;
        }
    }

    private void finalizeQueue(boolean successful, QueueProcess process, AtomicBoolean lock) {

        if (successful)
            setState(process.nextState);

        enqueue();

        lock.set(false);
    }

    private void enqueue() {

        switch (chunkState) {

            case NEEDS_GENERATION_DATA:
                queueSystem.addToGenerateQueue(coordinate);
                break;

            case NEEDS_ASSESSMENT_DATA:
                queueSystem.addToAssessmentQueue(coordinate);
                break;

            case NEEDS_BUILD_DATA:
                queueSystem.addToBuildQueue(coordinate);
                break;

            case NEEDS_BATCH_DATA:
                queueSystem.addToBatchQueue(coordinate);
                break;

            case FINALIZED:
                batchSystem.addChunk(this);
                break;

            default:
                break;
        }
    }

    // Generation \\

    private boolean processGenerationData() {

        if (verifySubchunks())
            return true;

        // TODO: For error handling may want to make the generator return a boolean
        worldGenerator.generateChunk(this);

        return true;
    }

    public void generate(SubChunk[] subChunks) {
        this.subChunks = subChunks;
    }

    public boolean verifySubchunks() {

        for (int i = 0; i < GlobalConstant.WORLD_HEIGHT; i++)
            if (subChunks[i] == null)
                return false;

        return true;
    }

    // Assessment \\

    private boolean processAssessmentData() {

        for (Direction2Int direction : Direction2Int.values()) {

            long neighborCoordinate = neighborCoordinates[direction.index];
            Chunk neighbor = queueSystem.getChunkFromCoordinate(neighborCoordinate);

            if (neighbor == null || !neighbor.verifySubchunks())
                continue;

            if (neighbors[direction.index] != neighbor) {
                neighbors[direction.index] = neighbor;
                neighbor.assessChunk(this);
            }
        }

        updateNeighborStatus();

        return neighborStatus == NeighborStatus.COMPLETE;
    }

    private void updateNeighborStatus() {

        for (int i = 0; i < 8; i++) {

            Chunk neighbor = neighbors[i];

            if (neighbor == null || neighbor.getState() == ChunkState.NEEDS_GENERATION_DATA) {

                neighborStatus = NeighborStatus.INCOMPLETE;
                return;
            }
        }

        neighborStatus = NeighborStatus.COMPLETE;
        setState(ChunkState.NEEDS_BUILD_DATA);
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

    // Build \\

    private boolean processBuildData() {
        return build();
    }

    private boolean build() {

        for (int subChunkIndex = 0; subChunkIndex < WORLD_HEIGHT; subChunkIndex++)
            if (!buildSubChunk(subChunkIndex))
                return false;

        return true;
    }

    private boolean buildSubChunk(int subChunkIndex) {
        return chunkBuilder.build(subChunkIndex);
    }

    // Batch \\

    private boolean processBatchData() {

        chunkMesh.Clear();

        for (int subChunkIndex = 0; subChunkIndex < WORLD_HEIGHT; subChunkIndex++)
            chunkMesh.merge(subChunkIndex);

        return true;
    }

    // Gameplay \\

    public void placeBlock() {

    }

    public void breakBlock() {

    }

    // Accessible \\

    public SubChunk getSubChunk(int index) {
        return subChunks[index];
    }

    public Chunk getNeighborChunk(Direction2Int direction) {
        return neighbors[direction.index];
    }

    public ChunkState getState() {
        return chunkState;
    }

    private void setState(ChunkState chunkState) {
        this.chunkState = chunkState;
    }
}
