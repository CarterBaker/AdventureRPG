package application.runtime.menueventsmanager.menus.inventory;

public enum InventoryContainer {

    /*
     * The containers the inventory can show beside the equipment panel. A
     * chest's panels sit directly above the backpack's when both are open,
     * and either takes the full height alone.
     */

    BACKPACK,
    CHEST;

    // Values
    public static final InventoryContainer[] VALUES = values();
}
