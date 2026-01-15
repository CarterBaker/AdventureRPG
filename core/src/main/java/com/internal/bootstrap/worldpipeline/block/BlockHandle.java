package com.internal.bootstrap.worldpipeline.block;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometry;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BlockHandle extends HandlePackage {

    // Identity
    private String blockName;
    private int blockID;
    private DynamicGeometry geometry;

    // Textures
    private Object2IntOpenHashMap<Direction3Vector> faceTextures;

    // Constructor \\

    public void constructor(
            String blockName,
            int blockID,
            DynamicGeometry geometry,
            int upTexture,
            int downTexture,
            int northTexture,
            int southTexture,
            int eastTexture,
            int westTexture) {

        this.blockName = blockName;
        this.blockID = blockID;
        this.geometry = geometry;

        this.faceTextures = new Object2IntOpenHashMap<>();
        this.faceTextures.put(Direction3Vector.UP, upTexture);
        this.faceTextures.put(Direction3Vector.DOWN, downTexture);
        this.faceTextures.put(Direction3Vector.NORTH, northTexture);
        this.faceTextures.put(Direction3Vector.SOUTH, southTexture);
        this.faceTextures.put(Direction3Vector.EAST, eastTexture);
        this.faceTextures.put(Direction3Vector.WEST, westTexture);
    }

    // Accessible \\

    public String getBlockName() {
        return blockName;
    }

    public int getBlockID() {
        return blockID;
    }

    public DynamicGeometry getGeometry() {
        return geometry;
    }

    public int getTextureForFace(Direction3Vector direction) {
        return faceTextures.getInt(direction);
    }
}