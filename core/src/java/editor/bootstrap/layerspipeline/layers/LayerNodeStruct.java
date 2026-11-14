package editor.bootstrap.layerspipeline.layers;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayerNodeStruct extends StructPackage {

    /*
     * One entry of a window's layer stack: a layer its tools work on, or a
     * group holding layers and groups of its own, as a painting tool nests
     * its layers in folders. The key is the window's identity for the entry,
     * handed back on a click and used to remember which groups were folded
     * or opened, so it must stay stable across rebuilds. A group may start
     * open; the Layers tab only remembers the groups turned away from that.
     * The active layer is the one the window's tools work on now, and a
     * group holding it carries that too. A layer that can be hidden carries
     * whether it is shown.
     */

    // Identity
    private final String layerKey;
    private final String label;

    // State
    private final boolean active;
    private final boolean hideable;
    private final boolean visible;
    private final boolean expandedByDefault;

    // Tree
    private final ObjectArrayList<LayerNodeStruct> children;

    // Constructor \\

    public LayerNodeStruct(String layerKey, String label, boolean active, boolean expandedByDefault) {
        this(layerKey, label, active, false, true, expandedByDefault);
    }

    public LayerNodeStruct(
            String layerKey,
            String label,
            boolean active,
            boolean hideable,
            boolean visible,
            boolean expandedByDefault) {

        // Identity
        this.layerKey = layerKey;
        this.label = label;

        // State
        this.active = active;
        this.hideable = hideable;
        this.visible = visible;
        this.expandedByDefault = expandedByDefault;

        // Tree
        this.children = new ObjectArrayList<>();
    }

    // Tree \\

    public void addChild(LayerNodeStruct child) {
        children.add(child);
    }

    // Accessible \\

    public String getLayerKey() {
        return layerKey;
    }

    public String getLabel() {
        return label;
    }

    // The layer the window's tools work on, or a group holding it
    public boolean isActive() {
        return active;
    }

    public boolean isHideable() {
        return hideable;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isExpandedByDefault() {
        return expandedByDefault;
    }

    public ObjectArrayList<LayerNodeStruct> getChildren() {
        return children;
    }

    public boolean isGroup() {
        return !children.isEmpty();
    }
}
