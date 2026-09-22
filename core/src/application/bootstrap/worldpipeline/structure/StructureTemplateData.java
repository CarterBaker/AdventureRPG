package application.bootstrap.worldpipeline.structure;

import engine.root.DataPackage;
import engine.root.EngineSetting;

public class StructureTemplateData extends DataPackage {

    /*
     * A hand-authored block volume. Cells are stored x-fastest, then z, then
     * y from the bottom layer up. A cell holding BLOCK_SKIP leaves whatever
     * terrain generation put there untouched; every other cell — air
     * included — overwrites it, which is how a template carves its own
     * interior out of a hillside. The anchor is the template cell that sits
     * on the placement point: its y is the floor layer, the one laid at
     * ground (or street) level. Front is the z = 0 face; placement rotates
     * the template so that face looks at whatever it is meant to face — the
     * street a building lines, or the road leading to it. When a foundation
     * block is set, every column whose bottom cell is solid is extended
     * downward with it until it meets the ground, so a template on a slope
     * stands on a plinth instead of floating.
     */

    public static final short BLOCK_SKIP = EngineSetting.REGISTRY_RESERVED_ID;

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final short[] blocks;

    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;

    private final short foundationBlockID;

    public StructureTemplateData(
            int sizeX, int sizeY, int sizeZ,
            short[] blocks,
            int anchorX, int anchorY, int anchorZ,
            short foundationBlockID) {

        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.blocks = blocks;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.foundationBlockID = foundationBlockID;
    }

    public short getBlock(int x, int y, int z) {
        return blocks[x + sizeX * (z + sizeZ * y)];
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public int getAnchorX() {
        return anchorX;
    }

    public int getAnchorY() {
        return anchorY;
    }

    public int getAnchorZ() {
        return anchorZ;
    }

    public short getFoundationBlockID() {
        return foundationBlockID;
    }

    public boolean hasFoundation() {
        return foundationBlockID != BLOCK_SKIP;
    }
}
