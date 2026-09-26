package engine.root;

import application.bootstrap.ApplicationBootstrapAssembly;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.screencapturepipeline.screencapturemanager.ScreenCaptureManager;
import application.kernel.ApplicationKernelAssembly;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.EditorBootstrapAssembly;
import editor.runtime.EditorMainWindowContext;

public class EditorEngine extends EnginePackage {

    /*
     * The editor engine. Registers the application kernel and the application
     * and editor bootstrap assemblies, pairs the main window with
     * EditorMainWindowContext, and in draw() flushes the main window and runs
     * the frame's screen capture, the only end-of-frame GPU work.
     */

    // Kernel
    private ApplicationKernelAssembly kernelAssembly;

    // Bootstrap
    private ApplicationBootstrapAssembly bootstrapAssembly;
    private EditorBootstrapAssembly editorBootstrapAssembly;

    // Runtime
    private EditorMainWindowContext editorMainWindowContext;

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

        // Bootstrap
        this.bootstrapAssembly = create(ApplicationBootstrapAssembly.class);
        this.editorBootstrapAssembly = create(EditorBootstrapAssembly.class);
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
        this.editorMainWindowContext = createContext(EditorMainWindowContext.class, windowManager.getMainWindow());
    }

    // Draw \\

    @Override
    protected void draw() {
        renderManager.draw();
        screenCaptureManager.flush();
    }
}