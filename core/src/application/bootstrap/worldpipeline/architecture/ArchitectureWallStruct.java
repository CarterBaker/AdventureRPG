package application.bootstrap.worldpipeline.architecture;

import engine.root.StructPackage;

public class ArchitectureWallStruct extends StructPackage {

    /*
     * The curtain wall an architecture rings its walled settlements with: the
     * block it is built of, the block its battlements are capped with, how
     * high it stands over the ground and how thick it is, and the colors
     * distant terrain and the map draw its top and face in.
     */

    // Blocks
    private final short wallBlockID;
    private final short capBlockID;

    // Shape
    private final int heightBlocks;
    private final float thicknessBlocks;

    // Colors
    private final int topColor;
    private final int sideColor;

    // Constructor \\

    public ArchitectureWallStruct(
            short wallBlockID,
            short capBlockID,
            int heightBlocks,
            float thicknessBlocks,
            int topColor,
            int sideColor) {

        // Blocks
        this.wallBlockID = wallBlockID;
        this.capBlockID = capBlockID;

        // Shape
        this.heightBlocks = heightBlocks;
        this.thicknessBlocks = thicknessBlocks;

        // Colors
        this.topColor = topColor;
        this.sideColor = sideColor;
    }

    // Accessible \\

    public short getWallBlockID() {
        return wallBlockID;
    }

    public short getCapBlockID() {
        return capBlockID;
    }

    public int getHeightBlocks() {
        return heightBlocks;
    }

    public float getThicknessBlocks() {
        return thicknessBlocks;
    }

    public float getHalfThicknessBlocks() {
        return thicknessBlocks * 0.5f;
    }

    public int getTopColor() {
        return topColor;
    }

    public int getSideColor() {
        return sideColor;
    }
}
