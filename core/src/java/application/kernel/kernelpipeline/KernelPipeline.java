package application.kernel.kernelpipeline;

import application.kernel.kernelpipeline.kernelmanager.KernelManager;
import engine.root.PipelinePackage;

public class KernelPipeline extends PipelinePackage {

    /*
     * Registers the kernel manager, which owns the native kernel library and
     * is the single call site of every C kernel the engine runs.
     */
    @Override
    protected void create() {
        create(KernelManager.class);
    }
}
