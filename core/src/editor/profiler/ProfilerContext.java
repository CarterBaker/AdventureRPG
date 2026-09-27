package editor.profiler;

import editor.profiler.panel.ProfilerPanelSystem;
import editor.profiler.panel.ProfilerView;
import engine.root.ContextPackage;

public class ProfilerContext extends ContextPackage {

    /*
     * Editor tab showing the engine profiler live: a frame time graph over the
     * last few seconds and a table of the chosen view. While the tab is open
     * the profiler samples every system, the GPU, the thread pools and the
     * heap; a capture records a fixed stretch of frames into a report.
     */

    // Internal
    private ProfilerPanelSystem profilerPanelSystem;

    // Internal \\

    @Override
    protected void create() {
        this.profilerPanelSystem = create(ProfilerPanelSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void showView(ProfilerView view) {
        profilerPanelSystem.showView(view);
    }

    public void togglePause() {
        profilerPanelSystem.togglePause();
    }

    public void capture() {
        profilerPanelSystem.capture();
    }
}
