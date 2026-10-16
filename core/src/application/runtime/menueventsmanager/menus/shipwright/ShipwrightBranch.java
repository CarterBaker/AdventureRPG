package application.runtime.menueventsmanager.menus.shipwright;

import application.bootstrap.entitypipeline.player.UseTargetStruct;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.itempipeline.itemdefinition.ItemStation;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightStorage;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.shipwrightpipeline.shipwrightmanager.ShipwrightManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.shipwright.ShipwrightSystem;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.settings.KeyBindings;
import engine.util.arpg.ArpgUtility;

public class ShipwrightBranch extends BranchPackage {

    /*
     * Menu event handlers for the drafting table, the same in the game and the
     * editor; every action works on the session of this window's table. In
     * the game, activating a shipwright's station where it stands, in the
     * world or aboard a vehicle, opens the table writing to the player's saves,
     * found through the same use target a chest is opened through. Launching
     * a saved ship sets it afloat beside the player and walks away from the
     * table.
     */

    // Internal
    private ShipwrightSystem shipwrightSystem;
    private ShipwrightManager shipwrightManager;
    private PlayerManager playerManager;
    private InputManager inputManager;

    // Use — what the activate binding would use
    private UseTargetStruct useTarget;

    // Base \\

    @Override
    protected void create() {

        // Use
        this.useTarget = new UseTargetStruct();
    }

    @Override
    protected void get() {
        this.shipwrightSystem = get(ShipwrightSystem.class);
        this.shipwrightManager = get(ShipwrightManager.class);
        this.playerManager = get(PlayerManager.class);
        this.inputManager = get(InputManager.class);
    }

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        int windowID = window.getWindowID();

        if (shipwrightSystem.isOpen()
                || !playerManager.isPlayingForWindow(windowID)
                || !inputManager.bindingClicked(KeyBindings.ACTIVATE, window)
                || !playerManager.findUseTargetForWindow(windowID, useTarget)
                || useTarget.getItemDefinitionHandle().getStation() != ItemStation.SHIPWRIGHT)
            return;

        shipwrightSystem.open(ShipwrightStorage.SAVES);
    }

    // Table \\

    public void closeTable() {
        shipwrightSystem.close();
    }

    public void launchShip(WindowInstance window) {

        int windowID = window.getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        boolean launched = shipwrightManager.launchShip(
                session(),
                playerManager.getPlayerForWindow(windowID),
                playerManager.getCameraForWindow(windowID).getDirection());

        if (launched)
            shipwrightSystem.close();
    }

    // File \\

    public void beginNaming() {
        shipwrightSystem.beginNaming();
    }

    public void newShip() {
        shipwrightManager.newShip(session());
    }

    public void openNext() {
        shipwrightManager.openShip(session(), 1);
    }

    public void openPrevious() {
        shipwrightManager.openShip(session(), -1);
    }

    public void saveShip() {
        shipwrightManager.saveShip(session());
    }

    public void undo() {
        shipwrightManager.undo(session());
    }

    public void redo() {
        shipwrightManager.redo(session());
    }

    // Tools \\

    public void selectTool(String toolName) {
        shipwrightManager.setTool(session(), ArpgUtility.toEnum(toolName, ShipwrightTool.class));
    }

    public void turn() {
        shipwrightManager.turn(session());
    }

    // Layers \\

    public void layerUp() {
        shipwrightManager.stepLayer(session(), 1);
    }

    public void layerDown() {
        shipwrightManager.stepLayer(session(), -1);
    }

    public void copyBelow() {
        shipwrightManager.copyLayer(session(), -1);
    }

    public void copyAbove() {
        shipwrightManager.copyLayer(session(), 1);
    }

    public void clearLayer() {
        shipwrightManager.clearLayer(session());
    }

    public void waterlineUp() {
        shipwrightManager.stepWaterline(session(), 1);
    }

    public void waterlineDown() {
        shipwrightManager.stepWaterline(session(), -1);
    }

    // Size \\

    public void longer() {
        shipwrightManager.resize(session(), 1, 0, 0);
    }

    public void shorter() {
        shipwrightManager.resize(session(), -1, 0, 0);
    }

    public void wider() {
        shipwrightManager.resize(session(), 0, 1, 0);
    }

    public void narrower() {
        shipwrightManager.resize(session(), 0, -1, 0);
    }

    public void higher() {
        shipwrightManager.resize(session(), 0, 0, 1);
    }

    public void lower() {
        shipwrightManager.resize(session(), 0, 0, -1);
    }

    // Options \\

    public void cycleRails() {
        shipwrightManager.cycleRails(session());
    }

    public void togglePorch() {
        shipwrightManager.togglePorch(session());
    }

    public void nextFurnishing() {
        shipwrightManager.stepFurnishing(session(), 1);
    }

    public void previousFurnishing() {
        shipwrightManager.stepFurnishing(session(), -1);
    }

    // Masts \\

    public void mastTaller() {
        shipwrightManager.stepMastHeight(session(), EngineSetting.SHIPWRIGHT_MAST_STEP);
    }

    public void mastShorter() {
        shipwrightManager.stepMastHeight(session(), -EngineSetting.SHIPWRIGHT_MAST_STEP);
    }

    public void toggleCrowsNest() {
        shipwrightManager.toggleCrowsNest(session());
    }

    public void toggleRigging() {
        shipwrightManager.toggleRigging(session());
    }

    // Utility \\

    private ShipwrightSessionInstance session() {
        return shipwrightSystem.getSession();
    }
}
