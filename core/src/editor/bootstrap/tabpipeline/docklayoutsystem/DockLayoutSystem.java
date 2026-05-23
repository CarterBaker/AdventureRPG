package editor.bootstrap.tabpipeline.docklayoutsystem;

import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.util.DropZone;
import engine.root.SystemPackage;

public class DockLayoutSystem extends SystemPackage {

    /*
     * Manages the dock BSP tree. addTab() always targets the largest leaf and
     * splits it — wide panels split left/right, tall panels split top/bottom.
     * addTabToLeaf() performs a directed split on a specific leaf at a given
     * DropZone so tab drag-and-drop can place tabs precisely.
     * removeTab() prunes empty leaves and collapses redundant split nodes.
     * computeRects() propagates the dock canvas bounds down the tree each frame
     * so every leaf knows its screen rect without storing stale state.
     *
     * Each split node owns a ratio in [0.1, 0.9] (default 0.5) controlling
     * where its divider sits. findDividerAt() walks the tree bottom-up so the
     * innermost node always wins when dividers are nested. setSplitRatio()
     * clamps and writes the ratio; propagateRect() reads it.
     *
     * findLeafAt() walks the tree by rect containment and returns the leaf
     * whose bounds contain the given screen coordinate. Used by TabDragSystem
     * each frame to resolve which leaf the cursor is over.
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

        splitNode(findLargestLeaf(root), handle, resolveSplitDirection(findLargestLeaf(root)));
    }

    public void addTabToLeaf(DockNodeStruct leaf, TabHandle handle, DropZone zone) {

        if (leaf == null || handle == null || zone == null)
            return;

        splitNode(leaf, handle, zone);
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

    // Leaf At Screen Point \\

    public DockNodeStruct findLeafAt(float screenX, float screenY) {
        return findLeafAt(root, screenX, screenY);
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