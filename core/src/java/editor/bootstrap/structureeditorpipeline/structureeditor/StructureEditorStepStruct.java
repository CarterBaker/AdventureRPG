package editor.bootstrap.structureeditorpipeline.structureeditor;

import engine.root.StructPackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class StructureEditorStepStruct extends StructPackage {

    /*
     * One step of the Structure Editor's history: every cell and the origin
     * as they stood before the step, so undoing a stroke that shifted the
     * structure off its corner puts the origin back with the cells.
     */

    // Snapshot
    private final Long2ObjectOpenHashMap<ArpgObjectStruct> cells;
    private final int[] origin;

    // Constructor \\

    public StructureEditorStepStruct(Long2ObjectOpenHashMap<ArpgObjectStruct> cells, int[] origin) {

        // Snapshot
        this.cells = new Long2ObjectOpenHashMap<>(cells);
        this.origin = origin.clone();
    }

    // Accessible \\

    public Long2ObjectOpenHashMap<ArpgObjectStruct> getCells() {
        return cells;
    }

    public int[] getOrigin() {
        return origin;
    }
}
