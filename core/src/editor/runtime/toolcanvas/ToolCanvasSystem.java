package editor.runtime.toolcanvas;

import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import engine.root.SystemPackage;

public class ToolCanvasSystem extends SystemPackage {

    /*
     * Opens this context's window to ToolManager for as long as it lives, so
     * the shared tool keys work in it, and, when the context supplies a tool
     * canvas, the Tools panel and those keys work on it too. Every editor
     * that takes tools, and every panel that takes their keys, creates one;
     * nothing else opens or closes a window there.
     */

    // Internal
    private ToolManager toolManager;

    // Base \\

    @Override
    protected void get() {
        this.toolManager = get(ToolManager.class);
    }

    @Override
    protected void awake() {
        toolManager.openWindow(
                context.getWindow(),
                context instanceof ToolCanvasProvider provider ? provider : null);
    }

    @Override
    protected void dispose() {
        toolManager.closeWindow(context.getWindow());
    }
}
