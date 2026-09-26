package editor.console.commandtree;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.console.ConsoleSetting;
import editor.console.panel.ConsolePanelSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class ConsoleCommandTreeSystem extends SystemPackage {

    /*
     * Lists every command that takes no arguments in the console's command
     * tree, under the group that defines it, so each one runs with a single
     * click. A group with no such command is left out. Groups start expanded
     * and collapse on click; the tree is laid out on the first frame and again
     * only when a group is toggled.
     */

    // Internal
    private MenuManager menuManager;
    private CommandManager commandManager;
    private ConsolePanelSystem consolePanelSystem;

    // Tree
    private ObjectArrayList<ElementInstance> treeElements;
    private ObjectOpenHashSet<String> collapsedGroupNames;
    private boolean layoutPending;

    // Base \\

    @Override
    protected void create() {

        // Tree
        this.treeElements = new ObjectArrayList<>();
        this.collapsedGroupNames = new ObjectOpenHashSet<>();
        this.layoutPending = true;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.commandManager = get(CommandManager.class);
        this.consolePanelSystem = get(ConsolePanelSystem.class);
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

        MenuInstance consoleMenu = consolePanelSystem.getConsoleMenu();
        ObjectArrayList<String> groupNames = commandManager.getGroupNames();

        for (int i = 0; i < treeElements.size(); i++)
            menuManager.eject(consoleMenu, ConsoleSetting.ENTRY_COMMAND_TREE, treeElements.get(i));

        treeElements.clear();

        for (int i = 0; i < groupNames.size(); i++)
            injectGroup(consoleMenu, groupNames.get(i));
    }

    private void injectGroup(MenuInstance consoleMenu, String groupName) {

        ObjectArrayList<CommandHandle> commandHandles = commandManager.getCommandHandles(groupName);

        if (!hasArgumentFreeCommand(commandHandles))
            return;

        boolean expanded = !collapsedGroupNames.contains(groupName);

        treeElements.add(menuManager.inject(
                consoleMenu,
                ConsoleSetting.ENTRY_COMMAND_TREE,
                ConsoleSetting.MENU_COMMAND_GROUP,
                element -> {
                    element.setActionArgOverride(groupName);
                    setChildText(element, ConsoleSetting.ELEMENT_GROUP_MARKER, expanded
                            ? EngineSetting.HIERARCHY_EXPANDED_MARKER
                            : EngineSetting.HIERARCHY_COLLAPSED_MARKER);
                    setChildText(element, ConsoleSetting.ELEMENT_GROUP_LABEL, groupName);
                }));

        if (!expanded)
            return;

        for (int i = 0; i < commandHandles.size(); i++)
            if (commandHandles.get(i).isArgumentFree())
                injectCommand(consoleMenu, commandHandles.get(i));
    }

    private void injectCommand(MenuInstance consoleMenu, CommandHandle commandHandle) {
        treeElements.add(menuManager.inject(
                consoleMenu,
                ConsoleSetting.ENTRY_COMMAND_TREE,
                ConsoleSetting.MENU_COMMAND_ENTRY,
                element -> {
                    element.setActionArgOverride(commandHandle.getCommandName());
                    setChildText(element, ConsoleSetting.ELEMENT_COMMAND_LABEL, commandHandle.getLabel());
                }));
    }

    // Management \\

    public void toggleCommandGroup(String groupName) {

        if (!collapsedGroupNames.remove(groupName))
            collapsedGroupNames.add(groupName);

        this.layoutPending = true;
    }

    // Utility \\

    private boolean hasArgumentFreeCommand(ObjectArrayList<CommandHandle> commandHandles) {

        for (int i = 0; i < commandHandles.size(); i++)
            if (commandHandles.get(i).isArgumentFree())
                return true;

        return false;
    }

    private void setChildText(ElementInstance element, String childId, String text) {

        ElementInstance child = element.findChildById(childId);

        if (child != null)
            child.setFontText(text);
    }
}
