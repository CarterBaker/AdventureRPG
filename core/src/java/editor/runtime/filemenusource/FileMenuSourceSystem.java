package editor.runtime.filemenusource;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenumanager.FileMenuManager;
import engine.root.SystemPackage;

public class FileMenuSourceSystem extends SystemPackage {

    /*
     * Opens this context's window to FileMenuManager for as long as it
     * lives, so the editor's File and Edit menus act on this tool while it is
     * the window worked in last. Every tool that saves, reverts, makes or
     * undoes anything creates one; nothing else opens or closes a window
     * there.
     */

    // Internal
    private FileMenuManager fileMenuManager;

    // Base \\

    @Override
    protected void get() {
        this.fileMenuManager = get(FileMenuManager.class);
    }

    @Override
    protected void awake() {

        if (!(context instanceof FileMenuProvider))
            throwException("A window that takes file actions must name them.");

        fileMenuManager.openWindow(context.getWindow(), (FileMenuProvider) context);
    }

    @Override
    protected void dispose() {
        fileMenuManager.closeWindow(context.getWindow());
    }
}
