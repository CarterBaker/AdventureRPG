package editor.bootstrap.infopipeline.inforow;

import editor.bootstrap.infopipeline.util.InfoRowKind;
import engine.root.StructPackage;

public class InfoRowStruct extends StructPackage {

    /*
     * One row of the info panel, flattened from the selected entry. The path
     * addresses the value inside the entry and is handed back on every click.
     * Groups report whether they are expanded and whether elements can be
     * added to them; any row outside a required field can be removed, and a
     * group standing in an array or a map can be duplicated beside itself. A
     * number with both ends of its range declared reports where its value
     * lies across that range, the share its slider's knob stands at; any
     * other number reports NaN and slides by steps about its value. A row
     * holding an error, or a group holding one beneath it, is marked so the
     * panel shows it in red, and a row the game ignores so it is shown muted.
     */

    // Identity
    private final String path;
    private final String label;
    private final int depth;
    private final InfoRowKind kind;

    // Value
    private final String valueText;
    private final float fraction;

    // Actions
    private final boolean expanded;
    private final boolean addable;
    private final boolean removable;
    private final boolean duplicable;

    // Validation
    private final boolean problem;
    private final boolean ignored;

    // Constructor \\

    public InfoRowStruct(
            String path,
            String label,
            int depth,
            InfoRowKind kind,
            String valueText,
            float fraction,
            boolean expanded,
            boolean addable,
            boolean removable,
            boolean duplicable,
            boolean problem,
            boolean ignored) {

        // Identity
        this.path = path;
        this.label = label;
        this.depth = depth;
        this.kind = kind;

        // Value
        this.valueText = valueText;
        this.fraction = fraction;

        // Actions
        this.expanded = expanded;
        this.addable = addable;
        this.removable = removable;
        this.duplicable = duplicable;

        // Validation
        this.problem = problem;
        this.ignored = ignored;
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

    public float getFraction() {
        return fraction;
    }

    public boolean hasRange() {
        return !Float.isNaN(fraction);
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

    public boolean isDuplicable() {
        return duplicable;
    }

    // Two rows listing the same thing the same way, so a panel can refresh one in place
    public boolean isLaidOutAs(InfoRowStruct other) {
        return path.equals(other.path)
                && label.equals(other.label)
                && depth == other.depth
                && kind == other.kind
                && expanded == other.expanded
                && addable == other.addable
                && removable == other.removable
                && duplicable == other.duplicable
                && problem == other.problem
                && ignored == other.ignored;
    }

    public boolean hasProblem() {
        return problem;
    }

    public boolean isIgnored() {
        return ignored;
    }
}
