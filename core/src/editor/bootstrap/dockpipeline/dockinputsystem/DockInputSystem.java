package editor.bootstrap.dockpipeline.dockinputsystem;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.dockpipeline.container.ContainerInstance;
import editor.bootstrap.dockpipeline.dockmanager.DockManager;
import editor.bootstrap.dockpipeline.node.NodeInstance;
import editor.bootstrap.dockpipeline.tab.TabInstance;
import editor.bootstrap.dockpipeline.tabgroup.TabGroupInstance;
import editor.bootstrap.dockpipeline.tabmanager.TabManager;
import engine.root.EngineSetting;
import engine.settings.KeyBindings;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DockInputSystem extends SystemPackage {

    /*
     * Handles all dock chrome interaction:
     * - Tab click → activate tab
     * - Close button click → destroy tab
     * - Splitter drag → adjust split ratio
     * - Tab drag → move tab between groups or detach to new OS window
     *
     * Hit testing mirrors ElementHitSystem — bounding box checks against
     * node rects and tab bar geometry. Mouse coords come from InputSystem.
     * All structural mutations go through DockManager and TabManager.
     */

    // Chrome constants — must match DockRenderSystem exactly
    private static final int TAB_BAR_HEIGHT = 24;
    private static final int TAB_MIN_WIDTH = 80;
    private static final int TAB_MAX_WIDTH = 200;
    private static final int TAB_CLOSE_SIZE = 14;
    private static final int SPLITTER_SIZE = 4;
    private static final int SPLITTER_HALF = SPLITTER_SIZE / 2;

    // Drag threshold — pixels before a drag is recognized
    private static final float DRAG_THRESHOLD = 6f;

    // Dependencies
    private InputSystem inputSystem;
    private DockManager dockManager;
    private TabManager tabManager;
    private WindowManager windowManager;

    // Splitter drag state
    private NodeInstance draggingSplitter;
    private boolean splitterDragActive;

    // Tab drag state
    private TabInstance draggingTab;
    private TabGroupInstance draggingFromGroup;
    private float dragStartX;
    private float dragStartY;
    private boolean tabDragActive;

    // Cursor state
    private int lastCursorShape;

    @Override
    protected void get() {
        this.inputSystem = get(InputSystem.class);
        this.dockManager = get(DockManager.class);
        this.tabManager = get(TabManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void update() {
        float mx = inputSystem.getMouseX();
        float my = inputSystem.getMouseY();
        updateHoverCursor(mx, my);
        boolean clicked = inputSystem.bindingClicked(KeyBindings.PRIMARY);
        boolean held = inputSystem.bindingHeld(KeyBindings.PRIMARY);
        boolean released = inputSystem.bindingReleased(KeyBindings.PRIMARY);

        if (released)
            handleRelease(mx, my);
        else if (tabDragActive)
            handleTabDrag(mx, my);
        else if (splitterDragActive)
            handleSplitterDrag(mx, my);
        else if (held && draggingTab != null)
            maybeStartTabDrag(mx, my);
        else if (clicked)
            handleClick(mx, my);
    }

    // Click \\

    private void handleClick(float mx, float my) {
        ObjectArrayList<ContainerInstance> containers = dockManager.getContainers();

        for (int i = 0; i < containers.size(); i++) {
            ContainerInstance container = containers.get(i);
            if (hitTestContainer(container, mx, my))
                return;
        }
    }

    private boolean hitTestContainer(ContainerInstance container, float mx, float my) {
        return hitTestNode(container.getRootNode(), container.getWindow(), mx, my);
    }

    private boolean hitTestNode(NodeInstance node, WindowInstance window, float mx, float my) {
        if (node == null)
            return false;

        if (node.isSplit()) {
            if (hitSplitter(node, mx, my)) {
                draggingSplitter = node;
                splitterDragActive = true;
                return true;
            }
            return hitTestNode(node.getChildA(), window, mx, my)
                    || hitTestNode(node.getChildB(), window, mx, my);
        }

        if (node.isLeaf())
            return hitTestTabBar(node, window, mx, my);

        return false;
    }

    private boolean hitTestTabBar(NodeInstance node, WindowInstance window, float mx, float my) {
        int x = node.getX();
        int y = node.getY();
        int w = node.getWidth();

        if (!inRect(mx, my, x, y, w, TAB_BAR_HEIGHT))
            return false;

        TabGroupInstance group = node.getTabGroup();
        if (group == null || group.isEmpty())
            return false;

        int tabCount = group.getTabs().size();
        int tabWidth = resolveTabWidth(tabCount, w);
        int tabX = x;

        for (int i = 0; i < tabCount; i++) {
            TabInstance tab = group.getTabs().get(i);

            // Close button
            int closeX = tabX + tabWidth - TAB_CLOSE_SIZE - 4;
            int closeY = y + (TAB_BAR_HEIGHT - TAB_CLOSE_SIZE) / 2;
            if (inRect(mx, my, closeX, closeY, TAB_CLOSE_SIZE, TAB_CLOSE_SIZE)) {
                tabManager.destroyTab(tab);
                dockManager.removeTab(tab, group);
                collapseIfEmpty(group, node, window);
                return true;
            }

            // Tab body — begin potential drag
            if (inRect(mx, my, tabX, y, tabWidth, TAB_BAR_HEIGHT)) {
                tabManager.activateTab(tab, group);
                draggingTab = tab;
                draggingFromGroup = group;
                dragStartX = mx;
                dragStartY = my;
                return true;
            }

            tabX += tabWidth;
        }

        return false;
    }

    // Splitter Drag \\

    private boolean hitSplitter(NodeInstance node, float mx, float my) {
        boolean horizontal = node.getSplitAxis() == NodeInstance.SplitAxis.HORIZONTAL;

        if (horizontal) {
            int splitX = node.getX() + (int) (node.getWidth() * node.getSplitRatio());
            return inRect(mx, my,
                    splitX - SPLITTER_HALF, node.getY(),
                    SPLITTER_SIZE, node.getHeight());
        } else {
            int splitY = node.getY() + (int) (node.getHeight() * node.getSplitRatio());
            return inRect(mx, my,
                    node.getX(), splitY - SPLITTER_HALF,
                    node.getWidth(), SPLITTER_SIZE);
        }
    }

    private void handleSplitterDrag(float mx, float my) {
        if (draggingSplitter == null)
            return;

        boolean horizontal = draggingSplitter.getSplitAxis() == NodeInstance.SplitAxis.HORIZONTAL;
        float ratio;

        if (horizontal) {
            float relX = mx - draggingSplitter.getX();
            ratio = relX / draggingSplitter.getWidth();
        } else {
            float relY = my - draggingSplitter.getY();
            ratio = relY / draggingSplitter.getHeight();
        }

        dockManager.setNodeRatio(draggingSplitter, ratio);
    }

    // Tab Drag \\

    private void maybeStartTabDrag(float mx, float my) {
        float dx = mx - dragStartX;
        float dy = my - dragStartY;
        if (dx * dx + dy * dy >= DRAG_THRESHOLD * DRAG_THRESHOLD)
            tabDragActive = true;
    }

    private void handleTabDrag(float mx, float my) {
        // Visual feedback handled by DockRenderSystem when draggingTab != null
        // Drop zone highlighting can be added here later
    }

    // Release \\

    private void handleRelease(float mx, float my) {
        if (splitterDragActive) {
            splitterDragActive = false;
            draggingSplitter = null;
            return;
        }

        if (tabDragActive && draggingTab != null) {
            resolveTabDrop(mx, my);
        }

        clearDragState();
    }

    private void resolveTabDrop(float mx, float my) {
        ObjectArrayList<ContainerInstance> containers = dockManager.getContainers();

        // Try drop into an existing container's tab group
        for (int i = 0; i < containers.size(); i++) {
            ContainerInstance container = containers.get(i);
            NodeInstance target = findDropNode(container.getRootNode(), mx, my);

            if (target != null && target.getTabGroup() != draggingFromGroup) {
                dockManager.moveTab(draggingTab, draggingFromGroup, target.getTabGroup());
                tabManager.activateTab(draggingTab, target.getTabGroup());
                collapseIfEmpty(draggingFromGroup,
                        findNodeForGroup(container, draggingFromGroup),
                        container.getWindow());
                return;
            }
        }

        // Dropped outside all containers — detach to new OS window
        dockManager.detachTab(draggingTab, draggingFromGroup);
    }

    private NodeInstance findDropNode(NodeInstance node, float mx, float my) {
        if (node == null)
            return null;

        if (node.isLeaf()) {
            int barBottom = node.getY() + TAB_BAR_HEIGHT;
            if (inRect(mx, my, node.getX(), node.getY(), node.getWidth(), TAB_BAR_HEIGHT))
                return node;
            // Allow drop onto content area too
            if (inRect(mx, my, node.getX(), barBottom,
                    node.getWidth(), node.getHeight() - TAB_BAR_HEIGHT))
                return node;
            return null;
        }

        NodeInstance result = findDropNode(node.getChildA(), mx, my);
        if (result != null)
            return result;
        return findDropNode(node.getChildB(), mx, my);
    }

    // Collapse empty group after tab removal \\

    private void collapseIfEmpty(TabGroupInstance group, NodeInstance node, WindowInstance window) {
        if (group == null || node == null)
            return;
        if (!group.isEmpty())
            return;

        ContainerInstance container = dockManager.getContainerForWindow(window);
        if (container == null)
            return;

        NodeInstance parent = findParentNode(container.getRootNode(), node);
        if (parent == null)
            return;

        boolean keepA = parent.getChildB() == node;
        dockManager.collapseNode(container, parent, keepA);
    }

    private NodeInstance findParentNode(NodeInstance current, NodeInstance target) {
        if (current == null || current.isLeaf())
            return null;
        if (current.getChildA() == target || current.getChildB() == target)
            return current;
        NodeInstance result = findParentNode(current.getChildA(), target);
        if (result != null)
            return result;
        return findParentNode(current.getChildB(), target);
    }

    private NodeInstance findNodeForGroup(ContainerInstance container, TabGroupInstance group) {
        return dockManager.findNodeForGroup(container.getRootNode(), group);
    }

    // Clear state \\

    private void clearDragState() {
        draggingTab = null;
        draggingFromGroup = null;
        tabDragActive = false;
        dragStartX = 0;
        dragStartY = 0;
    }

    private void updateHoverCursor(float mx, float my) {
        int shape = resolveHoverCursorShape(mx, my);
        if (shape == lastCursorShape)
            return;
        lastCursorShape = shape;
        WindowInstance active = windowManager.getActiveWindow();
        if (active == null)
            return;
        internal.windowPlatform.setCursorShape(active.getNativeHandle(), shape);
    }

    private int resolveHoverCursorShape(float mx, float my) {
        ObjectArrayList<ContainerInstance> containers = dockManager.getContainers();
        for (int i = 0; i < containers.size(); i++) {
            int shape = findSplitterCursorShape(containers.get(i).getRootNode(), mx, my);
            if (shape != EngineSetting.CURSOR_DEFAULT)
                return shape;
        }
        return EngineSetting.CURSOR_DEFAULT;
    }

    private int findSplitterCursorShape(NodeInstance node, float mx, float my) {
        if (node == null || node.isLeaf())
            return EngineSetting.CURSOR_DEFAULT;
        if (hitSplitter(node, mx, my))
            return node.getSplitAxis() == NodeInstance.SplitAxis.HORIZONTAL
                    ? EngineSetting.CURSOR_RESIZE_H
                    : EngineSetting.CURSOR_RESIZE_V;
        int result = findSplitterCursorShape(node.getChildA(), mx, my);
        if (result != EngineSetting.CURSOR_DEFAULT)
            return result;
        return findSplitterCursorShape(node.getChildB(), mx, my);
    }

    // Helpers \\

    private boolean inRect(float mx, float my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int resolveTabWidth(int tabCount, int zoneWidth) {
        if (tabCount == 0)
            return 0;
        int computed = zoneWidth / tabCount;
        return Math.max(TAB_MIN_WIDTH, Math.min(TAB_MAX_WIDTH, computed));
    }
}
