package com.internal.bootstrap.physicspipeline.util;

import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.core.engine.StructPackage;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

public class BlockCastStruct extends StructPackage {

    public boolean hit;
    public float distance;
    public Direction3Vector hitFace;

    public long chunkCoordinate;
    public int subChunkY;

    public BlockHandle block;
    public int blockX;
    public int blockY;
    public int blockZ;

    public int hitSubX;
    public int hitSubY;
    public int hitSubZ;
}