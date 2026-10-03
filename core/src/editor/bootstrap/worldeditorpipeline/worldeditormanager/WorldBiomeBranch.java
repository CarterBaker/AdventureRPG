package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
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
     * Biome selection runs both ways: a biome chosen in the world editor or
     * clicked on a map is selected in the hierarchy, and a biome selected
     * there becomes the world editor's brush when it paints the world.
     */

    // Internal
    private InfoManager infoManager;
    private BiomeManager biomeManager;
    private WorldEditorManager worldEditorManager;

    // Scratch
    private BiomeBlendStruct blend;

    // Base \\

    @Override
    protected void create() {
        this.blend = new BiomeBlendStruct();
    }

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
        infoManager.addSelectionListener(
                EditorSetting.INFO_SCHEMA_BIOMES,
                entry -> worldEditorManager.applyBiomeSelection(entry.getEntryName()));
    }

    // Selection \\

    void selectInHierarchy(String biomeName) {
        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_BIOMES, biomeName, biomeName);
    }

    // The biome that dominates the field at a world position, as generation resolves it
    void selectBiomeAt(WorldHandle worldHandle, double worldX, double worldZ) {

        biomeManager.sampleBiomeField(
                worldHandle,
                WorldWrapUtility.wrapBlockX(worldHandle, worldX),
                WorldWrapUtility.wrapBlockZ(worldHandle, worldZ),
                blend);

        selectInHierarchy(blend.getDominantBiome().getBiomeName());
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
