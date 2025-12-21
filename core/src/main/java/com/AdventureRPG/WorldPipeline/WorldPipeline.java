package com.AdventureRPG.WorldPipeline;

import com.AdventureRPG.WorldPipeline.batchsystem.BatchSystem;
import com.AdventureRPG.WorldPipeline.biomes.BiomeSystem;
import com.AdventureRPG.WorldPipeline.blocks.BlockSystem;
import com.AdventureRPG.WorldPipeline.queuesystem.QueueSystem;
import com.AdventureRPG.WorldPipeline.util.PackedCoordinate3Int;
import com.AdventureRPG.core.engine.PipelineFrame;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector2Int;
import com.badlogic.gdx.math.Vector3;

public class WorldPipeline extends PipelineFrame {

    // World System
    public BlockSystem blockSystem;
    public PackedCoordinate3Int packedCoordinate3Int;
    public WorldGenerator worldGenerator;
    public WorldTick worldTick;
    public WorldReader worldReader;
    public QueueSystem queueSystem;
    public BatchSystem batchSystem;
    public BiomeSystem biomeSystem;

    // Position
    private Vector3 currentPosition;
    private Vector2Int chunkCoordinate;

    // Base \\

    @Override
    protected void create() {

        // World System
        this.blockSystem = (BlockSystem) register(new BlockSystem());
        this.packedCoordinate3Int = (PackedCoordinate3Int) register(new PackedCoordinate3Int());
        this.worldGenerator = (WorldGenerator) register(new WorldGenerator());
        this.worldTick = (WorldTick) register(new WorldTick());
        this.worldReader = (WorldReader) register(new WorldReader());
        this.queueSystem = (QueueSystem) register(new QueueSystem());
        this.biomeSystem = (BiomeSystem) register(new BiomeSystem());

        // Position
        this.currentPosition = new Vector3();
        this.chunkCoordinate = new Vector2Int();
    }

    // Movement \\

    public Vector3 position() {
        return currentPosition;
    }

    public Vector2Int chunk() {
        return chunkCoordinate;
    }

    public void updatePosition(Vector3 currentPosition, Vector2Int chunkCoordinate) {

        this.currentPosition.set(currentPosition);

        if (this.chunkCoordinate.equals(chunkCoordinate))
            return;

        this.chunkCoordinate.set(chunkCoordinate);

        loadChunks();
    }

    public void loadChunks() {
        queueSystem.updateChunksInGrid(chunkCoordinate);
    }

    // Accessible \\

    public void rebuildGrid() {

        queueSystem.rebuildGrid();
    }
}
