package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;

class WorldBiomeBranch extends BranchPackage {

    /*
     * Carries biome edits from the Info Panel into the engine as they are
     * made, saved or not: an edited, created or reverted biome is rebuilt
     * live and a deleted one retired from the world map, so every map view
     * and window updates at once. An edit the engine refuses leaves the biome
     * as it was and reports why, in the world editor's status and the log.
     */

    // Internal
    private InfoManager infoManager;
    private BiomeManager biomeManager;
    private WorldEditorManager worldEditorManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_BIOMES, this::rebuildBiome);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_BIOMES, this::retireBiome);
    }

    // Live Edit \\

    private void rebuildBiome(InfoEntryStruct entry) {

        try {
            biomeManager.rebuildBiome(entry.getEntryName(), entry.getArpg());
            worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_BIOME_LIVE + entry.getEntryName());
        } catch (InternalException e) {
            refuse(e);
        }
    }

    private void retireBiome(InfoEntryStruct entry) {

        try {
            biomeManager.retireBiome(entry.getEntryName());
            worldEditorManager.setStatusMessage(
                    EditorSetting.WORLD_EDITOR_MESSAGE_BIOME_RETIRED + entry.getEntryName());
        } catch (InternalException e) {
            refuse(e);
        }
    }

    private void refuse(InternalException e) {
        errorLog(e.getMessage());
        worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_BIOME_REFUSED + e.getMessage());
    }
}
