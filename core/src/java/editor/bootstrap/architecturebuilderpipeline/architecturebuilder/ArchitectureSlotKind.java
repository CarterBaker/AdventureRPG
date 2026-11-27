package editor.bootstrap.architecturebuilderpipeline.architecturebuilder;

import editor.runtime.EditorSetting;

public enum ArchitectureSlotKind {

    /*
     * The two kinds of slot an architecture fills, each a root of the
     * Architecture Builder's layers: block slots, kept in an architecture's
     * "blocks" as a map of groups to their places, and furniture slots, the
     * furnishing tables kept in its "furniture" by their full name, grouped
     * by the table's folder.
     */

    BLOCKS(EditorSetting.ARCHITECTURE_BUILDER_LAYER_BLOCKS, "blocks", true),
    FURNITURE(EditorSetting.ARCHITECTURE_BUILDER_LAYER_FURNITURE, "furniture", false);

    // Identity
    public final String label;
    public final String field;

    // Layout — whether a slot is kept under its group's own map rather than by its full name
    public final boolean nested;

    // Constructor \\

    ArchitectureSlotKind(String label, String field, boolean nested) {

        // Identity
        this.label = label;
        this.field = field;

        // Layout
        this.nested = nested;
    }
}
