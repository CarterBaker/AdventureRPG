package application.bootstrap.worldpipeline.liquidmanager;

import java.util.BitSet;

import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinateUtility;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

class LiquidVisitedStruct extends StructPackage {

    /*
     * Visited set for a bounded liquid scan that may cross subchunk and chunk
     * borders. Each subchunk the scan reaches borrows one preallocated bitset
     * slot, so marking and testing a cell is a single bit operation and a scan
     * never allocates. A scan that needs more subchunks than there are slots
     * is, by definition, larger than anything the liquid pipeline resolves.
     */

    // Internal
    private final Reference2IntOpenHashMap<SubChunkInstance> subChunk2Slot;
    private final BitSet[] slotBits;
    private int slotCount;

    // Constructor \\

    LiquidVisitedStruct() {

        this.subChunk2Slot = new Reference2IntOpenHashMap<>();
        this.subChunk2Slot.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        this.slotBits = new BitSet[EngineSetting.LIQUID_SCAN_SUBCHUNK_LIMIT];

        for (int i = 0; i < slotBits.length; i++)
            slotBits[i] = new BitSet(ChunkCoordinateUtility.BLOCK_COORDINATE_COUNT);

        this.slotCount = 0;
    }

    // Management \\

    boolean hasRoomFor(SubChunkInstance subChunkInstance) {
        return slotCount < slotBits.length || subChunk2Slot.containsKey(subChunkInstance);
    }

    boolean isVisited(LiquidCellStruct cell) {

        int slot = subChunk2Slot.getInt(cell.getSubChunkInstance());

        if (slot == EngineSetting.INDEX_NOT_FOUND)
            return false;

        return slotBits[slot].get(ChunkCoordinateUtility.getIndex(cell.getPackedXYZ()));
    }

    void visit(LiquidCellStruct cell) {

        SubChunkInstance subChunkInstance = cell.getSubChunkInstance();
        int slot = subChunk2Slot.getInt(subChunkInstance);

        if (slot == EngineSetting.INDEX_NOT_FOUND) {

            if (slotCount >= slotBits.length)
                throwException("Liquid scan reached more subchunks than LIQUID_SCAN_SUBCHUNK_LIMIT allows — "
                        + "check hasRoomFor() before visiting a cell in a new subchunk.");

            slot = slotCount++;
            subChunk2Slot.put(subChunkInstance, slot);
        }

        slotBits[slot].set(ChunkCoordinateUtility.getIndex(cell.getPackedXYZ()));
    }

    void clear() {

        for (int i = 0; i < slotCount; i++)
            slotBits[i].clear();

        subChunk2Slot.clear();
        slotCount = 0;
    }
}
