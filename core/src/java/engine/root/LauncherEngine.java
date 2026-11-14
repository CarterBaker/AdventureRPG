package engine.root;

import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.kernel.ApplicationKernelAssembly;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.util.io.AssetManifestStruct;
import engine.util.io.AssetManifestUtility;
import launcher.bootstrap.LauncherBootstrapAssembly;
import launcher.bootstrap.launcherpipeline.launchermanager.LauncherManager;
import launcher.runtime.LauncherContext;
import launcher.runtime.LauncherSetting;

public class LauncherEngine extends EnginePackage {

    /*
     * The launcher engine. Registers the application kernel and the
     * launcher's own bootstrap assembly, which holds only what menus draw
     * with, pairs the main window with LauncherContext, and in draw() flushes
     * the main window. Its asset manifest queues only what those menus draw
     * with. Nothing of the game is loaded and nothing streams, so a player
     * can always reach its options however high the game's settings are. Once
     * it has shut down it answers whether the game starts next.
     */

    // Kernel
    private ApplicationKernelAssembly kernelAssembly;

    // Bootstrap
    private LauncherBootstrapAssembly launcherBootstrapAssembly;

    // Runtime
    private LauncherContext launcherContext;

    // Render
    private WindowManager windowManager;
    private RenderManager renderManager;

    // Launch
    private LauncherManager launcherManager;

    // Kernel \\

    @Override
    protected void kernel() {
        this.kernelAssembly = create(ApplicationKernelAssembly.class);
    }

    // Bootstrap \\

    @Override
    protected void bootstrap() {
        this.launcherBootstrapAssembly = create(LauncherBootstrapAssembly.class);
    }

    // Get \\

    @Override
    protected void get() {

        // Render
        this.windowManager = get(WindowManager.class);
        this.renderManager = get(RenderManager.class);

        // Launch
        this.launcherManager = get(LauncherManager.class);
    }

    // Awake \\

    @Override
    protected void awake() {
        this.launcherContext = createContext(LauncherContext.class, windowManager.getMainWindow());
    }

    // Assets \\

    @Override
    protected AssetManifestStruct assetManifest() {
        return AssetManifestUtility.load(LauncherSetting.ASSET_MANIFEST);
    }

    // Draw \\

    @Override
    protected void draw() {
        renderManager.draw();
    }

    // Accessible \\

    public boolean isLaunchRequested() {
        return launcherManager.isLaunchRequested();
    }
}
