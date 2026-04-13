package application.kernel.windowpipeline;

import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.PipelinePackage;

public class WindowPipeline extends PipelinePackage {
    /*
     * Registers the window manager which owns all engine windows and
     * drives context switching, focus tracking, and window lifecycle.
     */
    @Override
    protected void create() {
        create(WindowManager.class);
    }
}