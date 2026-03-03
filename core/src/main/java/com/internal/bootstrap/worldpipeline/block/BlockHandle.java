package com.internal.bootstrap.worldpipeline.block;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

public class BlockHandle extends HandlePackage {

    // Identity
    private String blockName;
    private short blockID; // short — matches BlockPaletteHandle storage
    private DynamicGeometryType geometry;

    // Textures
    private int materialID;
    private int[] faceTextures;
    private BlockRotationType rotationType;

    // Constructor \\

    public void constructor(
            String blockName,
            short blockID, // short — stable hash from RegistryUtility
            DynamicGeometryType geometry,
            BlockRotationType rotationType,
            int materialID,
            int northTexture, int eastTexture, int southTexture,
            int westTexture, int upTexture, int downTexture) {

        this.blockName = blockName;
        this.blockID = blockID;
        this.geometry = geometry;
        this.rotationType = rotationType;
        this.materialID = materialID;

        this.faceTextures = new int[Direction3Vector.LENGTH];
        this.faceTextures[Direction3Vector.NORTH.ordinal()] = northTexture;
        this.faceTextures[Direction3Vector.EAST.ordinal()] = eastTexture;
        this.faceTextures[Direction3Vector.SOUTH.ordinal()] = southTexture;
        this.faceTextures[Direction3Vector.WEST.ordinal()] = westTexture;
        this.faceTextures[Direction3Vector.UP.ordinal()] = upTexture;
        this.faceTextures[Direction3Vector.DOWN.ordinal()] = downTexture;
    }

    // Accessible \\

    public String getBlockName() {
        return blockName;
    }

    public short getBlockID() {
        return blockID;
    } // short — safe to write directly to chunk palette

    public DynamicGeometryType getGeometry() {
        return geometry;
    }

    public int getMaterialID() {
        return materialID;
    }

    public BlockRotationType getRotationType() {
        return rotationType;
    }

    public int getTextureForFace(Direction3Vector direction) {
        return faceTextures[direction.ordinal()];
    }
}