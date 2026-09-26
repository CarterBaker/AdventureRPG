package application.bootstrap.worldpipeline.block;

import java.util.Arrays;

import application.bootstrap.worldpipeline.util.SubBlockUtility;

class SubBlockPaletteHandle extends BlockTypePaletteHandle {

    /*
     * Partial child of BlockPaletteHandle. A cell is a member exactly while
     * its block is subdivided into sub-blocks, and on top of membership this
     * child owns that cell's octant mask (see SubBlockUtility) — the parent's
     * block ID is the material every present octant is made of. A member is
     * always genuinely partial: the parent collapses a mask that fills back
     * up to a whole block out of this child, and a mask that empties out is
     * written as air instead, so the geometry palettes and this one never
     * both claim a cell. Masks are realized on the first subdivided cell, so
     * a palette that never holds a sub-block never pays for them.
     */

    // Masks
    private byte[] masks;

    // Constructor \\

    @Override
    void constructor(int totalCells) {

        super.constructor(totalCells);

        if (masks != null)
            Arrays.fill(masks, (byte) SubBlockUtility.MASK_FULL);
    }

    @Override
    void releaseStorage() {
        super.releaseStorage();
        masks = null;
    }

    private void ensureMasks() {

        if (masks != null)
            return;

        masks = new byte[totalCells];
        Arrays.fill(masks, (byte) SubBlockUtility.MASK_FULL);
    }

    // Management \\

    @Override
    void add(int cellIndex) {
        super.add(cellIndex);
        ensureMasks();
    }

    @Override
    void remove(int cellIndex) {

        super.remove(cellIndex);

        if (masks != null)
            masks[cellIndex] = (byte) SubBlockUtility.MASK_FULL;
    }

    @Override
    void fillAll() {
        throwException("A subdivided cell is only ever written one cell at a time through its mask — "
                + "no block definition may declare PARTIAL geometry, so nothing can fill this palette wholesale.");
    }

    @Override
    void clear() {

        super.clear();

        if (masks != null)
            Arrays.fill(masks, (byte) SubBlockUtility.MASK_FULL);
    }

    // Mask \\

    int getMask(int cellIndex) {

        if (!contains(cellIndex))
            return SubBlockUtility.MASK_FULL;

        return masks[cellIndex] & SubBlockUtility.MASK_FULL;
    }

    void setMask(int cellIndex, int mask) {

        if (!contains(cellIndex))
            return;

        masks[cellIndex] = (byte) mask;
    }
}
