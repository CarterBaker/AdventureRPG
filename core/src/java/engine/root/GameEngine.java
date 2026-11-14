package engine.root;

import application.bootstrap.ApplicationBootstrapAssembly;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.kernel.ApplicationKernelAssembly;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeContext;

public class GameEngine extends EnginePackage {

    /*
     * The game engine. Registers the application kernel and bootstrap
     * assemblies, pairs the main window with RuntimeContext, and in draw()
     * flushes the main window.
     */

    // Kernel
    private ApplicationKernelAssembly kernelAssembly;

    // Bootstrap
    private ApplicationBootstrapAssembly bootstrapAssembly;

    // Runtime
    private RuntimeContext runtimeContext;

    // Render
    private WindowManager windowManager;
    private RenderManager renderManager;

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