package engine.root;

import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.runtime.EditorWindowMain;

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
    private application.kernel.KernelAssembly kernelAssembly;
    private editor.kernel.KernelAssembly editorKernelAssembly;

    // Bootstrap
    private application.bootstrap.BootstrapAssembly bootstrapAssembly;
    private editor.bootstrap.BootstrapAssembly editorBootstrapAssembly;

    // Runtime
    private EditorWindowMain runtimeContext;

    // Render
    private WindowManager windowManager;
    private RenderManager renderManager;

    // Kernel \\

    @Override
    protected void kernel() {
        this.kernelAssembly = create(application.kernel.KernelAssembly.class);
        this.editorKernelAssembly = create(editor.kernel.KernelAssembly.class);
    }

    // Bootstrap \\

    @Override
    protected void bootstrap() {

        // Bootstrap
        this.bootstrapAssembly = create(application.bootstrap.BootstrapAssembly.class);
        this.editorBootstrapAssembly = create(editor.bootstrap.BootstrapAssembly.class);
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