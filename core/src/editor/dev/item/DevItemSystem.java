package editor.dev.item;

import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.menueventsmanager.menus.inventory.InventoryBranch;
import engine.editor.EditorSetting;
import engine.root.SystemPackage;

public class DevItemSystem extends SystemPackage {

    /*
     * Hands items to this Dev window's player for testing. Driven by the
     * command console's give command, which names an item by its full name
     * or by a local or display name only one item carries. While the cursor
     * is over this window's open inventory the new item lands where it
     * points — a bag view, a slot, or a list — exactly as if it had been
     * carried there, which is what dropping an item tile from the command
     * console does. Otherwise it is packed into a worn backpack when it has
     * room and held in a free hand, exactly as picking it up would; a
     * free-flying window has no character to receive it.
     */

    // Internal
    private PlayerManager playerManager;
    private ItemDefinitionManager itemDefinitionManager;
    private ItemManager itemManager;
    private InventoryBranch inventoryBranch;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.itemManager = get(ItemManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
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

        InventoryHandle inventory = playerManager.getPlayerForWindow(windowID).getInventoryHandle();
        ItemInstance itemInstance = itemManager.createItem(item);

        if (inventoryBranch.receiveItem(window, itemInstance) || inventory.give(itemInstance))
            log(window.getTitle() + EditorSetting.COMMAND_MESSAGE_ITEM_GIVEN + item.getDisplayName());
        else
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_ITEM_NO_ROOM + item.getDisplayName());
    }
}
