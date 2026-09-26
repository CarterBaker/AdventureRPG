package editor.bootstrap.tabpipeline.docknode;

import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.runtime.EditorSetting;
import engine.root.StructPackage;

public class DockNodeStruct extends StructPackage {

    /*
     * One node of a dock BSP tree. A leaf holds exactly one tab; a split holds
     * two children and the divider ratio, clamped by DockLayoutSystem so
     * neither side collapses. Bounds are recomputed top-down every frame.
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
    private float ratio = EditorSetting.RATIO_DEFAULT;

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