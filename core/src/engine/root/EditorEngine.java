package engine.root;

import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.kernel.windowmanager.WindowManager;
import editor.runtime.RuntimeContext;

public class EditorEngine extends EnginePackage {

    /*
     * EditorEngine defines the concrete editor engine instance. Bootstraps
     * both the shared game pipeline and the editor-specific pipeline via
     * their respective BootstrapAssemblies, then creates the editor context
     * paired with the main window in awake(). draw() flushes the main window
     * only — detached windows flush themselves in their own
     * ApplicationListener.render() callback after the engine's full push
     * phase is complete.
     */

    // Bootstrap
    private application.bootstrap.BootstrapAssembly bootstrapAssembly;
    private editor.bootstrap.BootstrapAssembly editorBootstrapAssembly;

    // Runtime
    private RuntimeContext runtimeContext;

    // Render
    private WindowManager windowManager;
    private RenderManager renderManager;

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
        this.runtimeContext = createContext(RuntimeContext.class, windowManager.getMainWindow());
    }

    // Draw \\

    @Override
    void draw() {
        renderManager.draw();
    }
}