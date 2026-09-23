package engine.root;

import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.screencapturepipeline.screencapturemanager.ScreenCaptureManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.editor.EditorWindowContext;

public class EditorEngine extends EnginePackage {

    /*
     * EditorEngine defines the concrete editor engine instance. Registers
     * kernel infrastructure via KernelAssembly, bootstraps both the shared
     * game pipeline and the editor-specific pipeline via their respective
     * BootstrapAssemblies, then pairs the main window with the same
     * EditorWindowContext every secondary editor window uses, in awake().
     * draw() flushes the main window only — detached windows flush themselves
     * in their own ApplicationListener.render() callback after the engine's
     * full push phase is complete — and is also the engine's sole authority
     * over end-of-frame GPU work: it is the only place
     * ScreenCaptureManager.flush() is ever called, after the frame's own
     * render pass has fully drawn and presented, so screen capture never
     * touches the GPU from anywhere else in the frame.
     */

    // Kernel
    private application.kernel.ApplicationKernelAssembly kernelAssembly;
    private editor.kernel.EditorKernelAssembly editorKernelAssembly;

    // Bootstrap
    private application.bootstrap.ApplicationBootstrapAssembly bootstrapAssembly;
    private editor.bootstrap.EditorBootstrapAssembly editorBootstrapAssembly;

    // Runtime
    private EditorWindowContext editorWindowContext;

    // Render
    private WindowManager windowManager;
    private RenderManager renderManager;
    private ScreenCaptureManager screenCaptureManager;

    // Kernel \\

    @Override
    protected void kernel() {
        this.kernelAssembly = create(application.kernel.ApplicationKernelAssembly.class);
        this.editorKernelAssembly = create(editor.kernel.EditorKernelAssembly.class);
    }

    // Bootstrap \\

    @Override
    protected void bootstrap() {

        // Bootstrap
        this.bootstrapAssembly = create(application.bootstrap.ApplicationBootstrapAssembly.class);
        this.editorBootstrapAssembly = create(editor.bootstrap.EditorBootstrapAssembly.class);
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
        this.editorWindowContext = createContext(EditorWindowContext.class, windowManager.getMainWindow());
    }

    // Draw \\

    @Override
    protected void draw() {
        renderManager.draw();
        screenCaptureManager.flush();
    }
}