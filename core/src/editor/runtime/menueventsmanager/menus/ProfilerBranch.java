package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.profiler.ProfilerContext;
import editor.profiler.panel.ProfilerView;
import engine.root.BranchPackage;

public class ProfilerBranch extends BranchPackage {

    /*
     * Menu event handlers for the profiler tab. Each action targets the
     * profiler paired with the window its button was clicked in.
     */

    // View Operations \\

    public void showOverview(WindowInstance window) {
        showView(window, ProfilerView.OVERVIEW);
    }

    public void showSystems(WindowInstance window) {
        showView(window, ProfilerView.SYSTEMS);
    }

    public void showGroups(WindowInstance window) {
        showView(window, ProfilerView.GROUPS);
    }

    public void showGpu(WindowInstance window) {
        showView(window, ProfilerView.GPU);
    }

    public void showThreads(WindowInstance window) {
        showView(window, ProfilerView.THREADS);
    }

    private void showView(WindowInstance window, ProfilerView view) {
        if (window.getContext() instanceof ProfilerContext profilerContext)
            profilerContext.showView(view);
    }

    // Capture Operations \\

    public void togglePause(WindowInstance window) {
        if (window.getContext() instanceof ProfilerContext profilerContext)
            profilerContext.togglePause();
    }

    public void capture(WindowInstance window) {
        if (window.getContext() instanceof ProfilerContext profilerContext)
            profilerContext.capture();
    }
}
