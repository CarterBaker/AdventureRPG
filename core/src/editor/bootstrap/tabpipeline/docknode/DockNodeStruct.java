package editor.bootstrap.tabpipeline.docknode;

import editor.bootstrap.tabpipeline.tab.TabHandle;
import engine.root.StructPackage;

public class DockNodeStruct extends StructPackage {

    /*
     * One node in the dock BSP tree. Leaf nodes hold exactly one tab. Split
     * nodes own two children and hold no tab. Bounds are recomputed each
     * frame top-down from the dock canvas rect.
     *
     * There is no concept of multiple tabs sharing a leaf, and therefore no
     * "active tab within a leaf" — the editor does not support stacked tabs.
     * A leaf whose tab closes is removed from the tree entirely rather than
     * falling back to some other tab; see DockLayoutSystem.pruneTab().
     *
     * ratio controls where the divider sits along the split axis — 0.5 is
     * centre. Clamped to [0.1, 0.9] by DockLayoutSystem.setSplitRatio so
     * neither panel can collapse to nothing. Only meaningful on split nodes.
     */

    // Bounds
    private float x;
    private float y;
    private float w;
    private float h;

    // Split
    private boolean isSplit;
    private boolean splitHorizontal;
    private DockNodeStruct first;
    private DockNodeStruct second;
    private float ratio = 0.5f;

    // Leaf
    private TabHandle tab;

    // Bounds \\

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getW() {
        return w;
    }

    public void setW(float w) {
        this.w = w;
    }

    public float getH() {
        return h;
    }

    public void setH(float h) {
        this.h = h;
    }

    // Split \\

    public boolean isSplit() {
        return isSplit;
    }

    public void setSplit(boolean isSplit) {
        this.isSplit = isSplit;
    }

    public boolean isSplitHorizontal() {
        return splitHorizontal;
    }

    public void setSplitHorizontal(boolean splitHorizontal) {
        this.splitHorizontal = splitHorizontal;
    }

    public DockNodeStruct getFirst() {
        return first;
    }

    public void setFirst(DockNodeStruct first) {
        this.first = first;
    }

    public DockNodeStruct getSecond() {
        return second;
    }

    public void setSecond(DockNodeStruct second) {
        this.second = second;
    }

    public float getRatio() {
        return ratio;
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    // Leaf \\

    public TabHandle getTab() {
        return tab;
    }

    public void setTab(TabHandle tab) {
        this.tab = tab;
    }
}