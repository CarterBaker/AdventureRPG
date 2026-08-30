package engine.root;

import application.bootstrap.ApplicationBootstrapAssembly;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.screencapturepipeline.screencapturemanager.ScreenCaptureManager;
import application.kernel.ApplicationKernelAssembly;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeContext;

public class GameEngine extends EnginePackage {

    /*
     * GameEngine defines the concrete game engine instance. Registers kernel
     * infrastructure via KernelAssembly, bootstraps all pipelines via
     * BootstrapAssembly, then creates and pairs the runtime context with the
     * main window in awake(). draw() flushes the main window only — detached
     * windows flush themselves in their own ApplicationListener.render()
     * callback after the engine's full push phase is complete — and is also
     * the engine's sole authority over end-of-frame GPU work: it is the only
     * place ScreenCaptureManager.flush() is ever called, after the frame's
     * own render pass has fully drawn and presented, so screen capture never
     * touches the GPU from anywhere else in the frame.
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
    private ScreenCaptureManager screenCaptureManager;

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
        this.screenCaptureManager = get(ScreenCaptureManager.class);
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
        screenCaptureManager.flush();
    }
}