package editor.bootstrap.structureeditorpipeline.structureeditor;

import engine.root.StructPackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureEditorPartStruct extends StructPackage {

    /*
     * One layer of a structure as the Structure Editor stacks them, a floor,
     * a roof or any alteration: its name, the cells it lays keyed from the
     * structure's corner, the places it comes furnished at and the spots its
     * NPCs live at, each kept as the entry the file holds, and whether the
     * editor shows it, which is the editor's alone and never written. Entries
     * are never changed in place, so a copy for the editor's history shares
     * them.
     */

    // Identity
    private String name;

    // Contents
    private final Long2ObjectOpenHashMap<ArpgObjectStruct> cells;
    private final ObjectArrayList<ArpgObjectStruct> furnishings;
    private final ObjectArrayList<ArpgObjectStruct> spawns;

    // View
    private boolean shown;

    // Constructor \\

    public StructureEditorPartStruct(String name) {

        // Identity
        this.name = name;

        // Contents
        this.cells = new Long2ObjectOpenHashMap<>();
        this.furnishings = new ObjectArrayList<>();
        this.spawns = new ObjectArrayList<>();

        // View
        this.shown = true;
    }

    public StructureEditorPartStruct(StructureEditorPartStruct source) {

        // Identity
        this.name = source.name;

        // Contents
        this.cells = new Long2ObjectOpenHashMap<>(source.cells);
        this.furnishings = new ObjectArrayList<>(source.furnishings);
        this.spawns = new ObjectArrayList<>(source.spawns);

        // View
        this.shown = source.shown;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long2ObjectOpenHashMap<ArpgObjectStruct> getCells() {
        return cells;
    }

    public ObjectArrayList<ArpgObjectStruct> getFurnishings() {
        return furnishings;
    }

    public ObjectArrayList<ArpgObjectStruct> getSpawns() {
        return spawns;
    }

    public boolean isShown() {
        return shown;
    }

    public void setShown(boolean shown) {
        this.shown = shown;
    }
}
