package application.bootstrap.worldpipeline.structure;

import application.bootstrap.worldpipeline.block.BlockHandle;
import engine.root.StructPackage;

public class StructurePaletteEntryStruct extends StructPackage {

    /*
     * One block a structure lays, resolved once while it builds: the block,
     * its packed orientation, the sub-block mask it fills, full for a whole
     * block, the coverage grown over it, NONE for a bare block, and the
     * content it holds, NONE for an empty one. A structure's own palette
     * names these by key so its layers and block entries refer to them by a
     * single character.
     */

    // Block
    private final BlockHandle blockHandle;
    private final short orientation;
    private final int mask;
    private final short coverage;
    private final short content;

    // Constructor \\

    public StructurePaletteEntryStruct(
            BlockHandle blockHandle,
            short orientation,
            int mask,
            short coverage,
            short content) {

        // Block
        this.blockHandle = blockHandle;
        this.orientation = orientation;
        this.mask = mask;
        this.coverage = coverage;
        this.content = content;
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

    public short getCoverage() {
        return coverage;
    }

    public short getContent() {
        return content;
    }
}
