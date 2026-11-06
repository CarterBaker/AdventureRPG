package editor.runtime.biomepalette.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.worldeditorpipeline.worldbiome.WorldBiomeEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.biomepalette.BiomePaletteSetting;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomePaletteMenuSystem extends SystemPackage {

    /*
     * Opens the Biome Palette menu. When the palette moves it lists every
     * biome the World Editor can paint, one row per biome with its color
     * swatch and name, the selected one highlighted and every row carrying
     * its biome's name.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldEditorManager worldEditorManager;

    // Menus
    private MenuInstance paletteMenu;

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
        this.fboManager = get(FBOManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.paletteMenu = menuManager.openMenu(BiomePaletteSetting.MENU_BIOME_PALETTE, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (worldEditorManager.getPaletteRevision() == shownPaletteRevision)
            return;

        shownPaletteRevision = worldEditorManager.getPaletteRevision();
        refreshPalette();
    }

    // Palette \\

    private void refreshPalette() {

        for (int i = 0; i < paletteElements.size(); i++)
            menuManager.eject(paletteMenu, BiomePaletteSetting.ENTRY_ROWS, paletteElements.get(i));

        paletteElements.clear();

        ObjectArrayList<WorldBiomeEntryStruct> palette = worldEditorManager.getPalette();

        for (int i = 0; i < palette.size(); i++)
            paletteElements.add(injectBiome(palette.get(i)));
    }

    private ElementInstance injectBiome(WorldBiomeEntryStruct entry) {

        String template = entry.getBiomeName().equals(worldEditorManager.getSelectedBiomeName())
                ? BiomePaletteSetting.TEMPLATE_BIOME_ROW_SELECTED
                : BiomePaletteSetting.TEMPLATE_BIOME_ROW;
        MenuColorStruct swatchColor = new MenuColorStruct(PackedColorUtility.toColor(entry.getMapColor()));

        return menuManager.inject(
                paletteMenu, BiomePaletteSetting.ENTRY_ROWS, template,
                element -> {
                    element.setActionArgOverride(entry.getBiomeName());

                    ElementInstance swatch = element.findChildById(BiomePaletteSetting.ELEMENT_BIOME_SWATCH);
                    ElementInstance label = element.findChildById(BiomePaletteSetting.ELEMENT_BIOME_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(swatchColor);

                    if (label != null)
                        label.setFontText(entry.getDisplayName());
                });
    }
}
