package launcher.bootstrap.launcherpipeline;

import engine.root.PipelinePackage;
import launcher.bootstrap.launcherpipeline.launchermanager.LauncherManager;

public class LauncherPipeline extends PipelinePackage {

    /*
     * Registers LauncherManager, which holds whether the game starts once the
     * launcher closes.
     */

    // Internal \\

    @Override
    protected void create() {
        create(LauncherManager.class);
    }
}
