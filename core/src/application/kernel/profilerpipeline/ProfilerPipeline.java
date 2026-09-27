package application.kernel.profilerpipeline;

import application.kernel.profilerpipeline.profilermanager.ProfilerManager;
import engine.root.PipelinePackage;

public class ProfilerPipeline extends PipelinePackage {

    /*
     * Bootstraps the engine profiler: a single ProfilerManager measuring the
     * frame, every system, the GPU, the thread pools and the heap for the
     * whole engine loop, game and editor alike.
     */

    @Override
    protected void create() {
        create(ProfilerManager.class);
    }
}
