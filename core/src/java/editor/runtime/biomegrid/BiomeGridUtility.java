package editor.runtime.biomegrid;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import editor.bootstrap.biomeplannerpipeline.biomeplannermanager.BiomePlannerManager;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class BiomeGridUtility extends EngineUtility {

    /*
     * The one way a planner tab shows the biome planner's grid, the Biome
     * Planner and the Road Planner alike: every cell in its biome's color,
     * named by its biome and whatever the tab marks it with.
     */

    // Cells \\

    public static void refreshCell(
            ElementInstance element,
            int cell,
            String suffix,
            BiomePlannerManager biomePlannerManager,
            WorldEditorManager worldEditorManager) {

        if (element == null)
            return;

        ElementInstance swatch = element.findChildById(EditorSetting.BIOME_GRID_ELEMENT_CELL_SWATCH);
        ElementInstance label = element.findChildById(EditorSetting.BIOME_GRID_ELEMENT_CELL_LABEL);
        int mapColor = biomePlannerManager.getCellMapColor(cell);

        if (swatch != null && mapColor != EngineSetting.INDEX_NOT_FOUND)
            swatch.setColorOverride(new MenuColorStruct(PackedColorUtility.toColor(mapColor)));

        if (label != null)
            label.setFontText(worldEditorManager.getBiomeDisplayName(biomePlannerManager.getCellBiomeName(cell))
                    + suffix);
    }
}
