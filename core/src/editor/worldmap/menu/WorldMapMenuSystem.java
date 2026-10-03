package editor.worldmap.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.EditorInputSystem;
import editor.runtime.EditorSetting;
import editor.worldmap.WorldMapSetting;
import editor.worldmap.view.WorldMapViewSystem;
import engine.root.SystemPackage;

public class WorldMapMenuSystem extends SystemPackage {

    /*
     * Opens the status bar over the world map. It names the block under the
     * pointer, or the view's centre while the pointer is elsewhere, with the
     * biome there, and the follow button shows whether the view is on the
     * character. The biome is sampled only when the named block changes, and
     * both labels rewrite only on change.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldGenerationManager worldGenerationManager;
    private BiomeManager biomeManager;
    private EditorInputSystem editorInputSystem;
    private WorldMapViewSystem worldMapViewSystem;

    // Menus
    private MenuInstance statusMenu;

    // Status
    private TerrainSurfaceSampleStruct sample;
    private long sampledBlockX;
    private long sampledBlockZ;
    private String biomeName;
    private String shownStatus;
    private String shownFollow;

    // Base \\

    @Override
    protected void create() {
        this.sample = new TerrainSurfaceSampleStruct();
        this.sampledBlockX = Long.MIN_VALUE;
        this.sampledBlockZ = Long.MIN_VALUE;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.editorInputSystem = get(EditorInputSystem.class);
        this.worldMapViewSystem = get(WorldMapViewSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.statusMenu = menuManager.openMenu(WorldMapSetting.MENU_STATUS, window);
    }

    @Override
    protected void dispose() {

        menuManager.closeMenu(statusMenu);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }

    // Update \\

    @Override
    protected void update() {

        String status = resolveStatus();

        if (!status.equals(shownStatus)) {
            shownStatus = status;
            setEntryText(WorldMapSetting.ENTRY_STATUS, status);
        }

        String follow = worldMapViewSystem.isFollowing()
                ? EditorSetting.WORLD_MAP_FOLLOW_ON
                : EditorSetting.WORLD_MAP_FOLLOW_OFF;

        if (!follow.equals(shownFollow)) {
            shownFollow = follow;
            setEntryText(WorldMapSetting.ENTRY_FOLLOW_LABEL, follow);
        }
    }

    private void setEntryText(int entry, String text) {

        ElementInstance label = statusMenu.getEntryPoint(entry);

        if (label != null)
            label.setFontText(text);
    }

    // Status \\

    private String resolveStatus() {

        if (!worldMapViewSystem.hasWorld())
            return EditorSetting.WORLD_MAP_STATUS_NO_WORLD;

        WorldHandle worldHandle = worldMapViewSystem.getWorldHandle();
        double worldX = worldMapViewSystem.getCenterX();
        double worldZ = worldMapViewSystem.getCenterZ();

        if (editorInputSystem.isPointerActive()) {
            RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
            worldX = worldMapViewSystem.screenToWorldX(rawInput.getMouseX());
            worldZ = worldMapViewSystem.screenToWorldZ(rawInput.getMouseY());
        }

        long blockX = (long) Math.floor(WorldWrapUtility.wrapBlockX(worldHandle, worldX));
        long blockZ = (long) Math.floor(WorldWrapUtility.wrapBlockZ(worldHandle, worldZ));

        if (blockX != sampledBlockX || blockZ != sampledBlockZ) {
            sampledBlockX = blockX;
            sampledBlockZ = blockZ;
            worldGenerationManager.sampleSurface(worldHandle, blockX, blockZ, sample);
            biomeName = biomeManager.getDisplayName(sample.getBlend().getDominantBiome());
        }

        return EditorSetting.WORLD_MAP_STATUS_X + blockX
                + EditorSetting.WORLD_MAP_STATUS_Z + blockZ
                + EditorSetting.WORLD_MAP_STATUS_SEPARATOR + biomeName
                + EditorSetting.WORLD_MAP_STATUS_SEPARATOR + EditorSetting.WORLD_MAP_STATUS_SCALE
                + String.format(EditorSetting.WORLD_MAP_STATUS_SCALE_FORMAT, worldMapViewSystem.getBlocksPerPixel());
    }
}
