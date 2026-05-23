package editor.bootstrap.docklayoutsystem;

import editor.bootstrap.docknode.DockNodeStruct;
import editor.bootstrap.tab.TabHandle;
import engine.root.SystemPackage;

public class DockLayoutSystem extends SystemPackage {

    /*
     * Manages the dock BSP tree. addTab() always targets the largest leaf and
     * splits it — wide panels split left/right, tall panels split top/bottom.
     * removeTab() prunes empty leaves and collapses redundant split nodes.
     * computeRects() propagates the dock canvas bounds down the tree each frame
     * so every leaf knows its screen rect without storing stale state.
     *
     * Each split node owns a ratio in [0.1, 0.9] (default 0.5) controlling
     * where its divider sits. findDividerAt() walks the tree bottom-up so the
     * innermost node always wins when dividers are nested. setSplitRatio()
     * clamps and writes the ratio; propagateRect() reads it.
     */

    private static final float DIVIDER_HIT_TOLERANCE = 6f;
    private static final float RATIO_MIN = 0.1f;
    private static final float RATIO_MAX = 0.9f;

    // Tree
    private DockNodeStruct root;

    // Internal \\

    @Override
    protected void create() {
        this.root = null;
    }

    // Management \\

    public void addTab(TabHandle handle) {

        if (root == null) {
            root = new DockNodeStruct();
            root.getTabs().add(handle);
            return;
        }

        splitNode(findLargestLeaf(root), handle);
    }

    public void removeTab(TabHandle handle) {

        if (root == null)
            return;

        root = pruneTab(root, handle);
    }

    // Per-Frame \\

    public void computeRects(float x, float y, float w, float h) {

        if (root == null)
            return;

        propagateRect(root, x, y, w, h);
    }

    public float getTabX(TabHandle handle) {
        DockNodeStruct node = findLeaf(root, handle);
        return node != null ? node.getX() : 0f;
    }

    public float getTabY(TabHandle handle) {
        DockNodeStruct node = findLeaf(root, handle);
        return node != null ? node.getY() : 0f;
    }

    public float getTabW(TabHandle handle) {
        DockNodeStruct node = findLeaf(root, handle);
        return node != null ? node.getW() : 0f;
    }

    public float getTabH(TabHandle handle) {
        DockNodeStruct node = findLeaf(root, handle);
        return node != null ? node.getH() : 0f;
    }

    public boolean isTabActive(TabHandle handle) {

        DockNodeStruct node = findLeaf(root, handle);

        if (node == null || node.getTabs().isEmpty())
            return false;

        return node.getTabs().get(node.getActiveIndex()) == handle;
    }

    // Divider \\

    public DockNodeStruct findDividerAt(float screenX, float screenY) {
        return findDividerAt(root, screenX, screenY);
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

    private void splitNode(DockNodeStruct leaf, TabHandle incoming) {

        boolean splitHorizontal = leaf.getH() > leaf.getW();

        DockNodeStruct preserved = new DockNodeStruct();
        preserved.getTabs().addAll(leaf.getTabs());
        preserved.setActiveIndex(leaf.getActiveIndex());

        DockNodeStruct created = new DockNodeStruct();
        created.getTabs().add(incoming);

        leaf.setSplit(true);
        leaf.setSplitHorizontal(splitHorizontal);
        leaf.setTabs(null);
        leaf.setActiveIndex(0);
        leaf.setFirst(preserved);
        leaf.setSecond(created);
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