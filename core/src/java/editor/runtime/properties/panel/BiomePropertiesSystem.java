package editor.runtime.properties.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.StripLayoutUtility;
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
     * can paint as a grid of swatches, each with its name under it, the one
     * chosen highlighted and every cell carrying its biome's name, as many to
     * a row as the tab's width holds (StripLayoutUtility), and below them the
     * Info rows of the entry selected in the Hierarchy, the biome chosen once
     * a cell is pressed, the status line naming its file. The palette is laid
     * out again only when it moves or the tab's width changes how many fit a
     * row; the Info rows are then taken away first and listed again after it,
     * so they always stand below the palette.
     */

    // Internal
    private MenuManager menuManager;
    private WorldEditorManager worldEditorManager;
    private PropertiesSystem propertiesSystem;
    private InfoPropertiesSystem infoPropertiesSystem;

    // Rows — the grid's rows, the palette they show and how many cells each holds
    private ObjectArrayList<ElementInstance> rowElements;
    private int shownPaletteRevision;
    private int shownPerRow;

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

        int perRow = resolvePerRow();
        boolean relaid = force
                || worldEditorManager.getPaletteRevision() != shownPaletteRevision
                || perRow != shownPerRow;

        if (relaid) {
            clear();
            this.shownPaletteRevision = worldEditorManager.getPaletteRevision();
            this.shownPerRow = perRow;
            injectPalette(perRow);
        }

        return infoPropertiesSystem.refresh(provider, relaid) || relaid;
    }

    @Override
    public String getTitle(PropertiesProvider provider) {
        return PropertiesSetting.TITLE_BIOME_PALETTE;
    }

    @Override
    public void clear() {

        MenuInstance menu = propertiesSystem.getMenu();

        infoPropertiesSystem.clear();

        for (int i = 0; i < rowElements.size(); i++)
            menuManager.eject(menu, PropertiesSetting.ENTRY_ROWS, rowElements.get(i));

        rowElements.clear();
        this.shownPaletteRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownPerRow = EngineSetting.INDEX_NOT_FOUND;
    }

    // Palette \\

    // How many cells a row of the tab's width holds
    private int resolvePerRow() {
        return StripLayoutUtility.resolvePerRow(
                resolveWidth(),
                worldEditorManager.getPalette().size(),
                PropertiesSetting.BIOME_CELL_MIN_WIDTH_PIXELS,
                PropertiesSetting.BIOME_CELL_SPACING_PIXELS);
    }

    private float resolveWidth() {
        ElementInstance rows = propertiesSystem.getMenu().getEntryPoint(PropertiesSetting.ENTRY_ROWS);
        return rows != null ? rows.getComputedW() : 0f;
    }

    private void injectPalette(int perRow) {

        ObjectArrayList<WorldBiomeEntryStruct> palette = worldEditorManager.getPalette();
        DimensionVector2Struct cellSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(StripLayoutUtility.resolveWidth(
                        resolveWidth(),
                        perRow,
                        PropertiesSetting.BIOME_CELL_MAX_WIDTH_PIXELS,
                        PropertiesSetting.BIOME_CELL_SPACING_PIXELS)),
                DimensionValueStruct.ofAbsolute(PropertiesSetting.BIOME_CELL_HEIGHT_PIXELS));
        ElementInstance row = null;

        for (int i = 0; i < palette.size(); i++) {

            if (i % perRow == 0) {
                row = menuManager.inject(
                        propertiesSystem.getMenu(),
                        PropertiesSetting.ENTRY_ROWS,
                        PropertiesSetting.TEMPLATE_BIOME_GRID_ROW);
                rowElements.add(row);
            }

            injectBiome(row, palette.get(i), cellSize);
        }
    }

    private void injectBiome(ElementInstance row, WorldBiomeEntryStruct entry, DimensionVector2Struct cellSize) {

        String template = entry.getBiomeName().equals(worldEditorManager.getSelectedBiomeName())
                ? PropertiesSetting.TEMPLATE_BIOME_CELL_SELECTED
                : PropertiesSetting.TEMPLATE_BIOME_CELL;
        MenuColorStruct swatchColor = new MenuColorStruct(PackedColorUtility.toColor(entry.getMapColor()));

        menuManager.inject(
                row, template,
                element -> {
                    element.setActionArgOverride(entry.getBiomeName());
                    element.setSizeOverride(cellSize);

                    ElementInstance swatch = element.findChildById(PropertiesSetting.ELEMENT_BIOME_SWATCH);
                    ElementInstance label = element.findChildById(PropertiesSetting.ELEMENT_BIOME_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(swatchColor);

                    if (label != null)
                        label.setFontText(entry.getDisplayName());
                });
    }
}
