package editor.commandconsole.itemgrid;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.commandconsole.CommandConsoleSetting;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandConsoleItemGridSystem extends SystemPackage {

    /*
     * Fills every item grid the command tree lays out with a tile for each
     * item, grouped by category and named in order, so a command that takes
     * an item — give — can be run by picking one. The rows hold as many tiles
     * as the grid is wide and reflow whenever its width changes, and the grid
     * scrolls when there are more rows than fit. Pressing a tile hands its
     * command and item to CommandManager's drag: let go on the same tile and
     * it runs on every Dev window, drop it on a Dev window and it runs there.
     */

    // Internal
    private MenuManager menuManager;
    private CommandManager commandManager;
    private ItemDefinitionManager itemDefinitionManager;

    // Grids
    private ObjectArrayList<CommandConsoleItemGridStruct> grids;
    private ObjectArrayList<ItemDefinitionHandle> sortedItems;

    // Base \\

    @Override
    protected void create() {

        // Grids
        this.grids = new ObjectArrayList<>();
        this.sortedItems = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.commandManager = get(CommandManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        for (int i = 0; i < grids.size(); i++) {

            CommandConsoleItemGridStruct grid = grids.get(i);
            int columns = resolveColumns(grid);

            if (columns != grid.getColumns())
                layoutGrid(grid, columns);
        }
    }

    // Management \\

    public void addGrid(ElementInstance gridElement, CommandHandle commandHandle) {

        CommandConsoleItemGridStruct grid = new CommandConsoleItemGridStruct(gridElement, commandHandle);

        grids.add(grid);
        layoutGrid(grid, CommandConsoleSetting.ITEM_GRID_MIN_COLUMNS);
    }

    public void clearGrids() {
        grids.clear();
    }

    // Layout \\

    private void layoutGrid(CommandConsoleItemGridStruct grid, int columns) {

        ElementInstance gridElement = grid.getGridElement();

        while (!gridElement.getChildren().isEmpty())
            menuManager.eject(gridElement, gridElement.getChildren().get(0));

        grid.clearTiles();
        grid.setColumns(columns);
        sortItems();

        ElementInstance row = null;

        for (int i = 0; i < sortedItems.size(); i++) {

            if (i % columns == 0)
                row = menuManager.inject(gridElement, CommandConsoleSetting.MENU_ITEM_ROW, null);

            injectTile(grid, row, sortedItems.get(i));
        }
    }

    private void injectTile(CommandConsoleItemGridStruct grid, ElementInstance row, ItemDefinitionHandle item) {

        ElementInstance tile = menuManager.inject(
                row, CommandConsoleSetting.MENU_ITEM_TILE,
                element -> element.findChildById(CommandConsoleSetting.ELEMENT_ITEM_TILE_LABEL)
                        .setFontText(item.getDisplayName()));

        grid.addTile(tile, tile.findChildById(CommandConsoleSetting.ELEMENT_ITEM_TILE_ICON), item);
    }

    // As many tiles as fit across the grid, measured from the tiles already laid out in it
    private int resolveColumns(CommandConsoleItemGridStruct grid) {

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

    private void sortItems() {

        sortedItems.clear();
        sortedItems.addAll(itemDefinitionManager.getItemHandles());
        sortedItems.sort((first, second) -> first.getCategory() != second.getCategory()
                ? first.getCategory().compareTo(second.getCategory())
                : first.getDisplayName().compareTo(second.getDisplayName()));
    }

    // Drag \\

    public void dragTile(ElementInstance tileElement, WindowInstance window) {

        for (int i = 0; i < grids.size(); i++) {

            CommandConsoleItemGridStruct grid = grids.get(i);
            int index = grid.indexOfTile(tileElement);

            if (index == EngineSetting.INDEX_NOT_FOUND)
                continue;

            ItemDefinitionHandle item = grid.getTileItem(index);

            commandManager.dragCommand(
                    grid.getCommandHandle().getCommandName() + EditorSetting.COMMAND_TOKEN_SEPARATOR
                            + item.getItemName(),
                    item.getDisplayName(),
                    window,
                    tileElement);
            return;
        }
    }

    // Accessible \\

    public ObjectArrayList<CommandConsoleItemGridStruct> getGrids() {
        return grids;
    }
}
