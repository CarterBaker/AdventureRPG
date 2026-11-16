package editor.bootstrap.sceneassetspipeline;

import editor.bootstrap.sceneassetspipeline.sceneassetsmanager.SceneAssetsManager;
import engine.root.PipelinePackage;

public class SceneAssetsPipeline extends PipelinePackage {

    /*
     * Registers the scene assets' shared state, so every Scene Assets tab
     * shows what the same Dev window sees.
     */

    // Internal \\

    @Override
    protected void create() {
        create(SceneAssetsManager.class);
    }
}
