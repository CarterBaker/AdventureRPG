package com.internal.bootstrap.physicspipeline.util;

import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.core.engine.StructPackage;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

public class BlockCastStruct extends StructPackage {

    public boolean hit;
    public float distance;

    public long chunkCoordinate;
    public int subChunkY;

    public int blockX;
    public int blockY;
    public int blockZ;

    public Direction3Vector hitFace;
    public BlockHandle block;
}