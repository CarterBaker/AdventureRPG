package editor.dev.item;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import engine.root.SystemPackage;

public class DevItemSystem extends SystemPackage {

    /*
     * Hands items to this Dev window's player for testing. Driven by the
     * command console's give command, which names an item by its full name
     * or by a local or display name only one item carries. The new item is
     * packed into a worn backpack when it has room and held in a free hand
     * otherwise, exactly as picking it up would; a free-flying window has no
     * character to receive it.
     */

    // Internal
    private PlayerManager playerManager;
    private ItemDefinitionManager itemDefinitionManager;
    private ItemManager itemManager;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.itemManager = get(ItemManager.class);
    }

    // Management \\

    public void giveItem(String itemQuery) {

        WindowInstance window = context.getWindow();
        int windowID = window.getWindowID();
        ItemDefinitionHandle item = itemDefinitionManager.findItemHandle(itemQuery);

        if (item == null) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_ITEM_UNKNOWN + itemQuery);
            return;
        }

        if (!playerManager.hasPlayerForWindow(windowID) || playerManager.isFreeCameraForWindow(windowID)) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_ITEM_NO_CHARACTER + item.getDisplayName());
            return;
        }

        if (playerManager.getPlayerForWindow(windowID).getInventoryHandle().give(itemManager.createItem(item)))
            log(window.getTitle() + EditorSetting.COMMAND_MESSAGE_ITEM_GIVEN + item.getDisplayName());
        else
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_ITEM_NO_ROOM + item.getDisplayName());
    }
}
