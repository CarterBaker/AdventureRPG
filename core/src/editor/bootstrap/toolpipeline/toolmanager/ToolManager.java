package editor.bootstrap.toolpipeline.toolmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.tool.ToolOptionStruct;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class ToolManager extends ManagerPackage {

    /*
     * Owns the editor's tools the way a paint program's toolbox does: every
     * canvas keeps the tool chosen for it and its own value of every option
     * it honours, size included, and the Tools panel, the tool keys and the
     * brush size keys all work on the target, the canvas window focused last.
     * Focusing a panel keeps the target, so a tool chosen there reaches the
     * canvas it was chosen for. Every window the shared tool keys work in
     * opens itself here, a canvas with what supplies its canvas, a panel
     * without, through openWindow() and closeWindow(), the one pair that
     * owns them. A tool the target's canvas does not take is refused. The
     * tools, their options and the brush in use are kept between sessions
     * through ToolStateBranch, written once on the frame after they change.
     */

    // Internal
    private WindowManager windowManager;
    private TabManager tabManager;
    private BrushManager brushManager;
    private ToolStateBranch toolStateBranch;

    // Windows
    private Object2ObjectOpenHashMap<WindowInstance, ToolCanvasProvider> window2Provider;
    private ObjectOpenHashSet<WindowInstance> toolWindows;

    // Target
    private WindowInstance targetWindow;
    private ToolCanvas shownCanvas;

    // Tools
    private EditorTool[] canvas2Tool;
    private float[][] canvas2OptionValues;

    // Status
    private int revision;
    private int stateRevision;
    private int savedStateRevision;
    private int savedBrushRevision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.toolStateBranch = create(ToolStateBranch.class);

        // Windows
        this.window2Provider = new Object2ObjectOpenHashMap<>();
        this.toolWindows = new ObjectOpenHashSet<>();

        // Tools
        this.canvas2Tool = new EditorTool[ToolCanvas.VALUES.length];
        this.canvas2OptionValues = new float[ToolCanvas.VALUES.length][ToolOption.VALUES.length];

        for (int i = 0; i < ToolCanvas.VALUES.length; i++) {

            canvas2Tool[i] = ToolCanvas.VALUES[i].getDefaultTool();

            for (int j = 0; j < ToolOption.VALUES.length; j++) {

                ToolOptionStruct option = ToolCanvas.VALUES[i].findOption(ToolOption.VALUES[j]);

                if (option != null)
                    canvas2OptionValues[i][j] = option.getDefaultValue();
            }
        }
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.tabManager = get(TabManager.class);
        this.brushManager = get(BrushManager.class);
    }

    @Override
    protected void awake() {

        toolStateBranch.restore();

        // Status
        this.savedStateRevision = stateRevision;
        this.savedBrushRevision = brushManager.getRevision();
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance focused = windowManager.getFocusedWindow();
        WindowInstance window = focused != null ? tabManager.resolveContentWindow(focused) : null;

        if (window != null && window2Provider.containsKey(window))
            this.targetWindow = window;

        ToolCanvas canvas = getCanvas();

        if (canvas != shownCanvas) {
            this.shownCanvas = canvas;
            notifyChanged();
        }

        saveChanges();
    }

    // The tools and the brush in use written once either moves; moving the target alone changes nothing kept
    private void saveChanges() {

        int brushRevision = brushManager.getRevision();

        if (stateRevision == savedStateRevision && brushRevision == savedBrushRevision)
            return;

        this.savedStateRevision = stateRevision;
        this.savedBrushRevision = brushRevision;
        toolStateBranch.save();
    }

    // Windows \\

    // A window the shared tool keys work in; a canvas comes with what supplies its canvas, a panel with null
    public void openWindow(WindowInstance window, ToolCanvasProvider provider) {

        toolWindows.add(window);

        if (provider == null)
            return;

        window2Provider.put(window, provider);

        if (targetWindow == null)
            this.targetWindow = window;
    }

    public void closeWindow(WindowInstance window) {

        toolWindows.remove(window);
        window2Provider.remove(window);

        if (targetWindow == window)
            this.targetWindow = null;
    }

    // True for a window the shared tool keys work in
    public boolean isToolWindow(WindowInstance window) {
        return toolWindows.contains(window);
    }

    // Tools \\

    public void selectTool(EditorTool tool) {

        ToolCanvas canvas = getCanvas();

        if (canvas != null)
            setTool(canvas, tool);
    }

    // A tool made a canvas's own, when the canvas takes it
    void setTool(ToolCanvas canvas, EditorTool tool) {

        if (!canvas.supports(tool) || canvas2Tool[canvas.ordinal()] == tool)
            return;

        canvas2Tool[canvas.ordinal()] = tool;
        notifyStateChanged();
    }

    // Options \\

    // One of the target canvas's options moved a step, when the canvas honours it
    public void stepOption(ToolOption option, int direction) {

        ToolCanvas canvas = getCanvas();
        ToolOptionStruct range = canvas != null ? canvas.findOption(option) : null;

        if (range != null)
            setOption(canvas, option, range.step(getOption(canvas, option), direction));
    }

    // An option of a canvas set, held to the canvas's range for it
    void setOption(ToolCanvas canvas, ToolOption option, float value) {

        ToolOptionStruct range = canvas.findOption(option);

        if (range == null)
            return;

        canvas2OptionValues[canvas.ordinal()][option.ordinal()] = range.clamp(value);
        notifyStateChanged();
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    private void notifyStateChanged() {
        stateRevision++;
        notifyChanged();
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

    public float getOption(ToolCanvas canvas, ToolOption option) {
        return canvas2OptionValues[canvas.ordinal()][option.ordinal()];
    }

    public int getRevision() {
        return revision;
    }
}
