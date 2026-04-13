package editor.runtime;

import application.kernel.util.window.WindowData;
import application.kernel.util.window.WindowInstance;
import application.kernel.windowmanager.WindowManager;
import engine.root.SystemPackage;
import engine.settings.EngineSetting;

public class PreviewSystem extends SystemPackage {

    /*
     * Spawns game preview contexts at runtime. Each call to openPreview()
     * creates a new WindowInstance, registers it with WindowManager, and
     * pairs it with a fresh game RuntimeContext. The game RuntimeContext is
     * unmodified — it reads context.getWindow() to reach its render target,
     * so it behaves identically in the editor as it does in the shipped game.
     */

    // Internal
    private WindowManager windowManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.windowManager = get(WindowManager.class);
    }

    // Preview \\

    void openPreview() {

        WindowInstance mainWindow = windowManager.getMainWindow();

        WindowData data = new WindowData(
                windowManager.issueWindowID(),
                mainWindow.getWidth(),
                mainWindow.getHeight(),
                EngineSetting.WINDOW_TITLE + " — Preview");

        WindowInstance previewWindow = create(WindowInstance.class);
        previewWindow.constructor(data);
        windowManager.registerDetachedWindow(previewWindow);
        internal.createContext(application.runtime.RuntimeContext.class, previewWindow);

    }
}