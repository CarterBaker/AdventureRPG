package editor.runtime.worldmap.menu;

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
import editor.runtime.input.EditorInputSystem;
import editor.runtime.EditorSetting;
import editor.runtime.worldmap.WorldMapSetting;
import editor.runtime.worldmap.view.WorldMapViewSystem;
import engine.root.SystemPackage;

public class WorldMapMenuSystem extends SystemPackage {

    /*
     * Opens the status bar over the world map. It names the block under the
     * pointer, or the view's centre while the pointer is elsewhere, with the
     * biome there, and the follow button shows whether the view is on the
     * character. The biome is sampled only when the named block changes, and every
     * label rewrites only on change.
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

        WorldHandle worldHandle = worldMapViewSystem.getWorldHandle();
        double worldX = worldMapViewSystem.getCenterX();
        double worldZ = worldMapViewSystem.getCenterZ();
        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        boolean pointed = editorInputSystem.isPointerActive()
                && worldMapViewSystem.locatePointer(rawInput.getMouseX(), rawInput.getMouseY());

        if (pointed) {
            worldX = worldMapViewSystem.getPointerX();
            worldZ = worldMapViewSystem.getPointerZ();
        }

        long blockX = (long) Math.floor(WorldWrapUtility.wrapBlockX(worldHandle, worldX));
        long blockZ = (long) Math.floor(WorldWrapUtility.wrapBlockZ(worldHandle, worldZ));

        if (blockX != sampledBlockX || blockZ != sampledBlockZ) {
            sampledBlockX = blockX;
            sampledBlockZ = blockZ;
            worldGenerationManager.sampleSurface(worldHandle, blockX, blockZ, sample);
            biomeName = biomeManager.getDisplayName(sample.getBlend().getDominantBiome());
        }

        String place = EditorSetting.WORLD_MAP_STATUS_X + blockX
                + EditorSetting.WORLD_MAP_STATUS_Z + blockZ
                + EditorSetting.WORLD_MAP_STATUS_SEPARATOR + biomeName
                + EditorSetting.WORLD_MAP_STATUS_SEPARATOR;

        if (!worldMapViewSystem.isShown())
            return place + EditorSetting.WORLD_MAP_STATUS_SCALE
                    + String.format(EditorSetting.WORLD_MAP_STATUS_SCALE_FORMAT, worldMapViewSystem.getBlocksPerPixel());

        if (!pointed)
            return place;

        return place + EditorSetting.WORLD_MAP_STATUS_LATITUDE
                + String.format(EditorSetting.WORLD_MAP_STATUS_DEGREES_FORMAT, worldMapViewSystem.getPointerLatitude())
                + EditorSetting.WORLD_MAP_STATUS_LONGITUDE
                + String.format(EditorSetting.WORLD_MAP_STATUS_DEGREES_FORMAT, worldMapViewSystem.getPointerLongitude());
    }
}
