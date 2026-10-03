package application.bootstrap.worldpipeline.block;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;

public class BlockData extends DataPackage {

    /*
     * Immutable block definition. Viscosity is required for liquid blocks, and
     * natural marks blocks that receive the edge bevel and vertex jitter;
     * artificial blocks always meet their neighbors flat. A breakable solid
     * block names the tool that breaks it and the item texture its block
     * piece is drawn with. Every face also carries its texture's average
     * albedo, the color terrain drawn without textures stands in with.
     */

    // Identity
    private final String blockName;
    private final String localName;
    private final short blockID;
    private final DynamicGeometryType geometry;
    private final BlockRotationType rotationType;
    private final boolean natural;

    // Rendering
    private final int materialID;
    private final int[] faceTextures;
    private final int[] faceMapColors;

    // Breaking
    private final int breakTier;
    private final short requiredToolTypeID;
    private final int durability;

    // Piece — the item texture its block piece is drawn with, BLOCK_ITEM_TEXTURE_NONE for none
    private final String itemTextureName;

    // Physics
    private final float viscosity;

    // Constructor \\

    public BlockData(
            String blockName,
            String localName,
            short blockID,
            DynamicGeometryType geometry,
            BlockRotationType rotationType,
            boolean natural,
            int materialID,
            int northTexture, int eastTexture, int southTexture,
            int westTexture, int upTexture, int downTexture,
            int[] faceMapColors,
            int breakTier,
            short requiredToolTypeID,
            int durability,
            String itemTextureName,
            float viscosity) {

        this.blockName = blockName;
        this.localName = localName;
        this.blockID = blockID;
        this.geometry = geometry;
        this.rotationType = rotationType;
        this.natural = natural;
        this.materialID = materialID;

        this.faceTextures = new int[Direction3Vector.LENGTH];
        this.faceTextures[Direction3Vector.NORTH.ordinal()] = northTexture;
        this.faceTextures[Direction3Vector.EAST.ordinal()] = eastTexture;
        this.faceTextures[Direction3Vector.SOUTH.ordinal()] = southTexture;
        this.faceTextures[Direction3Vector.WEST.ordinal()] = westTexture;
        this.faceTextures[Direction3Vector.UP.ordinal()] = upTexture;
        this.faceTextures[Direction3Vector.DOWN.ordinal()] = downTexture;
        this.faceMapColors = faceMapColors;

        this.breakTier = breakTier;
        this.requiredToolTypeID = requiredToolTypeID;
        this.durability = durability;

        this.itemTextureName = itemTextureName;

        this.viscosity = viscosity;
    }

    // Accessible \\

    public String getBlockName() {
        return blockName;
    }

    public String getLocalName() {
        return localName;
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

    public boolean isNatural() {
        return natural;
    }

    public int getMaterialID() {
        return materialID;
    }

    public int getTextureForFace(Direction3Vector direction) {
        return faceTextures[direction.ordinal()];
    }

    public int getMapColorForFace(Direction3Vector direction) {
        return faceMapColors[direction.ordinal()];
    }

    public boolean hasMapColor() {
        return faceMapColors[Direction3Vector.UP.ordinal()] != EngineSetting.BLOCK_MAP_COLOR_UNDEFINED;
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

    public String getItemTextureName() {
        return itemTextureName;
    }

    public boolean hasPiece() {
        return !itemTextureName.isEmpty();
    }

    public float getViscosity() {
        return viscosity;
    }

    public boolean hasViscosity() {
        return viscosity != EngineSetting.BLOCK_VISCOSITY_UNDEFINED;
    }
}