package application.bootstrap.shipwrightpipeline.shipwright;

public enum ShipwrightStorage {

    /*
     * Where a drafting table keeps the ships drawn at it. The editor's table
     * writes ship files into the game's assets for every player to sail; a
     * table in the game writes them into the player's own saves, shared by
     * every character on the machine.
     */

    ASSETS,
    SAVES
}
