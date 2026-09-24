package application.bootstrap.menupipeline.hierarchy;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class HierarchyNodeStruct extends StructPackage {

    /*
     * One entry in a hierarchy tree. The node key is the provider's identity for
     * the entry, handed back on click and used to remember expansion, so it must
     * stay stable across rebuilds.
     */

    // Identity
    private final String nodeKey;
    private final String label;

    // State
    private final boolean selected;

    // Tree
    private final ObjectArrayList<HierarchyNodeStruct> children;

    // Constructor \\

    public HierarchyNodeStruct(String nodeKey, String label, boolean selected) {

        // Identity
        this.nodeKey = nodeKey;
        this.label = label;

        // State
        this.selected = selected;

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

    public ObjectArrayList<HierarchyNodeStruct> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}
