package editor.bootstrap.dockpipeline.node;

import editor.bootstrap.dockpipeline.tabgroup.TabGroupInstance;
import engine.root.InstancePackage;

public class NodeInstance extends InstancePackage {

    /*
     * A node in the dock tree. Either a leaf (owns a TabGroup and a rect)
     * or a split (owns two children and a ratio). Never both.
     * Mirrors a binary space partition — same pattern as any UI split system.
     */

    // Enums \\

    public enum SplitAxis {
        HORIZONTAL,
        VERTICAL
    }

    // Data
    private NodeData data;

    // Tree — split state
    private NodeInstance childA;
    private NodeInstance childB;
    private SplitAxis splitAxis;
    private float splitRatio;

    // Leaf state
    private TabGroupInstance tabGroup;

    // Constructor \\

    public void constructor(NodeData data) {
        this.data = data;
        this.splitRatio = 0.5f;
        this.childA = null;
        this.childB = null;
        this.splitAxis = null;
        this.tabGroup = null;
    }

    // Queries \\

    public boolean isLeaf() {
        return tabGroup != null;
    }

    public boolean isSplit() {
        return childA != null;
    }

    // Rect delegation \\

    public void setRect(int x, int y, int width, int height) {
        data.setX(x);
        data.setY(y);
        data.setWidth(width);
        data.setHeight(height);
    }

    public void resize(int width, int height) {
        data.setWidth(width);
        data.setHeight(height);
    }

    // Mutators \\

    public void setChildA(NodeInstance childA) {
        this.childA = childA;
    }

    public void setChildB(NodeInstance childB) {
        this.childB = childB;
    }

    public void setSplitAxis(SplitAxis splitAxis) {
        this.splitAxis = splitAxis;
    }

    public void setSplitRatio(float splitRatio) {
        this.splitRatio = Math.max(0.05f, Math.min(0.95f, splitRatio));
    }

    public void setTabGroup(TabGroupInstance tabGroup) {
        this.tabGroup = tabGroup;
    }

    // Accessible \\

    public NodeData getData() {
        return data;
    }

    public NodeInstance getChildA() {
        return childA;
    }

    public NodeInstance getChildB() {
        return childB;
    }

    public SplitAxis getSplitAxis() {
        return splitAxis;
    }

    public float getSplitRatio() {
        return splitRatio;
    }

    public TabGroupInstance getTabGroup() {
        return tabGroup;
    }

    public int getX() {
        return data.getX();
    }

    public int getY() {
        return data.getY();
    }

    public int getWidth() {
        return data.getWidth();
    }

    public int getHeight() {
        return data.getHeight();
    }

    public int getNodeID() {
        return data.getNodeID();
    }
}