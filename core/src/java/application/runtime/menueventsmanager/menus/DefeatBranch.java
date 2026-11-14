package application.runtime.menueventsmanager.menus;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.menus.inventory.InventoryBranch;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class DefeatBranch extends BranchPackage {

    /*
     * Ends play for this context's window the frame its player is defeated.
     * Whatever menu stands open over play — the inventory or the pause menu —
     * is closed, and the character leaves through the main menu's own way
     * back, saved where it fell. The player standing in for the menu is then
     * started over, alive, so a defeat ends play exactly once.
     */

    // Internal
    private PlayerManager playerManager;
    private ProgressionManager progressionManager;
    private MainMenuBranch mainMenuBranch;
    private PauseMenuBranch pauseMenuBranch;
    private InventoryBranch inventoryBranch;
    private SoundCueManager soundCueManager;

    // Cues
    private int defeatCueID;

    // Base \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.mainMenuBranch = get(MainMenuBranch.class);
        this.pauseMenuBranch = get(PauseMenuBranch.class);
        this.inventoryBranch = get(InventoryBranch.class);
        this.soundCueManager = get(SoundCueManager.class);
    }

    @Override
    protected void awake() {

        // Cues
        this.defeatCueID = soundCueManager.getCueIDFromCueName(RuntimeSetting.SOUND_CUE_DEFEAT);
    }

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        int windowID = window.getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        if (!player.getStatisticsHandle().isDefeated())
            return;

        soundCueManager.playCue(defeatCueID);
        inventoryBranch.closeMenu();

        if (pauseMenuBranch.isOpen())
            pauseMenuBranch.closeMenu();

        mainMenuBranch.returnToMainMenu(window);
        progressionManager.resetBeing(player, EngineSetting.IDENTITY_NONE);
    }
}
