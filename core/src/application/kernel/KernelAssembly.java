package application.kernel;

import application.kernel.inputpipeline.InputPipeline;
import application.kernel.threadpipeline.ThreadPipeline;

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
        create(InputPipeline.class);
    }
}