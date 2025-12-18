package com.AdventureRPG.WorldPipeline.chunks;

import java.util.concurrent.atomic.AtomicBoolean;

import com.AdventureRPG.WorldPipeline.WorldGenerator;
import com.AdventureRPG.WorldPipeline.WorldPipeline;
import com.AdventureRPG.WorldPipeline.queuesystem.QueueProcess;
import com.AdventureRPG.WorldPipeline.queuesystem.QueueSystem;
import com.AdventureRPG.WorldPipeline.subchunks.SubChunk;
import com.AdventureRPG.core.engine.EngineSetting;
import com.AdventureRPG.core.geometry.modelmanager.ModelManager;
import com.AdventureRPG.core.geometry.vaomanager.VAOHandle;
import com.AdventureRPG.core.geometry.vaomanager.VAOManager;
import com.AdventureRPG.core.scenepipeline.worldenginesystem.WorldEngineSystem;
import com.AdventureRPG.core.util.Methematics.Extras.Coordinate2Int;
import com.AdventureRPG.core.util.Methematics.Extras.Direction2Int;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class Chunk {

    // Game Manager
    public final ModelManager modelManager;
    public final WorldEngineSystem worldEngineSystem;
    public final WorldPipeline worldPipeline;
    public final WorldGenerator worldGenerator;
    public final QueueSystem queueSystem;

    // Settings
    private final int WORLD_HEIGHT;

    // Build
    public final VAOHandle vao;
    public final ChunkMeshData chunkMeshData;
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

    // Validation
    private boolean validSubChunks;
    private boolean validNeighbors;

    // Base \\

    public Chunk(
            VAOManager vaoManager,
            ModelManager modelManager,
            WorldEngineSystem worldEngineSystem,
            WorldPipeline worldPipeline,
            long coordinate) {
        this(
                vaoManager,
                modelManager,
                worldEngineSystem,
                worldPipeline,
                coordinate,
                ChunkState.NEEDS_GENERATION_DATA);
    }

    public Chunk(
            VAOManager vaoManager,
            ModelManager modelManager,
            WorldEngineSystem worldEngineSystem,
            WorldPipeline worldPipeline,
            long coordinate,
            ChunkState chunkState) {

        // Game Manager
        this.modelManager = modelManager;
        this.worldEngineSystem = worldEngineSystem;
        this.worldPipeline = worldPipeline;
        this.worldGenerator = worldPipeline.worldGenerator;
        this.queueSystem = worldPipeline.queueSystem;

        // Settings
        this.WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;

        // Build
        this.vao = vaoManager.getVAOHandleFromName("Stride9");
        this.chunkBuilder = new Builder(worldPipeline, this);
        this.chunkMeshData = new ChunkMeshData(
                vao,
                modelManager,
                this);

        // Chunk
        this.coordinate = coordinate;
        this.coordinateX = Coordinate2Int.unpackX(coordinate);
        this.coordinateY = Coordinate2Int.unpackY(coordinate);

        this.chunkState = chunkState;

        // Queue
        this.processLocks = new AtomicBoolean[ChunkState.values().length];

        for (int i = 0; i < processLocks.length; i++)
            processLocks[i] = new AtomicBoolean(false);

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

        // Validation
        this.validSubChunks = false;
        this.validNeighbors = false;
    }

    private long getWrappedNeighborCoordinate(Direction2Int direction) {

        long neighborCoordinate = Coordinate2Int.add(coordinate, direction.packed);

        neighborCoordinate = worldEngineSystem.wrapAroundWorld(neighborCoordinate);

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

    // Main \\

    public void addChunkToQueue() {
        queueSystem.addChunkToQueue(this);
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
            chunkState = process.nextState;

        enqueue();

        lock.set(false);
    }

    public void enqueue() {

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
                queueSystem.requestBatch(this);
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

        if (validSubChunks)
            return true;

        if (subChunks == null || subChunks.length != WORLD_HEIGHT)
            return false;

        for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++)
            if (subChunks[i] == null)
                return false;

        validSubChunks = true;

        return true;
    }

    // Assessment \\

    private boolean processAssessmentData() {

        // Since we assess neighbors as we find them some may be completed already
        if (neighborStatus == NeighborStatus.COMPLETE)
            return true;

        for (Direction2Int direction : Direction2Int.values()) {

            long neighborCoordinate = neighborCoordinates[direction.index];
            Chunk neighbor = queueSystem.getChunkFromCoordinate(neighborCoordinate);

            if (neighbor == null || !neighbor.verifySubchunks())
                continue;

            if (neighbors[direction.index] != neighbor) {
                neighbors[direction.index] = neighbor;
                neighbor.assessNeighborChunk(this);
            }
        }

        updateNeighborStatus();

        return neighborStatus == NeighborStatus.COMPLETE;
    }

    public void assessNeighborChunk(Chunk chunk) {

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

        if (verifyNeighbors())
            neighborStatus = NeighborStatus.COMPLETE;
    }

    private boolean verifyNeighbors() {

        if (validNeighbors)
            return true;

        if (neighbors == null || neighbors.length != 8)
            return false;

        for (int i = 0; i < 8; i++) {

            Chunk neighbor = neighbors[i];

            if (neighbor == null)
                return false;

            if (!neighbor.verifySubchunks())
                return false;
        }

        validNeighbors = true;

        return true;
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

        chunkMeshData.Clear();

        for (int subChunkIndex = 0; subChunkIndex < WORLD_HEIGHT; subChunkIndex++)
            chunkMeshData.merge(subChunkIndex);

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
}
