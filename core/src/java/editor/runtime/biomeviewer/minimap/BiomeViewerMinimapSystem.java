package editor.runtime.biomeviewer.minimap;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import editor.runtime.EditorSetting;
import editor.runtime.biomeviewer.BiomeViewerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2Int;

public class BiomeViewerMinimapSystem extends SystemPackage {

    /*
     * Draws the minimap in the viewer's corner: a wireframe net of the
     * planner's grid, its lines laid once for however many cells a side the
     * grid has, and a dot where the camera stands over the planner's world,
     * wrapped as the world wraps and laid out as the planner lays its cells,
     * row by row from the top left. The dot's column and row are each picked
     * from positions made once, and moved only when the step it stands on
     * changes, so following the camera makes no garbage.
     */

    // Internal
    private MenuManager menuManager;
    private PlayerManager playerManager;

    // Menus
    private MenuInstance minimapMenu;
    private ElementInstance carrier;
    private ElementInstance dot;

    // Positions — the dot's column across the minimap and its row down it, by step
    private DimensionVector2Struct[] columnPositions;
    private DimensionVector2Struct[] rowPositions;
    private int shownColumnStep;
    private int shownRowStep;

    // Base \\

    @Override
    protected void create() {

        // Positions
        this.columnPositions = new DimensionVector2Struct[BiomeViewerSetting.MINIMAP_STEPS + 1];
        this.rowPositions = new DimensionVector2Struct[BiomeViewerSetting.MINIMAP_STEPS + 1];

        for (int step = 0; step <= BiomeViewerSetting.MINIMAP_STEPS; step++) {
            columnPositions[step] = toColumnPosition(step, BiomeViewerSetting.MINIMAP_STEPS);
            rowPositions[step] = toRowPosition(step, BiomeViewerSetting.MINIMAP_STEPS);
        }

        this.shownColumnStep = EngineSetting.INDEX_NOT_FOUND;
        this.shownRowStep = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.playerManager = get(PlayerManager.class);
    }

    @Override
    protected void awake() {

        this.minimapMenu = menuManager.openMenu(BiomeViewerSetting.MENU_MINIMAP, context.getWindow());
        this.carrier = minimapMenu.getEntryPoint(BiomeViewerSetting.ENTRY_MINIMAP_CARRIER);
        this.dot = carrier != null ? carrier.findChildById(BiomeViewerSetting.ELEMENT_MINIMAP_DOT) : null;

        layNet();
    }

    // Net \\

    // The lines between the grid's cells, the minimap's outline framing its edges
    private void layNet() {

        int side = EditorSetting.BIOME_PLANNER_GRID_SIDE;

        for (int line = 1; line < side; line++) {

            DimensionVector2Struct column = toColumnPosition(line, side);
            DimensionVector2Struct row = toRowPosition(line, side);

            menuManager.inject(
                    minimapMenu, BiomeViewerSetting.ENTRY_MINIMAP_NET, BiomeViewerSetting.TEMPLATE_MINIMAP_COLUMN,
                    element -> element.setPositionOverride(column));
            menuManager.inject(
                    minimapMenu, BiomeViewerSetting.ENTRY_MINIMAP_NET, BiomeViewerSetting.TEMPLATE_MINIMAP_ROW,
                    element -> element.setPositionOverride(row));
        }
    }

    // Update \\

    @Override
    protected void update() {

        int windowID = context.getWindow().getWindowID();

        if (dot == null || !playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        WorldPositionStruct worldPosition = player.getWorldPositionStruct();
        Vector2Int worldScale = player.getWorldHandle().getWorldScale();
        long chunkCoordinate = worldPosition.getChunkCoordinate();

        int columnStep = toStep(
                Coordinate2Long.unpackX(chunkCoordinate), worldPosition.getPosition().x, worldScale.x);
        int rowStep = toStep(
                Coordinate2Long.unpackY(chunkCoordinate), worldPosition.getPosition().z, worldScale.y);

        if (columnStep != shownColumnStep) {
            shownColumnStep = columnStep;
            carrier.setPositionOverride(columnPositions[columnStep]);
        }

        if (rowStep != shownRowStep) {
            shownRowStep = rowStep;
            dot.setPositionOverride(rowPositions[rowStep]);
        }
    }

    // Utility \\

    // The step across the minimap a block position stands on, wrapped into the world
    private int toStep(int chunk, float local, int worldBlocks) {

        double block = (double) chunk * EngineSetting.CHUNK_SIZE + local;
        double fraction = Math.floorMod((long) Math.floor(block), worldBlocks) / (double) worldBlocks;

        return (int) Math.round(fraction * BiomeViewerSetting.MINIMAP_STEPS);
    }

    private DimensionVector2Struct toColumnPosition(int step, int steps) {
        return new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(BiomeViewerSetting.MINIMAP_PERCENT * step / steps),
                DimensionValueStruct.ofAbsolute(0f));
    }

    // Rows run down from the top, against the layout's upward axis
    private DimensionVector2Struct toRowPosition(int step, int steps) {
        return new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(0f),
                DimensionValueStruct.ofPercent(-BiomeViewerSetting.MINIMAP_PERCENT * step / steps));
    }
}
