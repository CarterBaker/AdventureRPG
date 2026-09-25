package editor.bootstrap.infopipeline.inforow;

import editor.bootstrap.infopipeline.util.InfoRowKind;
import engine.root.StructPackage;

public class InfoRowStruct extends StructPackage {

    /*
     * One row of the info panel, flattened from the selected entry. The path
     * addresses the value inside the entry and is handed back on every click.
     * Groups report whether they are expanded and whether elements can be
     * added to them; any row outside a required field can be removed.
     */

    // Identity
    private final String path;
    private final String label;
    private final int depth;
    private final InfoRowKind kind;

    // Value
    private final String valueText;

    // Actions
    private final boolean expanded;
    private final boolean addable;
    private final boolean removable;

    // Constructor \\

    public InfoRowStruct(
            String path,
            String label,
            int depth,
            InfoRowKind kind,
            String valueText,
            boolean expanded,
            boolean addable,
            boolean removable) {

        // Identity
        this.path = path;
        this.label = label;
        this.depth = depth;
        this.kind = kind;

        // Value
        this.valueText = valueText;

        // Actions
        this.expanded = expanded;
        this.addable = addable;
        this.removable = removable;
    }

    // Accessible \\

    public String getPath() {
        return path;
    }

    public String getLabel() {
        return label;
    }

    public int getDepth() {
        return depth;
    }

    public InfoRowKind getKind() {
        return kind;
    }

    public String getValueText() {
        return valueText;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isAddable() {
        return addable;
    }

    public boolean isRemovable() {
        return removable;
    }
}
