package application.runtime.menueventsmanager.menus.inventory;

import application.runtime.RuntimeSetting;

public enum InventoryContainer {

    /*
     * The containers the inventory can have open, each shown in its own panel
     * with its own list: the worn bag on the left, and on the right a chest or
     * bag opened where it lies in the world. Both can be open at once, so
     * items move between them by hand.
     */

    BACKPACK(RuntimeSetting.MENU_INVENTORY_BAG, RuntimeSetting.MENU_INVENTORY_BAG_LIST),
    CHEST(RuntimeSetting.MENU_INVENTORY_CHEST, RuntimeSetting.MENU_INVENTORY_CHEST_LIST);

    // Values
    public static final InventoryContainer[] VALUES = values();

    // Menus
    private final String panelMenuName;
    private final String listMenuName;

    // Constructor \\

    InventoryContainer(String panelMenuName, String listMenuName) {
        this.panelMenuName = panelMenuName;
        this.listMenuName = listMenuName;
    }

    // Accessible \\

    public String getPanelMenuName() {
        return panelMenuName;
    }

    public String getListMenuName() {
        return listMenuName;
    }
}
