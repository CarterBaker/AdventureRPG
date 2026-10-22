package editor.runtime.worldeditor.menu;

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
import editor.runtime.EditorSetting;
import editor.runtime.worldeditor.WorldEditorSetting;
import editor.runtime.worldeditor.render.WorldEditorRenderSystem;
import editor.runtime.worldeditor.tool.WorldEditorToolSystem;
import engine.graphics.color.Color;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldEditorMenuSystem extends SystemPackage {

    /*
     * Opens the World Editor toolbar and palette over the viewport. The status
     * line names the image, layer, tool, brush and what it paints, with the
     * pixel, biome and height under the pointer, and rewrites only on change,
     * as do the player marker and layer buttons' labels. When the palette
     * moves it lists what the active layer can paint under that layer's
     * title: every biome, one row per biome with its color swatch and name,
     * or every height level, one row per level with its grey swatch and its
     * name, the selected one highlighted and every
     * row carrying its biome's name or its level.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldEditorManager worldEditorManager;
    private WorldEditorToolSystem worldEditorToolSystem;
    private WorldEditorRenderSystem worldEditorRenderSystem;

    // Menus
    private MenuInstance toolbarMenu;

    // Palette
    private ObjectArrayList<ElementInstance> paletteElements;
    private int shownPaletteRevision;

    // Status
    private String shownStatus;
    private String shownPlayers;
    private WorldEditorLayer shownLayer;

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
        this.worldEditorToolSystem = get(WorldEditorToolSystem.class);
        this.worldEditorRenderSystem = get(WorldEditorRenderSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(WorldEditorSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        refreshStatus();
        refreshPlayers();
        refreshLayer();

        if (worldEditorManager.getPaletteRevision() == shownPaletteRevision)
            return;

        shownPaletteRevision = worldEditorManager.getPaletteRevision();
        refreshPalette();
    }

    // Status \\

    private void refreshStatus() {

        String status = worldEditorToolSystem.isHovering()
                ? worldEditorManager.getStatusText() + EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR
                        + worldEditorManager.describePixel(
                                worldEditorToolSystem.getHoveredX(), worldEditorToolSystem.getHoveredY())
                : worldEditorManager.getStatusText();

        if (status.equals(shownStatus))
            return;

        shownStatus = status;
        setEntryText(WorldEditorSetting.ENTRY_STATUS, status);
    }

    private void refreshPlayers() {

        String players = worldEditorRenderSystem.isShowingPlayers()
                ? EditorSetting.WORLD_EDITOR_PLAYERS_ON
                : EditorSetting.WORLD_EDITOR_PLAYERS_OFF;

        if (players.equals(shownPlayers))
            return;

        shownPlayers = players;
        setEntryText(WorldEditorSetting.ENTRY_PLAYERS_LABEL, players);
    }

    private void refreshLayer() {

        WorldEditorLayer layer = worldEditorManager.getLayer();

        if (layer == shownLayer)
            return;

        shownLayer = layer;
        setEntryText(WorldEditorSetting.ENTRY_LAYER_LABEL, layer.getLabel());
        setEntryText(WorldEditorSetting.ENTRY_PALETTE_TITLE, layer.getLabel());
    }

    private void setEntryText(int entry, String text) {

        ElementInstance label = toolbarMenu.getEntryPoint(entry);

        if (label != null)
            label.setFontText(text);
    }

    // Palette \\

    private void refreshPalette() {

        for (int i = 0; i < paletteElements.size(); i++)
            menuManager.eject(toolbarMenu, WorldEditorSetting.ENTRY_BIOMES, paletteElements.get(i));

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
                ? WorldEditorSetting.TEMPLATE_BIOME_ROW_SELECTED
                : WorldEditorSetting.TEMPLATE_BIOME_ROW;
        MenuColorStruct swatchColor = toSwatchColor(entry.getMapColor());

        return menuManager.inject(
                toolbarMenu, WorldEditorSetting.ENTRY_BIOMES, template,
                element -> {
                    element.setActionArgOverride(entry.getBiomeName());

                    ElementInstance swatch = element.findChildById(WorldEditorSetting.ELEMENT_BIOME_SWATCH);
                    ElementInstance label = element.findChildById(WorldEditorSetting.ELEMENT_BIOME_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(swatchColor);

                    if (label != null)
                        label.setFontText(entry.getDisplayName());
                });
    }

    private ElementInstance injectHeight(WorldHeightEntryStruct entry) {

        String template = entry.getLevel() == worldEditorManager.getSelectedHeightLevel()
                ? WorldEditorSetting.TEMPLATE_HEIGHT_ROW_SELECTED
                : WorldEditorSetting.TEMPLATE_HEIGHT_ROW;
        int level = entry.getLevel();
        MenuColorStruct swatchColor = toSwatchColor(PackedColorUtility.pack(level, level, level));

        return menuManager.inject(
                toolbarMenu, WorldEditorSetting.ENTRY_BIOMES, template,
                element -> {
                    element.setActionArgOverride(Integer.toString(entry.getLevel()));

                    ElementInstance swatch = element.findChildById(WorldEditorSetting.ELEMENT_HEIGHT_SWATCH);
                    ElementInstance label = element.findChildById(WorldEditorSetting.ELEMENT_HEIGHT_LABEL);

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
