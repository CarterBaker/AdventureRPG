package editor.commandconsole.tilegrid;

import application.bootstrap.itempipeline.itemdefinition.ItemCategory;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCategory;
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

public class CommandConsoleTileGridSystem extends SystemPackage {

    /*
     * Fills every tile grid the command tree lays out, one per category, with
     * a tile for each item or vehicle type in it, named in order, so a command
     * that takes an item — give — or a vehicle — spawnvehicle — is run by
     * picking one. Items and vehicles lay out identically: a row holds as many
     * tiles as the grid is wide and reflows whenever its width changes, and a
     * grid is exactly as tall as its rows, so the whole tree scrolls as one.
     * Pressing a tile hands its command and entry to CommandManager's drag:
     * let go on the same tile and it runs on every Dev window, drop it on a
     * Dev window and it runs there.
     */

    // Internal
    private MenuManager menuManager;
    private CommandManager commandManager;
    private ItemDefinitionManager itemDefinitionManager;
    private VehicleManager vehicleManager;

    // Grids
    private ObjectArrayList<CommandConsoleTileGridStruct<ItemDefinitionHandle>> itemGrids;
    private ObjectArrayList<CommandConsoleTileGridStruct<VehicleHandle>> vehicleGrids;
    private int resolvedColumns;

    // Scratch
    private ObjectArrayList<ItemDefinitionHandle> categoryItems;
    private ObjectArrayList<VehicleHandle> categoryVehicles;

    // Base \\

