package engine.root;

import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.editor.EditorWindowMain;

public class EditorEngine extends EnginePackage {

    /*
     * EditorEngine defines the concrete editor engine instance. Registers
     * kernel infrastructure via KernelAssembly, bootstraps both the shared
     * game pipeline and the editor-specific pipeline via their respective
     * BootstrapAssemblies, then creates the editor context paired with the
     * main window in awake(). draw() flushes the main window only — detached
     * windows flush themselves in their own ApplicationListener.render()
     * callback after the engine's full push phase is complete.
     */

    // Kernel
    private application.kernel.ApplicationKernelAssembly kernelAssembly;
    private editor.kernel.EditorKernelAssembly editorKernelAssembly;

    // Bootstrap
    private application.bootstrap.ApplicationBootstrapAssembly bootstrapAssembly;
    private editor.bootstrap.EditorBootstrapAssembly editorBootstrapAssembly;

    // Runtime
    private EditorWindowMain runtimeContext;

    // Render
    private WindowManager windowManager;
    private RenderManager renderManager;

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
    }

    // Awake \\

    @Override
    protected void awake() {
        this.runtimeContext = createContext(EditorWindowMain.class, windowManager.getMainWindow());
    }

    // Draw \\

    @Override
    protected void draw() {
        renderManager.draw();
    }
}