package editor.commandconsole.commandtree;

import application.bootstrap.itempipeline.itemdefinition.ItemCategory;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCategory;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.commandconsole.CommandConsoleSetting;
import editor.commandconsole.panel.CommandConsolePanelSystem;
import editor.commandconsole.tilegrid.CommandConsoleTileGridSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class CommandConsoleTreeSystem extends SystemPackage {

    /*
     * Lays out the command console's tree, one group per command file. A group
     * lists every command that takes no arguments first, each run with a
     * single click, then every command that takes an item or a vehicle under
     * its own header, with a section per item or vehicle category that holds
     * any, each a grid of tiles filled by the tile grid system. A group with
     * none of these is left out. Groups and categories start expanded and
     * collapse on click; the tree is laid out on the first frame and again
     * only when one of them is toggled.
     */

    // Internal
    private MenuManager menuManager;
    private CommandManager commandManager;
    private CommandConsolePanelSystem commandConsolePanelSystem;
    private CommandConsoleTileGridSystem commandConsoleTileGridSystem;

    // Tree
    private ObjectArrayList<ElementInstance> treeElements;
    private ObjectOpenHashSet<String> collapsedNodeKeys;
    private boolean layoutPending;

    // Base \\

    @Override
    protected void create() {

        // Tree
        this.treeElements = new ObjectArrayList<>();
        this.collapsedNodeKeys = new ObjectOpenHashSet<>();
        this.layoutPending = true;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.commandManager = get(CommandManager.class);
        this.commandConsolePanelSystem = get(CommandConsolePanelSystem.class);
        this.commandConsoleTileGridSystem = get(CommandConsoleTileGridSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        if (!layoutPending)
            return;

        layoutTree();
        this.layoutPending = false;
    }

    // Layout \\

    private void layoutTree() {

        MenuInstance commandConsoleMenu = commandConsolePanelSystem.getCommandConsoleMenu();
        ObjectArrayList<String> groupNames = commandManager.getGroupNames();

        commandConsoleTileGridSystem.clearGrids();

        for (int i = 0; i < treeElements.size(); i++)
            menuManager.eject(commandConsoleMenu, CommandConsoleSetting.ENTRY_COMMAND_TREE, treeElements.get(i));

        treeElements.clear();

        for (int i = 0; i < groupNames.size(); i++)
            injectGroup(commandConsoleMenu, groupNames.get(i));
    }

    private void injectGroup(MenuInstance commandConsoleMenu, String groupName) {

        ObjectArrayList<CommandHandle> commandHandles = commandManager.getCommandHandles(groupName);

        if (!hasListedCommand(commandHandles))
            return;

        boolean expanded = !collapsedNodeKeys.contains(groupName);

        treeElements.add(menuManager.inject(
                commandConsoleMenu,
                CommandConsoleSetting.ENTRY_COMMAND_TREE,
                CommandConsoleSetting.MENU_COMMAND_GROUP,
                element -> {
                    element.setActionArgOverride(groupName);
                    setChildText(element, CommandConsoleSetting.ELEMENT_GROUP_MARKER, toMarker(expanded));
                    setChildText(element, CommandConsoleSetting.ELEMENT_GROUP_LABEL, groupName);
                }));

        if (!expanded)
            return;

        for (int i = 0; i < commandHandles.size(); i++)
            if (commandHandles.get(i).isArgumentFree())
                injectCommand(commandConsoleMenu, commandHandles.get(i));

        for (int i = 0; i < commandHandles.size(); i++) {

            CommandHandle commandHandle = commandHandles.get(i);

            if (commandHandle.takesItem())
                injectItemCommand(commandConsoleMenu, commandHandle);
            else if (commandHandle.takesVehicle())
                injectVehicleCommand(commandConsoleMenu, commandHandle);
        }
    }

    private void injectCommand(MenuInstance commandConsoleMenu, CommandHandle commandHandle) {
        treeElements.add(menuManager.inject(
                commandConsoleMenu,
                CommandConsoleSetting.ENTRY_COMMAND_TREE,
                CommandConsoleSetting.MENU_COMMAND_ENTRY,
                element -> {
                    element.setActionArgOverride(commandHandle.getCommandName());
                    setChildText(element, CommandConsoleSetting.ELEMENT_COMMAND_LABEL, commandHandle.getLabel());
                }));
    }

    private void injectItemCommand(MenuInstance commandConsoleMenu, CommandHandle commandHandle) {

        injectTileHeader(commandConsoleMenu, commandHandle);

        for (ItemCategory itemCategory : ItemCategory.VALUES) {

            ObjectArrayList<ItemDefinitionHandle> items = commandConsoleTileGridSystem.collectItems(itemCategory);

            if (items.isEmpty())
                continue;

            ElementInstance grid = injectTileCategory(commandConsoleMenu, commandHandle, itemCategory.getTitle());

            if (grid != null)
                commandConsoleTileGridSystem.addItemGrid(grid, commandHandle, items);
        }
    }

    private void injectVehicleCommand(MenuInstance commandConsoleMenu, CommandHandle commandHandle) {

        injectTileHeader(commandConsoleMenu, commandHandle);

        for (VehicleCategory vehicleCategory : VehicleCategory.VALUES) {

            ObjectArrayList<VehicleHandle> vehicles = commandConsoleTileGridSystem.collectVehicles(vehicleCategory);

            if (vehicles.isEmpty())
                continue;

            ElementInstance grid = injectTileCategory(commandConsoleMenu, commandHandle, vehicleCategory.getTitle());

            if (grid != null)
                commandConsoleTileGridSystem.addVehicleGrid(grid, commandHandle, vehicles);
        }
    }

    private void injectTileHeader(MenuInstance commandConsoleMenu, CommandHandle commandHandle) {
        treeElements.add(menuManager.inject(
                commandConsoleMenu,
                CommandConsoleSetting.ENTRY_COMMAND_TREE,
                CommandConsoleSetting.MENU_TILE_HEADER,
                element -> setChildText(element, CommandConsoleSetting.ELEMENT_TILE_HEADER_LABEL,
                        commandHandle.getLabel())));
    }

    // A category's header under its command, and the grid its tiles go in — null while it is collapsed
    private ElementInstance injectTileCategory(
            MenuInstance commandConsoleMenu,
            CommandHandle commandHandle,
            String categoryTitle) {

        String categoryKey = commandHandle.getCommandName() + CommandConsoleSetting.CATEGORY_KEY_SEPARATOR
                + categoryTitle;
        boolean expanded = !collapsedNodeKeys.contains(categoryKey);

        treeElements.add(menuManager.inject(
                commandConsoleMenu,
                CommandConsoleSetting.ENTRY_COMMAND_TREE,
                CommandConsoleSetting.MENU_TILE_CATEGORY,
                element -> {
                    element.setActionArgOverride(categoryKey);
                    setChildText(element, CommandConsoleSetting.ELEMENT_TILE_CATEGORY_MARKER, toMarker(expanded));
                    setChildText(element, CommandConsoleSetting.ELEMENT_TILE_CATEGORY_LABEL, categoryTitle);
                }));

        if (!expanded)
            return null;

        ElementInstance grid = menuManager.inject(
                commandConsoleMenu,
                CommandConsoleSetting.ENTRY_COMMAND_TREE,
                CommandConsoleSetting.MENU_TILE_GRID);

        treeElements.add(grid);

        return grid;
    }

    // Management \\

    // Collapses an expanded group or category, or expands a collapsed one
    public void toggleTreeNode(String nodeKey) {

        if (!collapsedNodeKeys.remove(nodeKey))
            collapsedNodeKeys.add(nodeKey);

        this.layoutPending = true;
    }

    // Utility \\

    private boolean hasListedCommand(ObjectArrayList<CommandHandle> commandHandles) {

        for (int i = 0; i < commandHandles.size(); i++)
            if (commandHandles.get(i).isArgumentFree() || commandHandles.get(i).takesItem()
                    || commandHandles.get(i).takesVehicle())
                return true;

        return false;
    }

    private String toMarker(boolean expanded) {
        return expanded ? EngineSetting.HIERARCHY_EXPANDED_MARKER : EngineSetting.HIERARCHY_COLLAPSED_MARKER;
    }

    private void setChildText(ElementInstance element, String childId, String text) {

        ElementInstance child = element.findChildById(childId);

        if (child != null)
            child.setFontText(text);
    }
}
