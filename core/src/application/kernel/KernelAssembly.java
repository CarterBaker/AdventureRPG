package application.kernel;

import application.kernel.threadpipeline.ThreadPipeline;
import application.kernel.windowpipeline.WindowPipeline;
import engine.root.AssemblyPackage;

public class KernelAssembly extends AssemblyPackage {
    /*
     * Creates and owns all kernel pipelines in dependency order.
     * Thread infrastructure is registered before windowing since
     * the window manager may depend on async execution.
     */
    @Override
    public void create() {
        create(ThreadPipeline.class);
        create(WindowPipeline.class);
    }
}