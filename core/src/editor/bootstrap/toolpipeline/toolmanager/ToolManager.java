package editor.bootstrap.toolpipeline.toolmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class ToolManager extends ManagerPackage {

    /*
     * Owns the editor's tools the way a paint program's toolbox does: every
     * canvas keeps the tool chosen for it and its own brush size, and the
     * Tools panel, the tool keys and the brush size keys all work on the
     * target, the canvas window focused last. Focusing a panel keeps the
     * target, so a tool chosen there reaches the canvas it was chosen for.
     * Each window that takes tools opens itself here as a canvas with what
     * supplies its canvas, and the Brushes panel opens itself as a tool
     * window, so the shared tool keys work in it too. A tool the target's
     * canvas does not take is refused.
     */

    // Internal
    private WindowManager windowManager;
    private TabManager tabManager;

    // Windows
    private Object2ObjectOpenHashMap<WindowInstance, ToolCanvasProvider> window2Provider;
    private ObjectOpenHashSet<WindowInstance> toolWindows;

    // Target
    private WindowInstance targetWindow;
    private ToolCanvas shownCanvas;

    // Tools
    private EditorTool[] canvas2Tool;
    private float[] canvas2Size;

    // Status
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Windows
        this.window2Provider = new Object2ObjectOpenHashMap<>();
        this.toolWindows = new ObjectOpenHashSet<>();

        // Tools
        this.canvas2Tool = new EditorTool[ToolCanvas.VALUES.length];
        this.canvas2Size = new float[ToolCanvas.VALUES.length];

        for (int i = 0; i < ToolCanvas.VALUES.length; i++) {
            canvas2Tool[i] = ToolCanvas.VALUES[i].getDefaultTool();
            canvas2Size[i] = ToolCanvas.VALUES[i].getSizeDefault();
        }
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.tabManager = get(TabManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance focused = windowManager.getFocusedWindow();
        WindowInstance window = focused != null ? tabManager.resolveContentWindow(focused) : null;

        if (window != null && window2Provider.containsKey(window))
            this.targetWindow = window;

        ToolCanvas canvas = getCanvas();

        if (canvas == shownCanvas)
            return;

        this.shownCanvas = canvas;
        notifyChanged();
    }

    // Windows \\

    public void openCanvas(WindowInstance window, ToolCanvasProvider provider) {

        window2Provider.put(window, provider);
        toolWindows.add(window);

        if (targetWindow == null)
            this.targetWindow = window;
    }

    public void closeCanvas(WindowInstance window) {

        window2Provider.remove(window);
        toolWindows.remove(window);

        if (targetWindow == window)
            this.targetWindow = null;
    }

    public void openToolWindow(WindowInstance window) {
        toolWindows.add(window);
    }

    public void closeToolWindow(WindowInstance window) {
        toolWindows.remove(window);
    }

    // True for a window the shared tool keys work in
    public boolean isToolWindow(WindowInstance window) {
        return toolWindows.contains(window);
    }

    // Tools \\

    public void selectTool(EditorTool tool) {

        ToolCanvas canvas = getCanvas();

        if (canvas == null || !canvas.supports(tool) || canvas2Tool[canvas.ordinal()] == tool)
            return;

        canvas2Tool[canvas.ordinal()] = tool;
        notifyChanged();
    }

    // Size \\

    public void resizeBrush(int direction) {

        ToolCanvas canvas = getCanvas();

        if (canvas == null || !canvas.takesBrush())
            return;

        canvas2Size[canvas.ordinal()] = canvas.stepSize(canvas2Size[canvas.ordinal()], direction);
        notifyChanged();
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    // Accessible \\

    // What the target works on, null while there is no target or it takes no tools right now
    public ToolCanvas getCanvas() {

        ToolCanvasProvider provider = targetWindow != null ? window2Provider.get(targetWindow) : null;

        return provider != null ? provider.getToolCanvas() : null;
    }

    public EditorTool getTool(ToolCanvas canvas) {
        return canvas2Tool[canvas.ordinal()];
    }

    public float getSize(ToolCanvas canvas) {
        return canvas2Size[canvas.ordinal()];
    }

    public int getRevision() {
        return revision;
    }
}
