package editor.bootstrap.tabpipeline.docklayoutsystem;

import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.util.DropZone;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class DockLayoutSystem extends SystemPackage {

    /*
     * Manages one BSP tree per OS window. addTab() always targets the largest
     * leaf in the given window's tree and splits it. addTabToLeaf() performs a
     * directed split on a specific leaf at a given DropZone. removeTab() prunes
     * empty leaves and collapses redundant split nodes. computeRects() propagates
     * dock canvas bounds down the tree each frame so every leaf knows its screen
     * rect without storing stale state.
     *
     * initWindow() must be called when a new OS window is registered so the map
     * entry exists before any tab is added. removeWindow() cleans up the entry
     * when a secondary OS window is closed.
     *
     * findDividerAt() and findLeafAt() walk all trees — callers do not need to
     * know which window a divider or leaf belongs to. All other methods are
     * window-scoped so per-tree rect math stays isolated.
     *
     * Each split node owns a ratio in [0.1, 0.9] (default 0.5) controlling
     * where its divider sits. findDividerAt() walks each tree bottom-up so the
     * innermost node always wins when dividers are nested. setSplitRatio()
     * clamps and writes the ratio; propagateRect() reads it.
     */

    private static final float DIVIDER_HIT_TOLERANCE = 6f;
    private static final float RATIO_MIN = 0.1f;
    private static final float RATIO_MAX = 0.9f;

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
            root = new DockNodeStruct();
            root.getTabs().add(handle);
            roots.put(osWindow, root);
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

    public boolean isTabActive(WindowInstance osWindow, TabHandle handle) {

        DockNodeStruct node = findLeaf(roots.get(osWindow), handle);

        if (node == null || node.getTabs().isEmpty())
            return false;

        return node.getTabs().get(node.getActiveIndex()) == handle;
    }

    // Divider \\

    public DockNodeStruct findDividerAt(float screenX, float screenY) {

        for (DockNodeStruct root : roots.values()) {
            DockNodeStruct hit = findDividerAt(root, screenX, screenY);
            if (hit != null)
                return hit;
        }

        return null;
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
                    && sy >= dividerY - DIVIDER_HIT_TOLERANCE
                    && sy <= dividerY + DIVIDER_HIT_TOLERANCE)
                return node;
        } else {
            float dividerX = node.getX() + node.getW() * node.getRatio();
            if (sy >= node.getY() && sy <= node.getY() + node.getH()
                    && sx >= dividerX - DIVIDER_HIT_TOLERANCE
                    && sx <= dividerX + DIVIDER_HIT_TOLERANCE)
                return node;
        }

        return null;
    }

    public void setSplitRatio(DockNodeStruct node, float ratio) {
        node.setRatio(Math.max(RATIO_MIN, Math.min(RATIO_MAX, ratio)));
    }

    // Leaf At Screen Point \\

    public DockNodeStruct findLeafAt(float screenX, float screenY) {

        for (DockNodeStruct root : roots.values()) {
            DockNodeStruct hit = findLeafAt(root, screenX, screenY);
            if (hit != null)
                return hit;
        }

        return null;
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
            return node.getTabs().contains(handle) ? node : null;

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

        DockNodeStruct preserved = new DockNodeStruct();
        preserved.getTabs().addAll(leaf.getTabs());
        preserved.setActiveIndex(leaf.getActiveIndex());

        DockNodeStruct created = new DockNodeStruct();
        created.getTabs().add(incoming);

        leaf.setSplit(true);
        leaf.setSplitHorizontal(splitHorizontal);
        leaf.setTabs(null);
        leaf.setActiveIndex(0);
        leaf.setRatio(0.5f);

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

        if (!node.isSplit()) {
            node.getTabs().remove(handle);
            if (node.getActiveIndex() >= node.getTabs().size())
                node.setActiveIndex(Math.max(0, node.getTabs().size() - 1));
            return node.getTabs().isEmpty() ? null : node;
        }

        node.setFirst(pruneTab(node.getFirst(), handle));
        node.setSecond(pruneTab(node.getSecond(), handle));

        if (node.getFirst() == null && node.getSecond() == null)
            return null;

        if (node.getFirst() == null)
            return node.getSecond();

        if (node.getSecond() == null)
            return node.getFirst();

        return node;
    }
}