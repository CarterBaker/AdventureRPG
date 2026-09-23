package application.runtime.debug;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.runtime.input.InputSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;

public class DebugWaterPlacementSystem extends SystemPackage {

    /*
     * Debug-only input linkage for the liquid pipeline: a secondary click in
     * this context's window asks the engine to pour a Water block wherever
     * that window's player is aiming. Raycasting, placement, liquid wake-up,
     * and every geometry rebuild live in the engine. Flip ENABLED to false,
     * or remove the create() call in RuntimeContext, to take this out.
     */

    private static final boolean ENABLED = true;

    // Internal
    private PlayerManager playerManager;
    private InputSystem inputSystem;
    private BlockManager blockManager;

    // Block IDs
    private short waterBlockID;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
        this.inputSystem = get(InputSystem.class);
        this.blockManager = get(BlockManager.class);
    }

    @Override
    protected void awake() {

        // Block IDs
        this.waterBlockID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.DEFAULT_WATER_BLOCK_NAME);
    }

    // Update \\

    @Override
    protected void update() {

        if (!ENABLED)
            return;

        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        if (context.getWindow().getMenuListHandle().isInputLocked())
            return;

        if (!inputSystem.getRawInputHandle().isBindingClicked(KeyBindings.SECONDARY))
            return;

        playerManager.placeBlockForWindow(windowID, waterBlockID);
    }
}