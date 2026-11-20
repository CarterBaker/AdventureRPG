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
     * The Properties tab's view of the World Editor's Biomes layer, as a
     * terrain tool's inspector holds its paint palette: every biome the world
     * can paint, one row with its swatch and name, the one chosen
     * highlighted and every row carrying its biome's name, and below them
     * the Info rows of the entry selected in the Hierarchy, the biome chosen
     * once a row is pressed. The palette is listed again only when it moves;
     * the Info rows are then taken away first and listed again after it, so
     * they always stand below the palette.
     */

    // Internal
    private MenuManager menuManager;
    private WorldEditorManager worldEditorManager;
    private PropertiesSystem propertiesSystem;
    private InfoPropertiesSystem infoPropertiesSystem;

    // Rows
    private ObjectArrayList<ElementInstance> rowElements;
    private int shownPaletteRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.rowElements = new ObjectArrayList<>();
        this.shownPaletteRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.propertiesSystem = get(PropertiesSystem.class);
        this.infoPropertiesSystem = get(InfoPropertiesSystem.class);
    }

    // View \\

    @Override
    public boolean refresh(PropertiesProvider provider, boolean force) {

        boolean relaid = force || worldEditorManager.getPaletteRevision() != shownPaletteRevision;

        if (relaid) {
            clear();
            this.shownPaletteRevision = worldEditorManager.getPaletteRevision();
            injectPalette();
        }

        return infoPropertiesSystem.refresh(provider, relaid) || relaid;
    }

    @Override
    public String getTitle(PropertiesProvider provider) {
        return infoPropertiesSystem.getTitle(provider);
    }

    @Override
    public void clear() {

        MenuInstance menu = propertiesSystem.getMenu();

        infoPropertiesSystem.clear();

        for (int i = 0; i < rowElements.size(); i++)
            menuManager.eject(menu, PropertiesSetting.ENTRY_ROWS, rowElements.get(i));

        rowElements.clear();
        this.shownPaletteRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    // Palette \\

    private void injectPalette() {

        ObjectArrayList<WorldBiomeEntryStruct> palette = worldEditorManager.getPalette();

        for (int i = 0; i < palette.size(); i++)
            rowElements.add(injectBiome(palette.get(i)));
    }

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