    @Override
    protected void create() {

        // Grids
        this.itemGrids = new ObjectArrayList<>();
        this.vehicleGrids = new ObjectArrayList<>();
        this.resolvedColumns = CommandConsoleSetting.TILE_GRID_MIN_COLUMNS;

        // Scratch
        this.categoryItems = new ObjectArrayList<>();
        this.categoryVehicles = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.commandManager = get(CommandManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.vehicleManager = get(VehicleManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        for (int i = 0; i < itemGrids.size(); i++)
            refreshGrid(itemGrids.get(i));

        for (int i = 0; i < vehicleGrids.size(); i++)
            refreshGrid(vehicleGrids.get(i));
    }

    private void refreshGrid(CommandConsoleTileGridStruct<?> grid) {

        int columns = resolveColumns(grid);

        if (columns == grid.getColumns())
            return;

        layoutGrid(grid, columns);
        this.resolvedColumns = columns;
    }

    // Management \\

    // Every item in a category, named in order — empty when the category holds none
    public ObjectArrayList<ItemDefinitionHandle> collectItems(ItemCategory itemCategory) {

        ObjectArrayList<ItemDefinitionHandle> items = itemDefinitionManager.getItemHandles();

        categoryItems.clear();

        for (int i = 0; i < items.size(); i++)
            if (items.get(i).getCategory() == itemCategory)
                categoryItems.add(items.get(i));

        categoryItems.sort((first, second) -> first.getDisplayName().compareTo(second.getDisplayName()));

        return categoryItems;
    }

    // Every vehicle type in a category, named in order — empty when the category holds none
    public ObjectArrayList<VehicleHandle> collectVehicles(VehicleCategory vehicleCategory) {

        ObjectArrayList<VehicleHandle> vehicles = vehicleManager.getVehicleHandles();

        categoryVehicles.clear();

        for (int i = 0; i < vehicles.size(); i++)
            if (vehicles.get(i).getCategory() == vehicleCategory)
                categoryVehicles.add(vehicles.get(i));

        categoryVehicles.sort((first, second) -> first.getDisplayName().compareTo(second.getDisplayName()));

        return categoryVehicles;
    }

    public void addItemGrid(
            ElementInstance gridElement,
            CommandHandle commandHandle,
            ObjectArrayList<ItemDefinitionHandle> items) {

        CommandConsoleTileGridStruct<ItemDefinitionHandle> grid = new CommandConsoleTileGridStruct<>(
                gridElement, commandHandle);

        for (int i = 0; i < items.size(); i++)
            grid.addEntry(items.get(i), items.get(i).getItemName(), items.get(i).getDisplayName());

        itemGrids.add(grid);
        layoutGrid(grid, resolvedColumns);
    }

    public void addVehicleGrid(
            ElementInstance gridElement,
            CommandHandle commandHandle,
            ObjectArrayList<VehicleHandle> vehicles) {

        CommandConsoleTileGridStruct<VehicleHandle> grid = new CommandConsoleTileGridStruct<>(
                gridElement, commandHandle);

        for (int i = 0; i < vehicles.size(); i++)
            grid.addEntry(vehicles.get(i), vehicles.get(i).getVehicleName(), vehicles.get(i).getDisplayName());

        vehicleGrids.add(grid);
        layoutGrid(grid, resolvedColumns);
    }

    public void clearGrids() {
        itemGrids.clear();
        vehicleGrids.clear();
    }

    // Layout \\

    private void layoutGrid(CommandConsoleTileGridStruct<?> grid, int columns) {

        ElementInstance gridElement = grid.getGridElement();

        while (!gridElement.getChildren().isEmpty())
            menuManager.eject(gridElement, gridElement.getChildren().get(0));

        grid.clearTiles();
        grid.setColumns(columns);

        ElementInstance row = null;

        for (int i = 0; i < grid.getEntryCount(); i++) {

            if (i % columns == 0)
                row = menuManager.inject(gridElement, CommandConsoleSetting.MENU_TILE_ROW, null);

            injectTile(grid, row, i);
        }

        fitGrid(grid);
    }

    private void injectTile(CommandConsoleTileGridStruct<?> grid, ElementInstance row, int index) {

        ElementInstance tile = menuManager.inject(
                row, CommandConsoleSetting.MENU_TILE,
                element -> element.findChildById(CommandConsoleSetting.ELEMENT_TILE_LABEL)
                        .setFontText(grid.getEntryLabel(index)));

        grid.addTile(tile, tile.findChildById(CommandConsoleSetting.ELEMENT_TILE_ICON));
    }

    // Sizes the grid to exactly the height of its rows and the spacing between them
    private void fitGrid(CommandConsoleTileGridStruct<?> grid) {

        ElementInstance gridElement = grid.getGridElement();
        ObjectArrayList<ElementInstance> rows = gridElement.getChildren();
        float gridW = gridElement.getComputedW();
        float gridH = gridElement.getComputedH();
        DimensionValueStruct spacing = gridElement.getElementData().getSpacing();
        float spacingH = spacing != null ? spacing.resolve(gridH) : 0f;
        float height = 0f;

        for (int i = 0; i < rows.size(); i++)
            height += rows.get(i).getElementData().getLayout().resolveHeight(gridW, gridH) + (i > 0 ? spacingH : 0f);

        gridElement.setSizeOverride(new DimensionVector2Struct(
                gridElement.getElementData().getLayout().getSize().getX(),
                DimensionValueStruct.ofAbsolute(height)));
    }

    // As many tiles as fit across the grid, measured from the tiles already laid out in it
    private int resolveColumns(CommandConsoleTileGridStruct<?> grid) {

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

        return Math.max(CommandConsoleSetting.TILE_GRID_MIN_COLUMNS, (int) ((gridW + spacingW) / (tileW + spacingW)));
    }

    // Drag \\

    public void dragTile(ElementInstance tileElement, WindowInstance window) {
        if (!dragTile(itemGrids, tileElement, window))
            dragTile(vehicleGrids, tileElement, window);
    }

    private <T> boolean dragTile(
            ObjectArrayList<CommandConsoleTileGridStruct<T>> grids,
            ElementInstance tileElement,
            WindowInstance window) {

        for (int i = 0; i < grids.size(); i++) {

            CommandConsoleTileGridStruct<T> grid = grids.get(i);
            int index = grid.indexOfTile(tileElement);

            if (index == EngineSetting.INDEX_NOT_FOUND)
                continue;

            commandManager.dragCommand(
                    grid.getCommandHandle().getCommandName() + EditorSetting.COMMAND_TOKEN_SEPARATOR
                            + grid.getEntryArgument(index),
                    grid.getEntryLabel(index),
                    window,
                    tileElement);
            return true;
        }

        return false;
    }

    // Accessible \\

    public ObjectArrayList<CommandConsoleTileGridStruct<ItemDefinitionHandle>> getItemGrids() {
        return itemGrids;
    }

    public ObjectArrayList<CommandConsoleTileGridStruct<VehicleHandle>> getVehicleGrids() {
        return vehicleGrids;
    }
}
