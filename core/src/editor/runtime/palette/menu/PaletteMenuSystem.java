package editor.runtime.palette.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;
import editor.bootstrap.worldeditorpipeline.worldbiome.WorldBiomeEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.bootstrap.worldeditorpipeline.worldheight.WorldHeightEntryStruct;
import editor.runtime.palette.PaletteSetting;
import engine.graphics.color.Color;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PaletteMenuSystem extends SystemPackage {

    /*
     * Opens the Palette menu. When the palette moves it lists what the World
     * Editor's active layer can paint under that layer's title: every biome,
     * one row per biome with its color swatch and name, or every height
     * level, one row per level with its grey swatch and its name, the
     * selected one highlighted and every row carrying its biome's name or its
     * level.
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
        this.paletteMenu = menuManager.openMenu(PaletteSetting.MENU_PALETTE, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (worldEditorManager.getPaletteRevision() == shownPaletteRevision)
            return;

        shownPaletteRevision = worldEditorManager.getPaletteRevision();
        refreshTitle();
        refreshPalette();
    }

    // Palette \\

    private void refreshTitle() {

        ElementInstance label = paletteMenu.getEntryPoint(PaletteSetting.ENTRY_TITLE);

        if (label != null)
            label.setFontText(worldEditorManager.getLayer().getLabel());
    }

    private void refreshPalette() {

        for (int i = 0; i < paletteElements.size(); i++)
            menuManager.eject(paletteMenu, PaletteSetting.ENTRY_ROWS, paletteElements.get(i));

        paletteElements.clear();

        if (worldEditorManager.getLayer() == WorldEditorLayer.HEIGHTS) {

            ObjectArrayList<WorldHeightEntryStruct> heightPalette = worldEditorManager.getHeightPalette();

            for (int i = 0; i < heightPalette.size(); i++)
                paletteElements.add(injectHeight(heightPalette.get(i)));

            return;
        }

        ObjectArrayList<WorldBiomeEntryStruct> palette = worldEditorManager.getPalette();

        for (int i = 0; i < palette.size(); i++)
            paletteElements.add(injectBiome(palette.get(i)));
    }

    private ElementInstance injectBiome(WorldBiomeEntryStruct entry) {

        String template = entry.getBiomeName().equals(worldEditorManager.getSelectedBiomeName())
                ? PaletteSetting.TEMPLATE_BIOME_ROW_SELECTED
                : PaletteSetting.TEMPLATE_BIOME_ROW;
        MenuColorStruct swatchColor = toSwatchColor(entry.getMapColor());

        return menuManager.inject(
                paletteMenu, PaletteSetting.ENTRY_ROWS, template,
                element -> {
                    element.setActionArgOverride(entry.getBiomeName());

                    ElementInstance swatch = element.findChildById(PaletteSetting.ELEMENT_BIOME_SWATCH);
                    ElementInstance label = element.findChildById(PaletteSetting.ELEMENT_BIOME_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(swatchColor);

                    if (label != null)
                        label.setFontText(entry.getDisplayName());
                });
    }

    private ElementInstance injectHeight(WorldHeightEntryStruct entry) {

        String template = entry.getLevel() == worldEditorManager.getSelectedHeightLevel()
                ? PaletteSetting.TEMPLATE_HEIGHT_ROW_SELECTED
                : PaletteSetting.TEMPLATE_HEIGHT_ROW;
        int level = entry.getLevel();
        MenuColorStruct swatchColor = toSwatchColor(PackedColorUtility.pack(level, level, level));

        return menuManager.inject(
                paletteMenu, PaletteSetting.ENTRY_ROWS, template,
                element -> {
                    element.setActionArgOverride(Integer.toString(entry.getLevel()));

                    ElementInstance swatch = element.findChildById(PaletteSetting.ELEMENT_HEIGHT_SWATCH);
                    ElementInstance label = element.findChildById(PaletteSetting.ELEMENT_HEIGHT_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(swatchColor);

                    if (label != null)
                        label.setFontText(entry.getDisplayName());
                });
    }

    // Utility \\

    private MenuColorStruct toSwatchColor(int packedColor) {
        return new MenuColorStruct(new Color(
                PackedColorUtility.red(packedColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                PackedColorUtility.green(packedColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                PackedColorUtility.blue(packedColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                EngineSetting.COLOR_CHANNEL_MAX));
    }
}
