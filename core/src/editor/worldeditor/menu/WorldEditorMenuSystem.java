package editor.worldeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.worldeditorpipeline.worldbiome.WorldBiomeEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import editor.worldeditor.WorldEditorSetting;
import editor.worldeditor.render.WorldEditorRenderSystem;
import editor.worldeditor.tool.WorldEditorToolSystem;
import engine.graphics.color.Color;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldEditorMenuSystem extends SystemPackage {

    /*
     * Opens the World Editor toolbar and biome palette over the viewport. The
     * status line names the image, tool, brush and biome, with the pixel and
     * biome under the pointer, and rewrites only on change, as does the
     * player marker button's label. When the palette
     * moves it lists every biome the image can paint, one row per biome with
     * its color swatch and name, the selected one highlighted and every row
     * carrying its biome's name.
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
    private ObjectArrayList<ElementInstance> biomeElements;
    private int shownPaletteRevision;

    // Status
    private String shownStatus;
    private String shownPlayers;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.biomeElements = new ObjectArrayList<>();
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

    @Override
    protected void dispose() {

        menuManager.closeMenu(toolbarMenu);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }

    // Update \\

    @Override
    protected void update() {

        refreshStatus();
        refreshPlayers();

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

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(WorldEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(status);
    }

    private void refreshPlayers() {

        String players = worldEditorRenderSystem.isShowingPlayers()
                ? EditorSetting.WORLD_EDITOR_PLAYERS_ON
                : EditorSetting.WORLD_EDITOR_PLAYERS_OFF;

        if (players.equals(shownPlayers))
            return;

        shownPlayers = players;

        ElementInstance playersLabel = toolbarMenu.getEntryPoint(WorldEditorSetting.ENTRY_PLAYERS_LABEL);

        if (playersLabel != null)
            playersLabel.setFontText(players);
    }

    // Palette \\

    private void refreshPalette() {

        for (int i = 0; i < biomeElements.size(); i++)
            menuManager.eject(toolbarMenu, WorldEditorSetting.ENTRY_BIOMES, biomeElements.get(i));

        biomeElements.clear();

        ObjectArrayList<WorldBiomeEntryStruct> palette = worldEditorManager.getPalette();

        for (int i = 0; i < palette.size(); i++)
            biomeElements.add(injectBiome(palette.get(i)));
    }

    private ElementInstance injectBiome(WorldBiomeEntryStruct entry) {

        String template = entry.getBiomeName().equals(worldEditorManager.getSelectedBiomeName())
                ? WorldEditorSetting.TEMPLATE_BIOME_ROW_SELECTED
                : WorldEditorSetting.TEMPLATE_BIOME_ROW;
        int mapColor = entry.getMapColor();
        MenuColorStruct swatchColor = new MenuColorStruct(new Color(
                PackedColorUtility.red(mapColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                PackedColorUtility.green(mapColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                PackedColorUtility.blue(mapColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                EngineSetting.COLOR_CHANNEL_MAX));

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
}
