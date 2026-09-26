package editor.commandconsole.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.commandconsole.CommandConsoleSetting;
import engine.root.SystemPackage;

public class CommandConsolePanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and hosts the command console
     * menu for the context's lifetime. The command line shows whatever text
     * CommandConsoleInputSystem hands it, and the menu itself is shared with
     * CommandConsoleTreeSystem, which fills the command tree.
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;

    // Menus
    private MenuInstance commandConsoleMenu;
    private ElementInstance commandLabel;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.commandConsoleMenu = menuManager.openMenu(CommandConsoleSetting.MENU_COMMAND_CONSOLE, window);
        this.commandLabel = commandConsoleMenu.getEntryPoint(CommandConsoleSetting.ENTRY_COMMAND_LINE);

        if (commandLabel == null)
            throwException("Command console menu '" + CommandConsoleSetting.MENU_COMMAND_CONSOLE
                    + "' has no command line entry point.");

        if (commandConsoleMenu.getEntryPoint(CommandConsoleSetting.ENTRY_COMMAND_TREE) == null)
            throwException("Command console menu '" + CommandConsoleSetting.MENU_COMMAND_CONSOLE
                    + "' has no command tree entry point.");
    }

    @Override
    protected void dispose() {

        menuManager.closeMenu(commandConsoleMenu);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }

    // Management \\

    public void setCommandText(String text) {
        commandLabel.setFontText(text);
    }

    // Accessible \\

    public MenuInstance getCommandConsoleMenu() {
        return commandConsoleMenu;
    }
}
