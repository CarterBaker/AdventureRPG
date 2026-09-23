package application.bootstrap.worldpipeline.block;

import java.util.BitSet;

import engine.root.EngineSetting;
import engine.root.HandlePackage;

class BlockTypePaletteHandle extends HandlePackage {

    /*
     * Child of BlockPaletteHandle recording which of its parent's cells hold
     * one geometry type, as a bitset over the parent's cell index. Only the
     * parent ever writes here, routing every block write through it, so a
     * tick path can walk the cells of the type it simulates and nothing else.
     */

    // Palette Config
    int totalCells;

    // Membership
    private BitSet members;
    private int memberCount;

    // Constructor \\

    void constructor(int totalCells) {

        this.totalCells = totalCells;
        this.members = resetBits(members, totalCells);
        this.memberCount = 0;
    }

    void releaseStorage() {
        members = null;
        memberCount = 0;
    }

    static BitSet resetBits(BitSet bits, int totalCells) {

        if (bits == null)
            return new BitSet(totalCells);

        bits.clear();
        return bits;
    }

    // Management \\

    void add(int cellIndex) {

        if (members.get(cellIndex))
            return;

        members.set(cellIndex);
        memberCount++;
    }

    void remove(int cellIndex) {

        if (!members.get(cellIndex))
            return;

        members.clear(cellIndex);
        memberCount--;
    }

    void fillAll() {
        members.set(0, totalCells);
        memberCount = totalCells;
    }

    void clear() {
        members.clear();
        memberCount = 0;
    }

    // Accessible \\

    boolean contains(int cellIndex) {
        return members.get(cellIndex);
    }

    int getCount() {
        return memberCount;
    }

    int nextMember(int fromIndex) {

        int cellIndex = members.nextSetBit(fromIndex);

        return cellIndex < 0 ? EngineSetting.INDEX_NOT_FOUND : cellIndex;
    }
}
