package com.internal.bootstrap.worldpipeline.block;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.extras.Direction3Vector;

public class BlockHandle extends HandlePackage {

    /*
     * Persistent block definition record. Wraps BlockData and delegates all
     * access through it. Registered in BlockManager from bootstrap to shutdown.
     */

    // Internal
    private BlockData blockData;

    // Constructor \\

    public void constructor(BlockData blockData) {
        this.blockData = blockData;
    }

    // Accessible \\

    public BlockData getBlockData() {
        return blockData;
    }

    public String getBlockName() {
        return blockData.getBlockName();
    }

    public short getBlockID() {
        return blockData.getBlockID();
    }

    public DynamicGeometryType getGeometry() {
        return blockData.getGeometry();
    }

    public BlockRotationType getRotationType() {
        return blockData.getRotationType();
    }

    public int getMaterialID() {
        return blockData.getMaterialID();
    }

    public int getTextureForFace(Direction3Vector direction) {
        return blockData.getTextureForFace(direction);
    }

    public int getBreakTier() {
        return blockData.getBreakTier();
    }

    public short getRequiredToolTypeID() {
        return blockData.getRequiredToolTypeID();
    }

    public int getDurability() {
        return blockData.getDurability();
    }

    public boolean isUnbreakable() {
        return blockData.isUnbreakable();
    }
}