package application.runtime.shipwright;

public enum ShipwrightLeave {

    /*
     * What would take the ship on a drafting table's sheet away: starting a
     * new one, opening another, or closing the table — walking away from it
     * in the game, closing its tab in the editor. Any of them over unsaved
     * work asks first whether to save it, throw it away or stay.
     */

    NEW,
    OPEN,
    CLOSE
}
