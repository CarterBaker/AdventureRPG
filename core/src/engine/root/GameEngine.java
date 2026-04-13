package engine.root;

import application.bootstrap.BootstrapAssembly;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.kernel.KernelAssembly;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeContext;

public class GameEngine extends EnginePackage {

    /*
     * GameEngine defines the concrete game engine instance. Registers kernel
     * infrastructure via KernelAssembly, bootstraps all pipelines via
     * BootstrapAssembly, then creates and pairs the runtime context with the
     * main window in awake(). draw() flushes the main window only — detached
     * windows flush themselves in their own ApplicationListener.render()
     * callback after the engine's full push phase is complete.
     */

    // Kernel
    private KernelAssembly kernelAssembly;

    // Bootstrap
    private BootstrapAssembly bootstrapAssembly;

    // Runtime
    private RuntimeContext runtimeContext;

    // Render
    private WindowManager windowManager;
    private RenderManager renderManager;

    // Kernel \\

    @Override
    protected void kernel() {
        this.kernelAssembly = create(KernelAssembly.class);
    }

    // Bootstrap \\

    @Override
    protected void bootstrap() {
        this.bootstrapAssembly = create(BootstrapAssembly.class);
    }

    // Get \\

    @Override
    protected void get() {

        // Render
        this.windowManager = get(WindowManager.class);
        this.renderManager = get(RenderManager.class);
    }

    // Awake \\

    @Override
    protected void awake() {
        this.runtimeContext = createContext(RuntimeContext.class, windowManager.getMainWindow());
    }

    // Draw \\

    @Override
    protected void draw() {
        renderManager.draw();
    }
}