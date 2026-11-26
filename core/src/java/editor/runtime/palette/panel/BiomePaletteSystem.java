package editor.runtime.palette.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.StripLayoutUtility;
import editor.bootstrap.propertiespipeline.properties.PaletteProvider;
import editor.bootstrap.worldeditorpipeline.worldbiome.WorldBiomeEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.palette.PaletteSetting;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomePaletteSystem extends SystemPackage implements PaletteView {

    /*
     * The Palette tab's view of the World Editor's Biomes layer, as a terrain
     * tool's paint palette: every biome the world can paint as a grid of
     * swatches, each with its name under it, the one chosen highlighted and
     * every cell carrying its biome's name, as many to a row as the tab's
     * width holds (StripLayoutUtility). The biome chosen is selected in the
     * Hierarchy as the palette is shown and whenever a cell is pressed, so
     * the Properties tab holds its fields. The grid is laid out again only
     * when the palette moves or the tab's width changes how many fit a row.
     */

    // Internal
    private MenuManager menuManager;
    private WorldEditorManager worldEditorManager;
    private PaletteSystem paletteSystem;

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
        this.shownPerRow = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.paletteSystem = get(PaletteSystem.class);
    }

    // View \\

    @Override
    public boolean refresh(PaletteProvider provider, boolean force) {

        if (force) {
            paletteSystem.placeRows(false);
            worldEditorManager.showChosenBiome();
        }

        int perRow = resolvePerRow();

        if (!force && worldEditorManager.getPaletteRevision() == shownPaletteRevision && perRow == shownPerRow)
            return false;

        clear();
        this.shownPaletteRevision = worldEditorManager.getPaletteRevision();
        this.shownPerRow = perRow;
        injectPalette(perRow);

        return true;
    }

    @Override
    public void clear() {

        MenuInstance menu = paletteSystem.getMenu();

        for (int i = 0; i < rowElements.size(); i++)
            menuManager.eject(menu, PaletteSetting.ENTRY_ROWS, rowElements.get(i));

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
                PaletteSetting.BIOME_CELL_MIN_WIDTH_PIXELS,
                PaletteSetting.BIOME_CELL_SPACING_PIXELS);
    }

    private float resolveWidth() {
        ElementInstance rows = paletteSystem.getMenu().getEntryPoint(PaletteSetting.ENTRY_ROWS);
        return rows != null ? rows.getComputedW() : 0f;
    }

    private void injectPalette(int perRow) {

        ObjectArrayList<WorldBiomeEntryStruct> palette = worldEditorManager.getPalette();
        DimensionVector2Struct cellSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(StripLayoutUtility.resolveWidth(
                        resolveWidth(),
                        perRow,
                        PaletteSetting.BIOME_CELL_MAX_WIDTH_PIXELS,
                        PaletteSetting.BIOME_CELL_SPACING_PIXELS)),
                DimensionValueStruct.ofAbsolute(PaletteSetting.BIOME_CELL_HEIGHT_PIXELS));
        ElementInstance row = null;

        for (int i = 0; i < palette.size(); i++) {

            if (i % perRow == 0) {
                row = menuManager.inject(
                        paletteSystem.getMenu(),
                        PaletteSetting.ENTRY_ROWS,
                        PaletteSetting.TEMPLATE_BIOME_GRID_ROW);
                rowElements.add(row);
            }

            injectBiome(row, palette.get(i), cellSize);
        }
    }

    private void injectBiome(ElementInstance row, WorldBiomeEntryStruct entry, DimensionVector2Struct cellSize) {

        String template = entry.getBiomeName().equals(worldEditorManager.getSelectedBiomeName())
                ? PaletteSetting.TEMPLATE_BIOME_CELL_SELECTED
                : PaletteSetting.TEMPLATE_BIOME_CELL;
        MenuColorStruct swatchColor = new MenuColorStruct(PackedColorUtility.toColor(entry.getMapColor()));

        menuManager.inject(
                row, template,
                element -> {
                    element.setActionArgOverride(entry.getBiomeName());
                    element.setSizeOverride(cellSize);

                    ElementInstance swatch = element.findChildById(PaletteSetting.ELEMENT_BIOME_SWATCH);
                    ElementInstance label = element.findChildById(PaletteSetting.ELEMENT_BIOME_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(swatchColor);

                    if (label != null)
                        label.setFontText(entry.getDisplayName());
                });
    }
}
