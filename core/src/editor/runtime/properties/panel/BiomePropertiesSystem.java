package editor.runtime.properties.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.worldeditorpipeline.worldbiome.WorldBiomeEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.properties.PropertiesSetting;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomePropertiesSystem extends SystemPackage implements PropertiesView {

    /*
     * The Properties tab's view of the biome palette while the World Editor
     * paints: every biome the world image can paint, one row per biome with
     * its color swatch and name, the selected one highlighted and every row
     * carrying its biome's name, so choosing one chooses the biome every
     * World Editor paints. The rows are listed again whenever the palette
     * moves.
     */

    // Internal
    private MenuManager menuManager;
    private WorldEditorManager worldEditorManager;
    private PropertiesSystem propertiesSystem;

    // Palette
    private ObjectArrayList<ElementInstance> paletteElements;
    private int shownPaletteRevision;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.paletteElements = new ObjectArrayList<>();
        this.shownPaletteRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.propertiesSystem = get(PropertiesSystem.class);
    }

    // View \\

    @Override
    public boolean refresh(PropertiesProvider provider, boolean force) {

        if (!force && worldEditorManager.getPaletteRevision() == shownPaletteRevision)
            return false;

        this.shownPaletteRevision = worldEditorManager.getPaletteRevision();
        clear();

        ObjectArrayList<WorldBiomeEntryStruct> palette = worldEditorManager.getPalette();

        for (int i = 0; i < palette.size(); i++)
            paletteElements.add(injectBiome(palette.get(i)));

        return true;
    }

    @Override
    public String getTitle(PropertiesProvider provider) {
        return PropertiesSetting.TITLE_BIOMES;
    }

    @Override
    public void clear() {

        MenuInstance menu = propertiesSystem.getMenu();

        for (int i = 0; i < paletteElements.size(); i++)
            menuManager.eject(menu, PropertiesSetting.ENTRY_ROWS, paletteElements.get(i));

        paletteElements.clear();
    }

    // Palette \\

    private ElementInstance injectBiome(WorldBiomeEntryStruct entry) {

        String template = entry.getBiomeName().equals(worldEditorManager.getSelectedBiomeName())
                ? PropertiesSetting.TEMPLATE_BIOME_ROW_SELECTED
                : PropertiesSetting.TEMPLATE_BIOME_ROW;
        MenuColorStruct swatchColor = new MenuColorStruct(PackedColorUtility.toColor(entry.getMapColor()));

        return menuManager.inject(
                propertiesSystem.getMenu(), PropertiesSetting.ENTRY_ROWS, template,
                element -> {
                    element.setActionArgOverride(entry.getBiomeName());

                    ElementInstance swatch = element.findChildById(PropertiesSetting.ELEMENT_BIOME_SWATCH);
                    ElementInstance label = element.findChildById(PropertiesSetting.ELEMENT_BIOME_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(swatchColor);

                    if (label != null)
                        label.setFontText(entry.getDisplayName());
                });
    }
}
