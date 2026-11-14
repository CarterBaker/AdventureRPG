package editor.runtime.biomeoutliner.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.worldeditorpipeline.worldbiome.WorldBiomeEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.biomeoutliner.BiomeOutlinerSetting;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeOutlinerMenuSystem extends SystemPackage {

    /*
     * Opens the Biome Outliner menu and lists every biome the world can
     * paint, one row per biome with its swatch and name, the selected one
     * highlighted and every row carrying its biome's name. The rows are
     * listed again only when the palette moves.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldEditorManager worldEditorManager;

    // Menus
    private MenuInstance outlinerMenu;

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
        this.fboManager = get(FBOManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.outlinerMenu = menuManager.openMenu(BiomeOutlinerSetting.MENU_OUTLINER, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (worldEditorManager.getPaletteRevision() == shownPaletteRevision)
            return;

        this.shownPaletteRevision = worldEditorManager.getPaletteRevision();
        refreshRows();
    }

    // Rows \\

    private void refreshRows() {

        for (int i = 0; i < rowElements.size(); i++)
            menuManager.eject(outlinerMenu, BiomeOutlinerSetting.ENTRY_ROWS, rowElements.get(i));

        rowElements.clear();

        ObjectArrayList<WorldBiomeEntryStruct> palette = worldEditorManager.getPalette();

        for (int i = 0; i < palette.size(); i++)
            rowElements.add(injectBiome(palette.get(i)));
    }

    private ElementInstance injectBiome(WorldBiomeEntryStruct entry) {

        String template = entry.getBiomeName().equals(worldEditorManager.getSelectedBiomeName())
                ? BiomeOutlinerSetting.TEMPLATE_BIOME_ROW_SELECTED
                : BiomeOutlinerSetting.TEMPLATE_BIOME_ROW;
        MenuColorStruct swatchColor = new MenuColorStruct(PackedColorUtility.toColor(entry.getMapColor()));

        return menuManager.inject(
                outlinerMenu, BiomeOutlinerSetting.ENTRY_ROWS, template,
                element -> {
                    element.setActionArgOverride(entry.getBiomeName());

                    ElementInstance swatch = element.findChildById(BiomeOutlinerSetting.ELEMENT_BIOME_SWATCH);
                    ElementInstance label = element.findChildById(BiomeOutlinerSetting.ELEMENT_BIOME_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(swatchColor);

                    if (label != null)
                        label.setFontText(entry.getDisplayName());
                });
    }
}
