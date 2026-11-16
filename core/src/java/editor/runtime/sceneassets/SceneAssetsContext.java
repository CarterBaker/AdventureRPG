package editor.runtime.sceneassets;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.sceneassets.panel.SceneAssetsPanelSystem;
import engine.root.ContextPackage;

public class SceneAssetsContext extends ContextPackage implements PropertiesProvider {

    /*
     * Editor tab listing every asset the Dev window shows as the game runs:
     * the block under the crosshair in full — its block, biome, face
     * textures, material and shader — and every block, biome, texture,
     * material and shader in view, section by section. A row naming content
     * opens it in the Properties tab, so anything that looks wrong in play is
     * traced to the file that draws it. All state lives in
     * SceneAssetsManager; this tab only shows it and hands clicks to
     * SceneAssetsBranch.
     */

    // Internal
    private SceneAssetsPanelSystem sceneAssetsPanelSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.sceneAssetsPanelSystem = create(SceneAssetsPanelSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Properties \\

    // What a row opens is content shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }
}
