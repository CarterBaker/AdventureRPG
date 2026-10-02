package editor.commandconsole.vehiclegrid;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.commandconsole.CommandConsoleSetting;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandConsoleVehicleGridSystem extends SystemPackage {

    /*
     * Fills every vehicle grid the command tree lays out with a tile for each
     * vehicle type, named in order, so a command that takes a vehicle —
     * spawnvehicle — can be run by picking one, exactly as the item grid does
     * for items. The rows hold as many tiles as the grid is wide and reflow
     * whenever its width changes. Pressing a tile hands its command and
     * vehicle to CommandManager's drag: let go on the same tile and it runs on
     * every Dev window, drop it on a Dev window and it runs there.
     */

    // Internal
    private MenuManager menuManager;
    private CommandManager commandManager;
    private VehicleManager vehicleManager;

    // Grids
    private ObjectArrayList<CommandConsoleVehicleGridStruct> grids;
    private ObjectArrayList<VehicleHandle> sortedVehicles;

    // Base \\

    @Override
    protected void create() {

        // Grids
        this.grids = new ObjectArrayList<>();
        this.sortedVehicles = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.commandManager = get(CommandManager.class);
        this.vehicleManager = get(VehicleManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        for (int i = 0; i < grids.size(); i++) {

            CommandConsoleVehicleGridStruct grid = grids.get(i);
            int columns = resolveColumns(grid);

            if (columns != grid.getColumns())
                layoutGrid(grid, columns);
        }
    }

    // Management \\

    public void addGrid(ElementInstance gridElement, CommandHandle commandHandle) {

        CommandConsoleVehicleGridStruct grid = new CommandConsoleVehicleGridStruct(gridElement, commandHandle);

        grids.add(grid);
        layoutGrid(grid, CommandConsoleSetting.ITEM_GRID_MIN_COLUMNS);
    }

    public void clearGrids() {
        grids.clear();
    }

    // Layout \\

    private void layoutGrid(CommandConsoleVehicleGridStruct grid, int columns) {

        ElementInstance gridElement = grid.getGridElement();

        while (!gridElement.getChildren().isEmpty())
            menuManager.eject(gridElement, gridElement.getChildren().get(0));

        grid.clearTiles();
        grid.setColumns(columns);
        sortVehicles();

        ElementInstance row = null;

        for (int i = 0; i < sortedVehicles.size(); i++) {

            if (i % columns == 0)
                row = menuManager.inject(gridElement, CommandConsoleSetting.MENU_VEHICLE_ROW, null);

            injectTile(grid, row, sortedVehicles.get(i));
        }
    }

    private void injectTile(CommandConsoleVehicleGridStruct grid, ElementInstance row, VehicleHandle vehicle) {

        ElementInstance tile = menuManager.inject(
                row, CommandConsoleSetting.MENU_VEHICLE_TILE,
                element -> element.findChildById(CommandConsoleSetting.ELEMENT_VEHICLE_TILE_LABEL)
                        .setFontText(vehicle.getDisplayName()));

        grid.addTile(tile, vehicle);
    }

    // As many tiles as fit across the grid, measured from the tiles already laid out in it
    private int resolveColumns(CommandConsoleVehicleGridStruct grid) {

        ElementInstance gridElement = grid.getGridElement();
        float gridW = gridElement.getComputedW();

        if (gridW <= 0f || grid.getTileCount() == 0)
            return grid.getColumns();

        ElementInstance row = gridElement.getChildren().get(0);
        DimensionValueStruct spacing = row.getElementData().getSpacing();
        float spacingW = spacing != null ? spacing.resolve(gridW) : 0f;
        float tileW = grid.getTileElement(0).getElementData().getLayout().resolveWidth(gridW, row.getComputedH());

        if (tileW <= 0f)
            return grid.getColumns();

        return Math.max(CommandConsoleSetting.ITEM_GRID_MIN_COLUMNS, (int) ((gridW + spacingW) / (tileW + spacingW)));
    }

    private void sortVehicles() {

        sortedVehicles.clear();
        sortedVehicles.addAll(vehicleManager.getVehicleHandles());
        sortedVehicles.sort((first, second) -> first.getDisplayName().compareTo(second.getDisplayName()));
    }

    // Drag \\

    public void dragTile(ElementInstance tileElement, WindowInstance window) {

        for (int i = 0; i < grids.size(); i++) {

            CommandConsoleVehicleGridStruct grid = grids.get(i);
            int index = grid.indexOfTile(tileElement);

            if (index == EngineSetting.INDEX_NOT_FOUND)
                continue;

            VehicleHandle vehicle = grid.getTileVehicle(index);

            commandManager.dragCommand(
                    grid.getCommandHandle().getCommandName() + EditorSetting.COMMAND_TOKEN_SEPARATOR
                            + vehicle.getVehicleName(),
                    vehicle.getDisplayName(),
                    window,
                    tileElement);
            return;
        }
    }
}
