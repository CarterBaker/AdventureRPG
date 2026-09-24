package application.bootstrap.menupipeline.hierarchy;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class HierarchyNodeStruct extends StructPackage {

    /*
     * One entry in a hierarchy tree. The node key is the provider's identity for
     * the entry, handed back on click and used to remember expansion, so it must
     * stay stable across rebuilds. A node may start expanded; panels only
     * remember the nodes a user has toggled away from that default.
     */

    // Identity
    private final String nodeKey;
    private final String label;

    // State
    private final boolean selected;
    private final boolean expandedByDefault;

    // Tree
    private final ObjectArrayList<HierarchyNodeStruct> children;

    // Constructor \\

    public HierarchyNodeStruct(String nodeKey, String label, boolean selected) {
        this(nodeKey, label, selected, false);
    }

    public HierarchyNodeStruct(String nodeKey, String label, boolean selected, boolean expandedByDefault) {

        // Identity
        this.nodeKey = nodeKey;
        this.label = label;

        // State
        this.selected = selected;
        this.expandedByDefault = expandedByDefault;

        // Tree
        this.children = new ObjectArrayList<>();
    }

    // Tree \\

    public void addChild(HierarchyNodeStruct child) {
        children.add(child);
    }

    // Accessible \\

    public String getNodeKey() {
        return nodeKey;
    }

    public String getLabel() {
        return label;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isExpandedByDefault() {
        return expandedByDefault;
    }

    public ObjectArrayList<HierarchyNodeStruct> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}
