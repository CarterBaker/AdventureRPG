package launcher.bootstrap.launcherpipeline.launchermanager;

import engine.root.ManagerPackage;

public class LauncherManager extends ManagerPackage {

    /*
     * Owns the launcher's one decision: whether the game starts once the
     * launcher closes. Launch and Quit both close the launcher's window, so
     * the engine shuts down through the end of its loop rather than in the
     * middle of a frame; only Launch leaves the game to start after it in the
     * same JVM.
     */

    // State
    private boolean launchRequested;

    // Management \\

    public void launch() {
        this.launchRequested = true;
        internal.windowPlatform.exit();
    }

    public void quit() {
        internal.windowPlatform.exit();
    }

    // Accessible \\

    public boolean isLaunchRequested() {
        return launchRequested;
    }
}
