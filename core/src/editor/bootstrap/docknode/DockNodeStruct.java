package editor.bootstrap.docknode;

import editor.bootstrap.tab.TabHandle;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DockNodeStruct extends StructPackage {

    /*
     * One node in the dock BSP tree. Leaf nodes own a tab list and track the
     * active tab index. Split nodes own two children and hold no tabs.
     * Bounds are recomputed each frame top-down from the dock canvas rect.
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

    // Leaf
    private ObjectArrayList<TabHandle> tabs;
    private int activeIndex;

    // Internal \\

    public DockNodeStruct() {
        this.tabs = new ObjectArrayList<>();
    }

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

    // Leaf \\

    public ObjectArrayList<TabHandle> getTabs() {
        return tabs;
    }

    public void setTabs(ObjectArrayList<TabHandle> tabs) {
        this.tabs = tabs;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }
}