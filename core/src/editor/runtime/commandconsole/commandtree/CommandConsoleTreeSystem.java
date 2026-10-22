package editor.runtime.commandconsole.commandtree;

import application.bootstrap.itempipeline.itemdefinition.ItemCategory;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCategory;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.runtime.commandconsole.CommandConsoleSetting;
import editor.runtime.commandconsole.chunkfield.CommandConsoleChunkFieldSystem;
import editor.runtime.commandconsole.dial.CommandConsoleDialSystem;
import editor.runtime.commandconsole.panel.CommandConsolePanelSystem;
import editor.runtime.commandconsole.tilegrid.CommandConsoleTileGridSystem;
import editor.runtime.dev.time.DevTimeUnit;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class CommandConsoleTreeSystem extends SystemPackage {

    /*
     * Lays out the command console's tree, one group per command file. A group
     * lists every command that takes no arguments first, each run with a
     * single click, then a section per item or vehicle category that holds
     * any, each a grid of tiles filled by the tile grid system, for every
     * command that takes one, a row of X and Y fields, filled by the chunk
     * field system, for every command that takes a chunk, and a section of
     * dials per group of the clock's units, filled by the dial system, for
     * every command that takes a time unit and its value. An item or time
     * command heads its sections with its own name; a vehicle's tile is the
     * command itself, so its sections sit straight under the group. A group
     * with none of these is left out. A unit the world's calendar does not
     * have gets no dial.
     * Groups and categories start expanded and collapse on click; the tree is
     * laid out on the first frame and again only when one of them is toggled.
     */

    // Internal
    private MenuManager menuManager;
    private CommandManager commandManager;
    private CommandConsolePanelSystem commandConsolePanelSystem;
    private CommandConsoleTileGridSystem commandConsoleTileGridSystem;
    private CommandConsoleChunkFieldSystem commandConsoleChunkFieldSystem;
    private CommandConsoleDialSystem commandConsoleDialSystem;

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
        this.commandConsoleChunkFieldSystem = get(CommandConsoleChunkFieldSystem.class);
        this.commandConsoleDialSystem = get(CommandConsoleDialSystem.class);
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
        commandConsoleChunkFieldSystem.clearFields();
        commandConsoleDialSystem.clearDials();

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
            else if (commandHandle.takesChunk())
                injectChunkCommand(commandConsoleMenu, commandHandle);
            else if (commandHandle.takesTime())
                injectTimeCommand(commandConsoleMenu, commandHandle);
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

        injectHeader(
                commandConsoleMenu,
                commandHandle,
                CommandConsoleSetting.MENU_TILE_HEADER,
                CommandConsoleSetting.ELEMENT_TILE_HEADER_LABEL);

        for (ItemCategory itemCategory : ItemCategory.VALUES) {

            ObjectArrayList<ItemDefinitionHandle> items = commandConsoleTileGridSystem.collectItems(itemCategory);

            if (items.isEmpty())
                continue;

            ElementInstance grid = injectCategory(
                    commandConsoleMenu,
                    commandHandle,
                    itemCategory.getTitle(),
                    CommandConsoleSetting.MENU_TILE_GRID);

            if (grid != null)
                commandConsoleTileGridSystem.addItemGrid(grid, commandHandle, items);
        }
    }

    private void injectVehicleCommand(MenuInstance commandConsoleMenu, CommandHandle commandHandle) {
        for (VehicleCategory vehicleCategory : VehicleCategory.VALUES) {

            ObjectArrayList<VehicleHandle> vehicles = commandConsoleTileGridSystem.collectVehicles(vehicleCategory);

            if (vehicles.isEmpty())
                continue;

            ElementInstance grid = injectCategory(
                    commandConsoleMenu,
                    commandHandle,
                    vehicleCategory.getTitle(),
                    CommandConsoleSetting.MENU_TILE_GRID);

            if (grid != null)
                commandConsoleTileGridSystem.addVehicleGrid(grid, commandHandle, vehicles);
        }
    }

    private void injectChunkCommand(MenuInstance commandConsoleMenu, CommandHandle commandHandle) {
        treeElements.add(menuManager.inject(
                commandConsoleMenu,
                CommandConsoleSetting.ENTRY_COMMAND_TREE,
                CommandConsoleSetting.MENU_CHUNK_ENTRY,
                element -> {
                    setChildText(element, CommandConsoleSetting.ELEMENT_CHUNK_LABEL, commandHandle.getLabel());
                    commandConsoleChunkFieldSystem.addChunkFields(element, commandHandle);
                }));
    }

    private void injectTimeCommand(MenuInstance commandConsoleMenu, CommandHandle commandHandle) {

        injectHeader(
                commandConsoleMenu,
                commandHandle,
                CommandConsoleSetting.MENU_DIAL_HEADER,
                CommandConsoleSetting.ELEMENT_DIAL_HEADER_LABEL);

        String groupTitle = null;
        ElementInstance row = null;

        for (DevTimeUnit unit : DevTimeUnit.VALUES) {

            if (!commandConsoleDialSystem.isAvailable(unit))
                continue;

            if (!unit.getGroupTitle().equals(groupTitle)) {
                groupTitle = unit.getGroupTitle();
                row = injectCategory(
                        commandConsoleMenu,
                        commandHandle,
                        groupTitle,
                        CommandConsoleSetting.MENU_DIAL_ROW);
            }

            if (row != null)
                commandConsoleDialSystem.addDial(row, commandHandle, unit);
        }
    }

    private void injectHeader(
            MenuInstance commandConsoleMenu,
            CommandHandle commandHandle,
            String headerMenu,
            String labelId) {

        treeElements.add(menuManager.inject(
                commandConsoleMenu,
                CommandConsoleSetting.ENTRY_COMMAND_TREE,
                headerMenu,
                element -> setChildText(element, labelId, commandHandle.getLabel())));
    }

    // A category's header under its command, and the container its contents go in — null while it is collapsed
    private ElementInstance injectCategory(
            MenuInstance commandConsoleMenu,
            CommandHandle commandHandle,
            String categoryTitle,
            String contentMenu) {

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

        ElementInstance content = menuManager.inject(
                commandConsoleMenu,
                CommandConsoleSetting.ENTRY_COMMAND_TREE,
                contentMenu);

        treeElements.add(content);

        return content;
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
                    || commandHandles.get(i).takesVehicle() || commandHandles.get(i).takesChunk()
                    || commandHandles.get(i).takesTime())
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
