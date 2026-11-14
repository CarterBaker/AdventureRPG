package editor.bootstrap.toolpipeline.tool;

public interface ToolActionProvider {

    /*
     * Runs the actions a window's canvas lists in the Tools panel, by the key
     * each carries, exactly as the window's own button for it does. Every
     * window whose canvas lists actions implements it beside
     * ToolCanvasProvider.
     */

    void runToolAction(String actionKey);
}
