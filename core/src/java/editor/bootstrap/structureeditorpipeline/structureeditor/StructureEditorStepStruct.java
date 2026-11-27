package editor.bootstrap.structureeditorpipeline.structureeditor;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureEditorStepStruct extends StructPackage {

    /*
     * One step of the Structure Editor's history: every layer, its cells,
     * furniture and NPCs, and the origin as they stood before the step, so
     * undoing a stroke that shifted the structure off its corner, or a layer
     * added, removed, moved or renamed, puts everything back together.
     */

    // Snapshot
    private final ObjectArrayList<StructureEditorPartStruct> parts;
    private final int[] origin;
    private final int partIndex;

    // Constructor \\

    public StructureEditorStepStruct(ObjectArrayList<StructureEditorPartStruct> parts, int[] origin, int partIndex) {

        // Snapshot
        this.parts = new ObjectArrayList<>(parts.size());
        this.origin = origin.clone();
        this.partIndex = partIndex;

        for (int i = 0; i < parts.size(); i++)
            this.parts.add(new StructureEditorPartStruct(parts.get(i)));
    }

    // Accessible \\

    public ObjectArrayList<StructureEditorPartStruct> getParts() {
        return parts;
    }

    public int[] getOrigin() {
        return origin;
    }

    public int getPartIndex() {
        return partIndex;
    }
}
