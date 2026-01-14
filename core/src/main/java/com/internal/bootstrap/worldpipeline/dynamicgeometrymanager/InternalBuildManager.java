package com.internal.bootstrap.worldpipeline.dynamicgeometrymanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.dynamicgeometrymanager.dynamicgeometry.BlockGeometryBranch;
import com.internal.bootstrap.worldpipeline.dynamicgeometrymanager.dynamicgeometry.FurnitureGeometryBranch;
import com.internal.bootstrap.worldpipeline.dynamicgeometrymanager.dynamicgeometry.LiquidGeometryBranch;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.ManagerPackage;

class InternalBuildManager extends ManagerPackage {

    // Internal
    private BlockGeometryBranch blockGeometryBranch;
    private FurnitureGeometryBranch furnitureGeometryBranch;
    private LiquidGeometryBranch liquidGeometryBranch;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.blockGeometryBranch = create(BlockGeometryBranch.class);
        this.furnitureGeometryBranch = create(FurnitureGeometryBranch.class);
        this.liquidGeometryBranch = create(LiquidGeometryBranch.class);
    }

    // Geometry Builder \\

    public boolean build(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance) {
        return true;
    }
}
