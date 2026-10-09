package application.bootstrap.worldpipeline.road;

import engine.root.StructPackage;

public class RoadBridgeStruct extends StructPackage {

    /*
     * How a road crosses water or a deep fall: the deck it is carried on, the
     * rail along each edge, the pillars that stand down to the ground every
     * so many blocks along it, and how far above the ground the road must
     * run before it takes to a bridge at all.
     */

    // Blocks
    private final short deckBlockID;
    private final short railBlockID;
    private final short pillarBlockID;

    // Shape
    private final int pillarSpacingBlocks;
    private final int minHeightBlocks;

    // Constructor \\

    public RoadBridgeStruct(
            short deckBlockID,
            short railBlockID,
            short pillarBlockID,
            int pillarSpacingBlocks,
            int minHeightBlocks) {

        // Blocks
        this.deckBlockID = deckBlockID;
        this.railBlockID = railBlockID;
        this.pillarBlockID = pillarBlockID;

        // Shape
        this.pillarSpacingBlocks = pillarSpacingBlocks;
        this.minHeightBlocks = minHeightBlocks;
    }

    // Accessible \\

    public short getDeckBlockID() {
        return deckBlockID;
    }

    public short getRailBlockID() {
        return railBlockID;
    }

    public short getPillarBlockID() {
        return pillarBlockID;
    }

    public int getPillarSpacingBlocks() {
        return pillarSpacingBlocks;
    }

    public int getMinHeightBlocks() {
        return minHeightBlocks;
    }
}
