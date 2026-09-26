package application.runtime.menueventsmanager.menus.inventory;

public enum InventoryDragMode {

    /*
     * What the current press of the primary button is doing. A press starts
     * at NONE; the first drag callback it reaches decides whether it carries
     * an item, turns a container view, or does nothing, and that holds until
     * the button is released.
     */

    NONE,
    CARRY,
    TURN_VIEW,
    SPENT
}
