package application.bootstrap.worldpipeline.block;

import java.util.Arrays;
import java.util.BitSet;

import engine.root.EngineSetting;

class LiquidBlockPaletteHandle extends BlockTypePaletteHandle {

    /*
     * Liquid child of BlockPaletteHandle. On top of membership it owns every
     * per-cell liquid property: fill level, whether the cell is active (may
     * still move and so must be simulated), whether it belongs to a
     * permanent body that acts as an infinite source, and whether it belongs
     * to the tidal ocean, whose level the tide owns rather than the flow
     * simulation. Levels are realized on
     * the first liquid cell, so a palette that never holds liquid never pays
     * for them.
     */

    // Levels
    private byte[] levels;

    // Activity
    private BitSet active;
    private int activeCount;

    // Permanence
    private BitSet permanent;

    // Tide
    private BitSet tidal;

    // Constructor \\

    @Override
    void constructor(int totalCells) {

        super.constructor(totalCells);

        this.active = resetBits(active, totalCells);
        this.permanent = resetBits(permanent, totalCells);
        this.tidal = resetBits(tidal, totalCells);
        this.activeCount = 0;

        if (levels != null)
            Arrays.fill(levels, (byte) EngineSetting.LIQUID_LEVEL_EMPTY);
    }

    @Override
    void releaseStorage() {
        super.releaseStorage();
        levels = null;
        active = null;
        permanent = null;
        tidal = null;
        activeCount = 0;
    }

    private void ensureLevels() {
        if (levels == null)
            levels = new byte[totalCells];
    }

    // Management \\

    @Override
    void add(int cellIndex) {

        super.add(cellIndex);
        ensureLevels();

        levels[cellIndex] = (byte) EngineSetting.LIQUID_LEVEL_MAX;
        permanent.clear(cellIndex);
        tidal.clear(cellIndex);
        deactivate(cellIndex);
    }

    @Override
    void remove(int cellIndex) {

        super.remove(cellIndex);

        if (levels != null)
            levels[cellIndex] = (byte) EngineSetting.LIQUID_LEVEL_EMPTY;

        permanent.clear(cellIndex);
        tidal.clear(cellIndex);
        deactivate(cellIndex);
    }

    @Override
    void fillAll() {

        super.fillAll();
        ensureLevels();

        Arrays.fill(levels, (byte) EngineSetting.LIQUID_LEVEL_MAX);
        permanent.set(0, totalCells);
        tidal.set(0, totalCells);
        active.clear();
        activeCount = 0;
    }

    @Override
    void clear() {

        super.clear();

        if (levels != null)
            Arrays.fill(levels, (byte) EngineSetting.LIQUID_LEVEL_EMPTY);

        active.clear();
        permanent.clear();
        tidal.clear();
        activeCount = 0;
    }

    // Level \\

    short getLevel(int cellIndex) {

        if (!contains(cellIndex))
            return EngineSetting.LIQUID_LEVEL_EMPTY;

        return (short) (levels[cellIndex] & 0xFF);
    }

    void setLevel(int cellIndex, short level) {

        if (!contains(cellIndex))
            return;

        levels[cellIndex] = (byte) level;
    }

    // Permanence \\

    boolean isPermanent(int cellIndex) {
        return permanent.get(cellIndex);
    }

    void setPermanent(int cellIndex, boolean isPermanent) {

        if (!contains(cellIndex))
            return;

        permanent.set(cellIndex, isPermanent);
    }

    // Tide \\

    boolean isTidal(int cellIndex) {
        return tidal.get(cellIndex);
    }

    void setTidal(int cellIndex, boolean isTidal) {

        if (!contains(cellIndex))
            return;

        tidal.set(cellIndex, isTidal);
    }

    // Activity \\

    void activate(int cellIndex) {

        if (!contains(cellIndex) || active.get(cellIndex))
            return;

        active.set(cellIndex);
        activeCount++;
    }

    void deactivate(int cellIndex) {

        if (!active.get(cellIndex))
            return;

        active.clear(cellIndex);
        activeCount--;
    }

    int getActiveCount() {
        return activeCount;
    }

    int collectActive(int[] target) {

        int count = 0;

        for (int cellIndex = active.nextSetBit(0); cellIndex >= 0; cellIndex = active.nextSetBit(cellIndex + 1))
            target[count++] = cellIndex;

        return count;
    }
}
