package application.bootstrap.worldpipeline.block;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;

public class BlockData extends DataPackage {

    /*
     * Persistent block definition record. All fields are immutable after
     * construction — block definitions never change at runtime. Owned by
     * BlockHandle for the full engine session. viscosity is required (Pa·s)
     * for any LIQUID-geometry block — see BlockBuilder — and unused/optional
     * for every other geometry type.
     */

    // Identity
    private final String blockName;
    private final short blockID;
    private final DynamicGeometryType geometry;
    private final BlockRotationType rotationType;

    // Rendering
    private final int materialID;
    private final int[] faceTextures;

    // Breaking
    private final int breakTier;
    private final short requiredToolTypeID;
    private final int durability;

    // Physics
    private final float viscosity;

    // Constructor \\

    public BlockData(
            String blockName,
            short blockID,
            DynamicGeometryType geometry,
            BlockRotationType rotationType,
            int materialID,
            int northTexture, int eastTexture, int southTexture,
            int westTexture, int upTexture, int downTexture,
            int breakTier,
            short requiredToolTypeID,
            int durability,
            float viscosity) {

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

        this.breakTier = breakTier;
        this.requiredToolTypeID = requiredToolTypeID;
        this.durability = durability;

        this.viscosity = viscosity;
    }

    // Accessible \\

    public String getBlockName() {
        return blockName;
    }

    public short getBlockID() {
        return blockID;
    }

    public DynamicGeometryType getGeometry() {
        return geometry;
    }

    public BlockRotationType getRotationType() {
        return rotationType;
    }

    public int getMaterialID() {
        return materialID;
    }

    public int getTextureForFace(Direction3Vector direction) {
        return faceTextures[direction.ordinal()];
    }

    public int getBreakTier() {
        return breakTier;
    }

    public short getRequiredToolTypeID() {
        return requiredToolTypeID;
    }

    public int getDurability() {
        return durability;
    }

    public boolean isUnbreakable() {
        return breakTier < 0;
    }

    public float getViscosity() {
        return viscosity;
    }

    public boolean hasViscosity() {
        return viscosity != EngineSetting.BLOCK_VISCOSITY_UNDEFINED;
    }
}