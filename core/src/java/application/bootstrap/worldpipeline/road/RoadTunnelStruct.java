package application.bootstrap.worldpipeline.road;

import engine.root.StructPackage;

public class RoadTunnelStruct extends StructPackage {

    /*
     * How a road bores through high ground: the lining its walls and arched
     * ceiling are built of, the height of the bore at its centre, and how
     * deep under the ground the road must run before it takes to a tunnel at
     * all.
     */

    // Blocks
    private final short liningBlockID;

    // Shape
    private final int heightBlocks;
    private final int minDepthBlocks;

    // Constructor \\

    public RoadTunnelStruct(short liningBlockID, int heightBlocks, int minDepthBlocks) {

        // Blocks
        this.liningBlockID = liningBlockID;

        // Shape
        this.heightBlocks = heightBlocks;
        this.minDepthBlocks = minDepthBlocks;
    }

    // Accessible \\

    public short getLiningBlockID() {
        return liningBlockID;
    }

    public int getHeightBlocks() {
        return heightBlocks;
    }

    public int getMinDepthBlocks() {
        return minDepthBlocks;
    }
}
