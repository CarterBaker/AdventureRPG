package application.kernel.frameratepipeline;

import application.kernel.frameratepipeline.frameratemanager.FrameRateManager;
import engine.root.PipelinePackage;

public class FrameRatePipeline extends PipelinePackage {

    /*
     * Bootstraps frame timing: a single FrameRateManager owning frame
     * pacing and FPS measurement for the entire engine loop.
     */

    @Override
    protected void create() {
        create(FrameRateManager.class);
    }
}