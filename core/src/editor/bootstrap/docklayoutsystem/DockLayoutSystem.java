package editor.bootstrap.docklayoutsystem;

import editor.bootstrap.docknode.DockNodeStruct;
import editor.bootstrap.tab.TabHandle;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DockLayoutSystem extends SystemPackage {

    /*
     * Manages the dock BSP tree. addTab() always targets the largest leaf and
     * splits it — wide panels split left/right, tall panels split top/bottom.
     * removeTab() prunes empty leaves and collapses redundant split nodes.
     * computeRects() propagates the dock canvas bounds down the tree each frame
     * so every leaf knows its screen rect without storing stale state.
     */

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

        if (node.isSplitHorizontal()) {
            float half = h * 0.5f;
            propagateRect(node.getFirst(), x, y, w, half);
            propagateRect(node.getSecond(), x, y + half, w, half);
        } else {
            float half = w * 0.5f;
            propagateRect(node.getFirst(), x, y, half, h);
            propagateRect(node.getSecond(), x + half, y, half, h);
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