package editor.runtime;

import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class PreviewSystem extends SystemPackage {

    /*
     * Spawns game preview contexts at runtime. Each call to openPreview()
     * opens a new window sized to the main window and binds a fresh
     * RuntimeContext to it.
     */

    private WindowManager windowManager;

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
    }

    public void openPreview() {
        windowManager.openWindow(
                EngineSetting.WINDOW_TITLE + " — Preview",
                application.runtime.RuntimeContext.class);
    }
}