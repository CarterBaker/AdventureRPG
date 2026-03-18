package com.internal.bootstrap.menupipeline.menueventsmanager.menus;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.entitypipeline.inventory.InventoryHandle;
import com.internal.bootstrap.menupipeline.element.ElementInstance;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.menupipeline.menumanager.MenuManager;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.core.engine.BranchPackage;

public class InventoryBranch extends BranchPackage {

    /*
     * Handles open, close, and rebuild actions for the inventory menu. Holds
     * the active MenuInstance and drives slot injection from the entity's
     * backpack contents. WindowInstance is passed by the caller so the menu
     * is bound to the correct window.
     */

    // Internal
    private MenuManager menuManager;

    // State
    private MenuInstance inventoryMenu;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.menuManager = get(MenuManager.class);
    }

    // Open / Close \\

    public void openInventory(EntityInstance entity, WindowInstance window) {

        if (inventoryMenu != null)
            return;

        inventoryMenu = menuManager.openMenu("Inventory/Inventory", window);
        rebuildUI(entity.getInventoryHandle());
    }

    public void closeInventory() {

        if (inventoryMenu == null)
            return;

        inventoryMenu = menuManager.closeMenu(inventoryMenu);
    }

    public void toggleInventory(EntityInstance entity, WindowInstance window) {

        if (inventoryMenu != null)
            closeInventory();
        else
            openInventory(entity, window);
    }

    public boolean isOpen() {
        return inventoryMenu != null;
    }

    // UI Rebuild \\

    /*
     * Clears and repopulates the item list from the backpack contents.
     * Call after any inventory mutation to keep the UI in sync.
     */
    public void rebuildUI(InventoryHandle inventory) {

        if (inventoryMenu == null)
            return;

        ElementInstance list = inventoryMenu.getEntryPoint(0);

        if (list == null)
            return;

        while (!list.getChildren().isEmpty())
            menuManager.eject(inventoryMenu, 0, list.getChildren().get(0));

        for (int i = 0; i < inventory.getBackpack().size(); i++)
            injectSlot(inventory.getBackpack().getItems().get(i).getItemName());
    }

    private void injectSlot(String displayName) {
        menuManager.inject(inventoryMenu, 0, "Items/item_slot", el -> {
            ElementInstance label = el.findChildById("item_label");
            if (label != null)
                label.getFontInstance().setText(displayName);
        });
    }

    // Click Handler \\

    public void selectItem() {
        // Wire up selection logic when item interaction is implemented
    }
}