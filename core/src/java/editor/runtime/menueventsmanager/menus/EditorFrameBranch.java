package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.BranchPackage;

public class EditorFrameBranch extends BranchPackage {

    /*
     * Menu event handlers for the frame every editor window draws in place of
     * the OS one: minimize, maximize or restore, and close, each acting on the
     * OS window the pressed control sits in. Closing takes the OS close
     * button's own path, so the main window still saves its placement on the
     * way out.
     */

    // Frame \\

    public void minimize(WindowInstance window) {
        internal.windowPlatform.minimizeWindow(window.getGLWindow());
    }

    public void toggleMaximized(WindowInstance window) {
        WindowInstance osWindow = window.getGLWindow();
        internal.windowPlatform.setMaximized(osWindow, !internal.windowPlatform.isMaximized(osWindow));
    }

    public void close(WindowInstance window) {
        internal.windowPlatform.requestClose(window.getGLWindow());
    }
}
