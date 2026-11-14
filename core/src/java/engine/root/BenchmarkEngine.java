package engine.root;

import application.benchmark.BenchmarkContext;
import application.bootstrap.ApplicationBootstrapAssembly;
import application.kernel.ApplicationKernelAssembly;
import application.kernel.kernelpipeline.kernelmanager.KernelManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;

public class BenchmarkEngine extends EnginePackage {

    /*
     * The benchmark engine. Registers the same kernel and bootstrap
     * assemblies as the game, so every routine runs on exactly the data the
     * game builds, and pairs its hidden main window with BenchmarkContext,
     * which measures the streaming routines once and closes the engine. Once
     * it has shut down it hands back the report, the checksum of each
     * routine's output and the CPU level its kernels ran at, so runs at
     * different levels are compared.
     */

    // Kernel
    private ApplicationKernelAssembly kernelAssembly;

    // Bootstrap
    private ApplicationBootstrapAssembly bootstrapAssembly;

    // Runtime
    private BenchmarkContext benchmarkContext;

    // Render
    private WindowManager windowManager;

    // Kernels
    private KernelManager kernelManager;

    // Kernel \\

    @Override
    protected void kernel() {
        this.kernelAssembly = create(ApplicationKernelAssembly.class);
    }

    // Bootstrap \\

    @Override
    protected void bootstrap() {
        this.bootstrapAssembly = create(ApplicationBootstrapAssembly.class);
    }

    // Get \\

    @Override
    protected void get() {

        // Render
        this.windowManager = get(WindowManager.class);

        // Kernels
        this.kernelManager = get(KernelManager.class);
    }

    // Awake \\

    @Override
    protected void awake() {
        this.benchmarkContext = createContext(BenchmarkContext.class, windowManager.getMainWindow());
    }

    // Accessible \\

    public String getReport() {
        return benchmarkContext.getReport();
    }

    public long getGenerationChecksum() {
        return benchmarkContext.getGenerationChecksum();
    }

    public long getMeshingChecksum() {
        return benchmarkContext.getMeshingChecksum();
    }

    public int getKernelCpuLevel() {
        return kernelManager.getCpuLevel();
    }
}
