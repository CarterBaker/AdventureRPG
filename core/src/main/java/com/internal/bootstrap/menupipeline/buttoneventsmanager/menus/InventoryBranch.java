package com.internal.bootstrap.menupipeline.buttoneventsmanager.menus;

import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.entitypipeline.inventory.InventoryHandle;
import com.internal.bootstrap.menupipeline.element.ElementInstance;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.menupipeline.menumanager.MenuManager;
import com.internal.core.engine.BranchPackage;

public class InventoryBranch extends BranchPackage {

    private MenuManager menuManager;
    private MenuInstance inventoryMenu;

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    // Open / Close \\

    public void openInventory(EntityHandle entity) {
        if (inventoryMenu != null)
            return;
        inventoryMenu = menuManager.openMenu("Inventory/Inventory");
        rebuildUI(entity.getInventoryHandle());
    }

    public void closeInventory() {
        if (inventoryMenu == null)
            return;
        inventoryMenu = menuManager.closeMenu(inventoryMenu);
    }

    public void toggleInventory(EntityHandle entity) {
        if (inventoryMenu != null)
            closeInventory();
        else
            openInventory(entity);
    }

    public boolean isOpen() {
        return inventoryMenu != null;
    }

    // UI Rebuild \\

    /*
     * Clears and repopulates the item list from the backpack contents.
     * Call after any inventory mutation to keep UI in sync.
     */
    public void rebuildUI(InventoryHandle inventory) {
        if (inventoryMenu == null)
            return;

        ElementInstance list = inventoryMenu.getEntryPoint(0);
        if (list == null)
            return;

        // Clear all existing entries
        while (!list.getChildren().isEmpty())
            menuManager.eject(inventoryMenu, 0, list.getChildren().get(0));

        // Inject one slot per backpack item
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