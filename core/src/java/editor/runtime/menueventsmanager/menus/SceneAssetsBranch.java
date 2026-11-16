package editor.runtime.menueventsmanager.menus;

import editor.bootstrap.sceneassetspipeline.sceneassetsmanager.SceneAssetsManager;
import editor.runtime.sceneassets.SceneAssetsSetting;
import engine.root.BranchPackage;

public class SceneAssetsBranch extends BranchPackage {

    /*
     * Menu event handlers for the Scene Assets tab. A section's head opens or
     * closes it, handing back its place among the sections; a row naming
     * content opens it in the Properties tab, handing back its section's
     * place and its own as one key.
     */

    // Internal
    private SceneAssetsManager sceneAssetsManager;

    // Base \\

    @Override
    protected void get() {
        this.sceneAssetsManager = get(SceneAssetsManager.class);
    }

    // Scene Assets \\

    public void toggleSection(String sectionKey) {
        sceneAssetsManager.toggleSection(Integer.parseInt(sectionKey));
    }

    public void selectRow(String rowKey) {

        int separator = rowKey.indexOf(SceneAssetsSetting.ROW_KEY_SEPARATOR);

        sceneAssetsManager.selectRow(
                Integer.parseInt(rowKey.substring(0, separator)),
                Integer.parseInt(rowKey.substring(separator + 1)));
    }
}
