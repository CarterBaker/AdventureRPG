package editor.bootstrap.dockpipeline.dockrendersystem;

import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.dockpipeline.container.ContainerInstance;
import editor.bootstrap.dockpipeline.dockmanager.DockManager;
import editor.bootstrap.dockpipeline.node.NodeInstance;
import editor.bootstrap.dockpipeline.tab.TabInstance;
import editor.bootstrap.dockpipeline.tabgroup.TabGroupInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DockRenderSystem extends SystemPackage {

    /*
     * Draws all dock chrome as screen-space flat color quads.
     * Tab bars, tab backgrounds, active highlights, close buttons,
     * splitter handles, and zone borders.
     * Uses the same FlatColorRect material + Sprite mesh path
     * as MenuRenderSystem — identical transform convention.
     * Does not touch FBOs or context rendering — that is the context's job.
     */

    // Chrome constants — pixels
    private static final int TAB_BAR_HEIGHT = 24;
    private static final int TAB_MIN_WIDTH = 80;
    private static final int TAB_MAX_WIDTH = 200;
    private static final int TAB_CLOSE_SIZE = 14;
    private static final int SPLITTER_SIZE = 4;
    private static final int BORDER_SIZE = 1;
    private static final int ACTIVE_STRIP_H = 2;

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
    private RenderManager renderManager;
    private MaterialManager materialManager;
    private MeshManager meshManager;
    private ModelManager modelManager;
    private DockManager dockManager;

    // Shared draw resources
    private ModelInstance flatRectModel;
    private Vector4 colorScratch;
    private Matrix4 transformScratch;

    // Per-frame state
    private WindowInstance currentWindow;

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
        this.materialManager = get(MaterialManager.class);
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.dockManager = get(DockManager.class);
    }

    @Override
    protected void awake() {
        MaterialInstance material = materialManager.cloneMaterial("util/FlatColorRect");
        this.flatRectModel = modelManager.createModel(
                meshManager.getMeshHandleFromMeshName("util/Sprite"), material);
        this.colorScratch = new Vector4();
        this.transformScratch = new Matrix4();
    }

    @Override
    protected void render() {
        ObjectArrayList<ContainerInstance> containers = dockManager.getContainers();
        Object[] elements = containers.elements();
        int count = containers.size();

        for (int i = 0; i < count; i++) {
            ContainerInstance container = (ContainerInstance) elements[i];
            currentWindow = container.getWindow();
            drawNode(container.getRootNode());
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

        drawRect(x, y, w, BORDER_SIZE, COL_BORDER);
        drawRect(x, y + h - BORDER_SIZE, w, BORDER_SIZE, COL_BORDER);
        drawRect(x, y, BORDER_SIZE, h, COL_BORDER);
        drawRect(x + w - BORDER_SIZE, y, BORDER_SIZE, h, COL_BORDER);
    }

    private void drawTabBar(NodeInstance node, TabGroupInstance group) {
        int x = node.getX();
        int y = node.getY();
        int w = node.getWidth();

        drawRect(x, y, w, TAB_BAR_HEIGHT, COL_TAB_BAR);

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
        drawRect(x, y, width, TAB_BAR_HEIGHT,
                active ? COL_TAB_ACTIVE : COL_TAB_INACTIVE);

        if (active)
            drawRect(x, y, width, ACTIVE_STRIP_H, COL_TAB_HIGHLIGHT);

        drawRect(x + width - 1, y, 1, TAB_BAR_HEIGHT, COL_TAB_SEPARATOR);

        int closeX = x + width - TAB_CLOSE_SIZE - 4;
        int closeY = y + (TAB_BAR_HEIGHT - TAB_CLOSE_SIZE) / 2;
        drawRect(closeX, closeY, TAB_CLOSE_SIZE, TAB_CLOSE_SIZE, COL_TAB_CLOSE);
    }

    // Splitter \\

    private void drawSplitter(NodeInstance node) {
        boolean horizontal = node.getSplitAxis() == NodeInstance.SplitAxis.HORIZONTAL;

        if (horizontal) {
            int splitX = node.getX() + (int) (node.getWidth() * node.getSplitRatio());
            drawRect(splitX - SPLITTER_SIZE / 2, node.getY(),
                    SPLITTER_SIZE, node.getHeight(), COL_SPLITTER);
        } else {
            int splitY = node.getY() + (int) (node.getHeight() * node.getSplitRatio());
            drawRect(node.getX(), splitY - SPLITTER_SIZE / 2,
                    node.getWidth(), SPLITTER_SIZE, COL_SPLITTER);
        }
    }

    // Primitive \\

    private void drawRect(int x, int y, int width, int height, float[] col) {
        transformScratch.set(
                width, 0, 0, x,
                0, height, 0, y,
                0, 0, 1, 0,
                0, 0, 0, 1);

        colorScratch.set(col[0], col[1], col[2], col[3]);

        flatRectModel.getMaterial().setUniform("u_transform", transformScratch);
        flatRectModel.getMaterial().setUniform("u_color", colorScratch);

        renderManager.pushScreenCall(flatRectModel, currentWindow);
    }

    // Helpers \\

    private int resolveTabWidth(int tabCount, int zoneWidth) {
        if (tabCount == 0)
            return 0;
        int computed = zoneWidth / tabCount;
        return Math.max(TAB_MIN_WIDTH, Math.min(TAB_MAX_WIDTH, computed));
    }
}