package editor.bootstrap.tabpipeline.docklayoutsystem;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.util.DropZone;
import editor.runtime.EditorSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class DockLayoutSystem extends SystemPackage {

    /*
     * Keeps one BSP dock tree per OS window, one tab per leaf. Adds, splits and
     * removes leaves, propagates dock bounds into leaf rects each frame, and
     * resolves dividers and leaves for window-local coordinates. Split ratios
     * are clamped, and collapse() is the one rule for a split that lost a
     * child.
     */

    // Per-OS-window BSP roots — null root means window is registered but empty.
    private Object2ObjectOpenHashMap<WindowInstance, DockNodeStruct> roots;

    // Internal \\

    @Override
    protected void create() {
        this.roots = new Object2ObjectOpenHashMap<>();
    }

    // Window Lifecycle \\

    public void initWindow(WindowInstance osWindow) {
        roots.put(osWindow, null);
    }

    public void removeWindow(WindowInstance osWindow) {
        roots.remove(osWindow);
    }

    // Management \\

    public void addTab(WindowInstance osWindow, TabHandle handle) {

        DockNodeStruct root = roots.get(osWindow);

        if (root == null) {
            roots.put(osWindow, createLeaf(handle));
            return;
        }

        DockNodeStruct largest = findLargestLeaf(root);
        splitNode(largest, handle, resolveSplitDirection(largest));
    }

    public void addTabToLeaf(DockNodeStruct leaf, TabHandle handle, DropZone zone) {

        if (leaf == null || handle == null || zone == null)
            return;

        splitNode(leaf, handle, zone);
    }

    public void removeTab(WindowInstance osWindow, TabHandle handle) {

        DockNodeStruct root = roots.get(osWindow);

        if (root == null)
            return;

        root = pruneTab(root, handle);
        roots.put(osWindow, root);
    }

    // Per-Frame \\

    public void computeRects(WindowInstance osWindow, float x, float y, float w, float h) {

        DockNodeStruct root = roots.get(osWindow);

        if (root == null)
            return;

        propagateRect(root, x, y, w, h);
    }

    public float getTabX(WindowInstance osWindow, TabHandle handle) {
        DockNodeStruct node = findLeaf(roots.get(osWindow), handle);
        return node != null ? node.getX() : 0f;
    }

    public float getTabY(WindowInstance osWindow, TabHandle handle) {
        DockNodeStruct node = findLeaf(roots.get(osWindow), handle);
        return node != null ? node.getY() : 0f;
    }

    public float getTabW(WindowInstance osWindow, TabHandle handle) {
        DockNodeStruct node = findLeaf(roots.get(osWindow), handle);
        return node != null ? node.getW() : 0f;
    }

    public float getTabH(WindowInstance osWindow, TabHandle handle) {
        DockNodeStruct node = findLeaf(roots.get(osWindow), handle);
        return node != null ? node.getH() : 0f;
    }

    // Divider \\

    public DockNodeStruct findDividerAt(WindowInstance osWindow, float x, float y) {
        return findDividerAt(roots.get(osWindow), x, y);
    }

    private DockNodeStruct findDividerAt(DockNodeStruct node, float sx, float sy) {

        if (node == null || !node.isSplit())
            return null;

        DockNodeStruct hit = findDividerAt(node.getFirst(), sx, sy);
        if (hit != null)
            return hit;

        hit = findDividerAt(node.getSecond(), sx, sy);
        if (hit != null)
            return hit;

        if (node.isSplitHorizontal()) {
            float dividerY = node.getY() + node.getH() * node.getRatio();
            if (sx >= node.getX() && sx <= node.getX() + node.getW()
                    && sy >= dividerY - EditorSetting.DIVIDER_HIT_TOLERANCE
                    && sy <= dividerY + EditorSetting.DIVIDER_HIT_TOLERANCE)
                return node;
        } else {
            float dividerX = node.getX() + node.getW() * node.getRatio();
            if (sy >= node.getY() && sy <= node.getY() + node.getH()
                    && sx >= dividerX - EditorSetting.DIVIDER_HIT_TOLERANCE
                    && sx <= dividerX + EditorSetting.DIVIDER_HIT_TOLERANCE)
                return node;
        }

        return null;
    }

    public void setSplitRatio(DockNodeStruct node, float ratio) {
        node.setRatio(Math.max(EditorSetting.RATIO_MIN, Math.min(EditorSetting.RATIO_MAX, ratio)));
    }

    // Leaf At Screen Point — window-scoped \\

    public DockNodeStruct findLeafAt(WindowInstance osWindow, float localX, float localY) {
        return findLeafAt(roots.get(osWindow), localX, localY);
    }

    private DockNodeStruct findLeafAt(DockNodeStruct node, float sx, float sy) {

        if (node == null)
            return null;

        if (!containsPoint(node, sx, sy))
            return null;

        if (!node.isSplit())
            return node;

        DockNodeStruct hit = findLeafAt(node.getFirst(), sx, sy);
        if (hit != null)
            return hit;

        return findLeafAt(node.getSecond(), sx, sy);
    }

    private boolean containsPoint(DockNodeStruct node, float sx, float sy) {
        return sx >= node.getX()
                && sx < node.getX() + node.getW()
                && sy >= node.getY()
                && sy < node.getY() + node.getH();
    }

    // Tree Traversal \\

    private DockNodeStruct findLargestLeaf(DockNodeStruct node) {

        if (!node.isSplit())
            return node;

        DockNodeStruct largestFirst = findLargestLeaf(node.getFirst());
        DockNodeStruct largestSecond = findLargestLeaf(node.getSecond());

        return largestFirst.getW() * largestFirst.getH() >= largestSecond.getW() * largestSecond.getH()
                ? largestFirst
                : largestSecond;
    }

    private DockNodeStruct findLeaf(DockNodeStruct node, TabHandle handle) {

        if (node == null)
            return null;

        if (!node.isSplit())
            return node.getTab() == handle ? node : null;

        DockNodeStruct result = findLeaf(node.getFirst(), handle);
        return result != null ? result : findLeaf(node.getSecond(), handle);
    }

    private void propagateRect(DockNodeStruct node, float x, float y, float w, float h) {

        node.setX(x);
        node.setY(y);
        node.setW(w);
        node.setH(h);

        if (!node.isSplit())
            return;

        float ratio = node.getRatio();

        if (node.isSplitHorizontal()) {
            float split = h * ratio;
            propagateRect(node.getFirst(), x, y, w, split);
            propagateRect(node.getSecond(), x, y + split, w, h - split);
        } else {
            float split = w * ratio;
            propagateRect(node.getFirst(), x, y, split, h);
            propagateRect(node.getSecond(), x + split, y, w - split, h);
        }
    }

    // Split / Merge \\

    private DropZone resolveSplitDirection(DockNodeStruct leaf) {
        return leaf.getH() > leaf.getW() ? DropZone.BOTTOM : DropZone.RIGHT;
    }

    private void splitNode(DockNodeStruct leaf, TabHandle incoming, DropZone zone) {

        boolean splitHorizontal = zone == DropZone.TOP || zone == DropZone.BOTTOM;
        boolean incomingIsSecond = zone == DropZone.RIGHT || zone == DropZone.BOTTOM;

        DockNodeStruct preserved = createLeaf(leaf.getTab());
        DockNodeStruct created = createLeaf(incoming);

        leaf.setSplit(true);
        leaf.setSplitHorizontal(splitHorizontal);
        leaf.setTab(null);
        leaf.setRatio(EditorSetting.RATIO_DEFAULT);

        if (incomingIsSecond) {
            leaf.setFirst(preserved);
            leaf.setSecond(created);
        } else {
            leaf.setFirst(created);
            leaf.setSecond(preserved);
        }
    }

    private DockNodeStruct pruneTab(DockNodeStruct node, TabHandle handle) {

        if (node == null)
            return null;

        if (!node.isSplit())
            return node.getTab() == handle ? null : node;

        node.setFirst(pruneTab(node.getFirst(), handle));
        node.setSecond(pruneTab(node.getSecond(), handle));

        return collapse(node);
    }

    private DockNodeStruct collapse(DockNodeStruct node) {

        if (node.getFirst() == null)
            return node.getSecond();

        if (node.getSecond() == null)
            return node.getFirst();

        return node;
    }

    // Layout Persistence \\

    public Object2ObjectOpenHashMap<WindowInstance, DockNodeStruct> getRoots() {
        return roots;
    }

    public DockNodeStruct createLeaf(TabHandle handle) {

        DockNodeStruct leaf = new DockNodeStruct();
        leaf.setTab(handle);

        return leaf;
    }

    public DockNodeStruct createSplit(
            DockNodeStruct first,
            DockNodeStruct second,
            boolean splitHorizontal,
            float ratio) {

        DockNodeStruct node = new DockNodeStruct();
        node.setSplit(true);
        node.setSplitHorizontal(splitHorizontal);
        node.setFirst(first);
        node.setSecond(second);
        setSplitRatio(node, ratio);

        return collapse(node);
    }

    public void restoreRoot(WindowInstance osWindow, DockNodeStruct root) {
        roots.put(osWindow, root);
    }
}