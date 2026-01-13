package com.internal.bootstrap.worldpipeline.block;

import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.Extras.Direction3Int;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BlockHandle extends HandlePackage {

    // Identity
    private String blockName;
    private int blockID;
    private BlockType blockType;

    // Textures
    private Object2IntOpenHashMap<Direction3Int> faceTextures;

    // Constructor \\

    public void constructor(
            String blockName,
            int blockID,
            BlockType blockType,
            int upTexture,
            int downTexture,
            int northTexture,
            int southTexture,
            int eastTexture,
            int westTexture) {

        this.blockName = blockName;
        this.blockID = blockID;
        this.blockType = blockType;

        this.faceTextures = new Object2IntOpenHashMap<>();
        this.faceTextures.put(Direction3Int.UP, upTexture);
        this.faceTextures.put(Direction3Int.DOWN, downTexture);
        this.faceTextures.put(Direction3Int.NORTH, northTexture);
        this.faceTextures.put(Direction3Int.SOUTH, southTexture);
        this.faceTextures.put(Direction3Int.EAST, eastTexture);
        this.faceTextures.put(Direction3Int.WEST, westTexture);
    }

    // Accessible \\

    public String getBlockName() {
        return blockName;
    }

    public int getBlockID() {
        return blockID;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public int getTextureForFace(Direction3Int direction) {
        return faceTextures.getInt(direction);
    }
}