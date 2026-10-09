package application.bootstrap.worldpipeline.structure;

import application.bootstrap.worldpipeline.block.BlockHandle;
import engine.root.StructPackage;

public class StructurePaletteEntryStruct extends StructPackage {

    /*
     * One block a structure lays, resolved once while it builds: the block,
     * its packed orientation and the sub-block mask it fills, full for a
     * whole block. A structure's own palette names these by key so its
     * layers and block entries refer to them by a single character.
     */

    // Block
    private final BlockHandle blockHandle;
    private final short orientation;
    private final int mask;

    // Constructor \\

    public StructurePaletteEntryStruct(BlockHandle blockHandle, short orientation, int mask) {

        // Block
        this.blockHandle = blockHandle;
        this.orientation = orientation;
        this.mask = mask;
    }

    // Accessible \\

    public BlockHandle getBlockHandle() {
        return blockHandle;
    }

    public short getOrientation() {
        return orientation;
    }

    public int getMask() {
        return mask;
    }
}
