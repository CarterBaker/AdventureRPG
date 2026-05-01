package editor.bootstrap.dockpipeline.dockrendersystem;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.dockpipeline.container.ContainerInstance;
import editor.bootstrap.dockpipeline.dockgeometrysystem.DockGeometrySystem;
import editor.bootstrap.dockpipeline.dockmanager.DockManager;
import editor.bootstrap.dockpipeline.node.NodeInstance;
import editor.bootstrap.dockpipeline.tab.TabInstance;
import editor.bootstrap.dockpipeline.tabgroup.TabGroupInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DockRenderSystem extends SystemPackage {

    /*
     * Draws all dock chrome as screen-space flat color quads.
     * Uses the default sprite material with a color uniform —
     * same path as menu sprites, no custom shader needed.
     * Does not touch FBOs or context rendering — that is the context's job.
     */

    // Colors — r g b a
    private static final float[] COL_TAB_BAR = { 0.18f, 0.18f, 0.18f, 1f };
    private static final float[] COL_TAB_ACTIVE = { 0.28f, 0.28f, 0.28f, 1f };
    private static final float[] COL_TAB_INACTIVE = { 0.20f, 0.20f, 0.20f, 1f };
    private static final float[] COL_TAB_HIGHLIGHT = { 0.40f, 0.65f, 1.00f, 1f };
    private static final float[] COL_TAB_CLOSE = { 0.55f, 0.20f, 0.20f, 1f };
    private static final float[] COL_TAB_SEPARATOR = { 0.12f, 0.12f, 0.12f, 1f };
    private static final float[] COL_BORDER = { 0.15f, 0.15f, 0.15f, 1f };
    private static final float[] COL_SPLITTER = { 0.12f, 0.12f, 0.12f, 1f };

    // Dependencies
    private DockManager dockManager;
    private DockGeometrySystem dockGeometrySystem;

    // Per-frame state
    private WindowInstance currentWindow;

    @Override
    protected void get() {
        this.dockManager = get(DockManager.class);
        this.dockGeometrySystem = get(DockGeometrySystem.class);
    }


    @Override
    protected void render() {
        dockGeometrySystem.beginFrame();

        ObjectArrayList<ContainerInstance> containers = dockManager.getContainers();
        Object[] elements = containers.elements();
        int count = containers.size();

        for (int i = 0; i < count; i++) {
            ContainerInstance container = (ContainerInstance) elements[i];
            currentWindow = container.getWindow();
            drawNode(container.getRootNode());
            dockGeometrySystem.flushAndSubmit(currentWindow);
        }
    }

    // Node Traversal \\

    private void drawNode(NodeInstance node) {
        if (node == null)
            return;

        if (node.isLeaf()) {
            drawTabGroup(node);
            return;
        }

        drawSplitter(node);
        drawNode(node.getChildA());
        drawNode(node.getChildB());
    }

    // Tab Group Chrome \\

    private void drawTabGroup(NodeInstance node) {
        TabGroupInstance group = node.getTabGroup();
        if (group == null || group.isEmpty())
            return;

        drawZoneBorder(node);
        drawTabBar(node, group);
    }

    private void drawZoneBorder(NodeInstance node) {
        int x = node.getX();
        int y = node.getY();
        int w = node.getWidth();
        int h = node.getHeight();

        drawRect(x, y, w, EngineSetting.DOCK_BORDER_SIZE, COL_BORDER);
        drawRect(x, y + h - EngineSetting.DOCK_BORDER_SIZE, w, EngineSetting.DOCK_BORDER_SIZE, COL_BORDER);
        drawRect(x, y, EngineSetting.DOCK_BORDER_SIZE, h, COL_BORDER);
        drawRect(x + w - EngineSetting.DOCK_BORDER_SIZE, y, EngineSetting.DOCK_BORDER_SIZE, h, COL_BORDER);
    }

    private void drawTabBar(NodeInstance node, TabGroupInstance group) {
        int x = node.getX();
        int y = node.getY();
        int w = node.getWidth();

        drawRect(x, y, w, EngineSetting.DOCK_TAB_BAR_HEIGHT, COL_TAB_BAR);

        ObjectArrayList<TabInstance> tabs = group.getTabs();
        int tabCount = tabs.size();
        int tabWidth = resolveTabWidth(tabCount, w);
        int tabX = x;

        for (int i = 0; i < tabCount; i++) {
            drawTab(tabs.get(i), tabX, y, tabWidth, i == group.getActiveIndex());
            tabX += tabWidth;
        }
    }

    private void drawTab(TabInstance tab, int x, int y, int width, boolean active) {
        drawRect(x, y, width, EngineSetting.DOCK_TAB_BAR_HEIGHT,
                active ? COL_TAB_ACTIVE : COL_TAB_INACTIVE);

        if (active)
            drawRect(x, y, width, EngineSetting.DOCK_ACTIVE_STRIP_H, COL_TAB_HIGHLIGHT);

        drawRect(x + width - 1, y, 1, EngineSetting.DOCK_TAB_BAR_HEIGHT, COL_TAB_SEPARATOR);

        int closeX = x + width - EngineSetting.DOCK_TAB_CLOSE_SIZE - 4;
        int closeY = y + (EngineSetting.DOCK_TAB_BAR_HEIGHT - EngineSetting.DOCK_TAB_CLOSE_SIZE) / 2;
        drawRect(closeX, closeY, EngineSetting.DOCK_TAB_CLOSE_SIZE,
                EngineSetting.DOCK_TAB_CLOSE_SIZE, COL_TAB_CLOSE);
    }

    // Splitter \\

    private void drawSplitter(NodeInstance node) {
        boolean horizontal = node.getSplitAxis() == NodeInstance.SplitAxis.HORIZONTAL;
        int half = EngineSetting.DOCK_SPLITTER_SIZE / 2;

        if (horizontal) {
            int splitX = node.getX() + (int) (node.getWidth() * node.getSplitRatio());
            drawRect(splitX - half, node.getY(),
                    EngineSetting.DOCK_SPLITTER_SIZE, node.getHeight(), COL_SPLITTER);
        } else {
            int splitY = node.getY() + (int) (node.getHeight() * node.getSplitRatio());
            drawRect(node.getX(), splitY - half,
                    node.getWidth(), EngineSetting.DOCK_SPLITTER_SIZE, COL_SPLITTER);
        }
    }

    // Primitive \\

    private void drawRect(int x, int y, int width, int height, float[] col) {
        dockGeometrySystem.pushRect(x, y, width, height, col);
    }

    // Helpers \\

    private int resolveTabWidth(int tabCount, int zoneWidth) {
        if (tabCount == 0)
            return 0;
        int computed = zoneWidth / tabCount;
        return Math.max(EngineSetting.DOCK_TAB_MIN_WIDTH,
                Math.min(EngineSetting.DOCK_TAB_MAX_WIDTH, computed));
    }
}