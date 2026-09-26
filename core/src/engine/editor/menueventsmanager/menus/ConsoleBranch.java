package engine.editor.menueventsmanager.menus;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.console.ConsoleContext;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;

public class ConsoleBranch extends BranchPackage {

    /*
     * Menu event handlers for the console tab. Each action targets the
     * console paired with the window its button was clicked in. Give Player
     * asks for an item — its full name, or a local or display name only one
     * item carries — and hands a new one of it to every player standing in a
     * running preview, packed into a worn backpack when it has room and held
     * in a free hand otherwise. The outcome for each player is logged.
     */

    // Internal
    private NameDialogBranch nameDialogBranch;
    private ItemDefinitionManager itemDefinitionManager;
    private ItemManager itemManager;
    private PlayerManager playerManager;

    // Base \\

    @Override
    protected void get() {
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.itemManager = get(ItemManager.class);
        this.playerManager = get(PlayerManager.class);
    }

    // Console Operations \\

    public void clear(WindowInstance window) {
        if (window.getContext() instanceof ConsoleContext consoleContext)
            consoleContext.clear();
    }

    public void givePlayer(WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_GIVE_PLAYER,
                EditorSetting.TEXT_INPUT_EMPTY,
                itemName -> itemDefinitionManager.findItemHandle(itemName.trim()) != null,
                this::giveItem);
    }

    // Give \\

    private void giveItem(String itemName) {

        ItemDefinitionHandle item = itemDefinitionManager.findItemHandle(itemName.trim());
        boolean given = false;

        for (int windowID : playerManager.getPlayerWindowIDs()) {

            if (playerManager.isFreeCameraForWindow(windowID))
                continue;

            EntityInstance player = playerManager.getPlayerForWindow(windowID);
            given = true;

            if (player.getInventoryHandle().give(itemManager.createItem(item)))
                log(String.format(EditorSetting.CONSOLE_GIVE_PACKED, item.getDisplayName(), windowID));
            else
                errorLog(String.format(EditorSetting.CONSOLE_GIVE_NO_ROOM, windowID, item.getDisplayName()));
        }

        if (!given)
            errorLog(String.format(EditorSetting.CONSOLE_GIVE_NO_PLAYER, item.getDisplayName()));
    }
}
