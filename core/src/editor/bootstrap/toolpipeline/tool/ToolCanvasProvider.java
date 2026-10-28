package editor.bootstrap.toolpipeline.tool;

import editor.bootstrap.toolpipeline.util.ToolCanvas;

public interface ToolCanvasProvider {

    /*
     * Supplies what a window that takes tools works on right now, which can
     * change as the window changes mode or layer. Null while the window takes
     * no tools, so the Tools panel shows none.
     */

    ToolCanvas getToolCanvas();
}
