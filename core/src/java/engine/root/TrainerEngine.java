package engine.root;

import java.io.BufferedReader;
import java.io.PrintStream;

import application.bootstrap.ApplicationBootstrapAssembly;
import application.kernel.ApplicationKernelAssembly;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.trainer.TrainerContext;

public class TrainerEngine extends EnginePackage {

    /*
     * The dialogue trainer's engine. Registers the same kernel and bootstrap
     * assemblies as the game, so the trainer talks to exactly the dictionary,
     * replies and models the game builds, and pairs its hidden main window
     * with TrainerContext, which holds practice talks over the console it is
     * handed and closes the engine when the trainer leaves.
     */

    // Console
    private final PrintStream console;
    private final BufferedReader input;

    // Kernel
    private ApplicationKernelAssembly kernelAssembly;

    // Bootstrap
    private ApplicationBootstrapAssembly bootstrapAssembly;

    // Runtime
    private TrainerContext trainerContext;

    // Render
    private WindowManager windowManager;

    // Constructor \\

    public TrainerEngine(PrintStream console, BufferedReader input) {

        // Console
        this.console = console;
        this.input = input;
    }

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
    }

    // Awake \\

    @Override
    protected void awake() {
        this.trainerContext = createContext(TrainerContext.class, windowManager.getMainWindow());
        trainerContext.bindConsole(console, input);
    }
}
