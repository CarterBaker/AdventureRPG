package editor.bootstrap.dockpipeline.dockmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.dockpipeline.container.ContainerData;
import editor.bootstrap.dockpipeline.container.ContainerInstance;
import editor.bootstrap.dockpipeline.node.NodeData;
import editor.bootstrap.dockpipeline.node.NodeInstance;
import editor.bootstrap.dockpipeline.node.NodeInstance.SplitAxis;
import editor.bootstrap.dockpipeline.tab.TabInstance;
import editor.bootstrap.dockpipeline.tabgroup.TabGroupData;
import editor.bootstrap.dockpipeline.tabgroup.TabGroupInstance;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DockManager extends ManagerPackage {

    /*
     * Owns all dock containers, nodes, and tab groups.
     * One container per OS window. Each container owns a binary node tree.
     * Each leaf node owns a tab group. Each tab group owns a list of tabs.
     * DockManager is the sole factory for all dock structures — nothing
     * creates nodes or groups directly.
     */

    // Dependencies
    private WindowManager windowManager;

    // Registry
    private ObjectArrayList<ContainerInstance> containers;
    private Object2ObjectOpenHashMap<WindowInstance, ContainerInstance> window2Container;

    // Identity
    private int nextContainerID;
    private int nextNodeID;
    private int nextGroupID;

    // Internal \\

    @Override
    protected void create() {
        this.containers = new ObjectArrayList<>();
        this.window2Container = new Object2ObjectOpenHashMap<>();
        this.nextContainerID = 0;
        this.nextNodeID = 0;
        this.nextGroupID = 0;
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
    }

    // Container Lifecycle \\

    public ContainerInstance createContainer(WindowInstance window) {
        ContainerData data = new ContainerData(nextContainerID++);
        ContainerInstance container = create(ContainerInstance.class);
        container.constructor(data, window);

        NodeInstance root = createNode(0, 0, window.getWidth(), window.getHeight());
        root.setTabGroup(createGroup());
        container.setRootNode(root);

        containers.add(container);
        window2Container.put(window, container);
        return container;
    }

    public void removeContainer(WindowInstance window) {
        ContainerInstance container = window2Container.remove(window);
        if (container == null)
            return;
        containers.remove(container);
    }

    public void resizeContainer(ContainerInstance container) {
        NodeInstance root = container.getRootNode();
        if (root == null)
            return;
        root.setRect(0, 0, container.getWidth(), container.getHeight());
        recalculateRects(root);
    }

    // Node Operations \\

    public void splitNode(NodeInstance node, SplitAxis axis) {
        if (!node.isLeaf())
            return;

        TabGroupInstance originalGroup = node.getTabGroup();

        NodeInstance childA = createNode(
                computeChildRect(node, axis, true));
        NodeInstance childB = createNode(
                computeChildRect(node, axis, false));

        childA.setTabGroup(originalGroup);
        childB.setTabGroup(createGroup());

        node.setTabGroup(null);
        node.setChildA(childA);
        node.setChildB(childB);
        node.setSplitAxis(axis);
        node.setSplitRatio(0.5f);
    }

    public void collapseNode(ContainerInstance container, NodeInstance parent, boolean keepA) {
        if (!parent.isSplit())
            return;

        NodeInstance keep = keepA ? parent.getChildA() : parent.getChildB();

        if (!keep.isLeaf())
            return;

        parent.setTabGroup(keep.getTabGroup());
        parent.setChildA(null);
        parent.setChildB(null);
        parent.setSplitAxis(null);
    }

    public void setNodeRatio(NodeInstance node, float ratio) {
        node.setSplitRatio(ratio);
        recalculateRects(node);
    }

    // Tab Operations \\

    public void addTab(TabInstance tab, TabGroupInstance group) {
        group.addTab(tab);
        group.setActiveIndex(group.getTabs().size() - 1);
    }

    public void removeTab(TabInstance tab, TabGroupInstance group) {
        group.removeTab(tab);
    }

    public void moveTab(TabInstance tab, TabGroupInstance from, TabGroupInstance to) {
        from.removeTab(tab);
        addTab(tab, to);
    }

    @SuppressWarnings("unchecked")
    public void detachTab(TabInstance tab, TabGroupInstance from) {
        from.removeTab(tab);

        Class contextClass = tab.getTabData().getContextClass();
        WindowInstance newWindow = windowManager.openWindow(tab.getTitle(), contextClass);
        ContainerInstance newContainer = getContainerForWindow(newWindow);
        if (newContainer == null)
            return;
        addTab(tab, newContainer.getRootNode().getTabGroup());
    }

    // Rect Recalculation \\

    public void recalculateRects(NodeInstance node) {
        if (node == null || node.isLeaf())
            return;

        int[] rectA = computeChildRect(node, node.getSplitAxis(), true);
        int[] rectB = computeChildRect(node, node.getSplitAxis(), false);

        node.getChildA().setRect(rectA[0], rectA[1], rectA[2], rectA[3]);
        node.getChildB().setRect(rectB[0], rectB[1], rectB[2], rectB[3]);

        recalculateRects(node.getChildA());
        recalculateRects(node.getChildB());
    }

    private int[] computeChildRect(NodeInstance node, SplitAxis axis, boolean isA) {
        int x = node.getX();
        int y = node.getY();
        int w = node.getWidth();
        int h = node.getHeight();
        float ratio = node.getSplitRatio();

        if (axis == SplitAxis.HORIZONTAL) {
            int splitW = (int) (w * ratio);
            if (isA)
                return new int[] { x, y, splitW, h };
            else
                return new int[] { x + splitW, y, w - splitW, h };
        } else {
            int splitH = (int) (h * ratio);
            if (isA)
                return new int[] { x, y, w, splitH };
            else
                return new int[] { x, y + splitH, w, h - splitH };
        }
    }

    // Factory \\

    private NodeInstance createNode(int x, int y, int width, int height) {
        NodeData data = new NodeData(nextNodeID++, x, y, width, height);
        NodeInstance node = create(NodeInstance.class);
        node.constructor(data);
        return node;
    }

    private NodeInstance createNode(int[] rect) {
        return createNode(rect[0], rect[1], rect[2], rect[3]);
    }

    private TabGroupInstance createGroup() {
        TabGroupData data = new TabGroupData(nextGroupID++);
        TabGroupInstance group = create(TabGroupInstance.class);
        group.constructor(data);
        return group;
    }

    // Lookup \\

    public ContainerInstance getContainerForWindow(WindowInstance window) {
        return window2Container.get(window);
    }

    public ObjectArrayList<ContainerInstance> getContainers() {
        return containers;
    }

    public NodeInstance findNodeForGroup(NodeInstance node, TabGroupInstance group) {
        if (node == null)
            return null;
        if (node.isLeaf())
            return node.getTabGroup() == group ? node : null;
        NodeInstance result = findNodeForGroup(node.getChildA(), group);
        if (result != null)
            return result;
        return findNodeForGroup(node.getChildB(), group);
    }
}
