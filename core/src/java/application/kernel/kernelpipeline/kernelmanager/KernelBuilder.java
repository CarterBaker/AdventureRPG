package application.kernel.kernelpipeline.kernelmanager;

import application.kernel.kernelpipeline.kernel.KernelData;
import application.kernel.kernelpipeline.kernel.KernelHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;

class KernelBuilder extends BuilderPackage {

    /*
     * Builds a KernelHandle for one resolved kernel: its name, ID, variant
     * symbol and function address, with the profiler counter names it reports
     * under built here once.
     */

    // Build \\

    KernelHandle build(String kernelName, int kernelID, String symbolName, long functionAddress) {

        KernelData kernelData = new KernelData(
                kernelName,
                kernelID,
                symbolName,
                functionAddress,
                String.format(EngineSetting.KERNEL_PROFILER_CALLS_FORMAT, kernelName),
                String.format(EngineSetting.KERNEL_PROFILER_MILLIS_FORMAT, kernelName));
        KernelHandle kernelHandle = create(KernelHandle.class);
        kernelHandle.constructor(kernelData);

        return kernelHandle;
    }
}
