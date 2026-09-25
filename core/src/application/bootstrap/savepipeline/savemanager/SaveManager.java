package application.bootstrap.savepipeline.savemanager;

import java.io.File;

import com.google.gson.JsonObject;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.io.JsonUtility;

public class SaveManager extends ManagerPackage {

    /*
     * Owns game save persistence. Only the player on the main window — the one
     * the standalone game pairs its RuntimeContext with — is saved; editor
     * previews run in their own windows and never read or write the save. The
     * player is restored as it spawns and written back when its context tears
     * down. PlayerSaveBranch captures the character and its location,
     * PlayerRestoreBranch applies them, and a save that cannot be read is
     * logged and skipped so the player keeps its fresh spawn.
     */

    // Internal
    private WindowManager windowManager;
    private PlayerSaveBranch playerSaveBranch;
    private PlayerRestoreBranch playerRestoreBranch;

    // Directory
    private File saveDirectory;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.playerSaveBranch = create(PlayerSaveBranch.class);
        this.playerRestoreBranch = create(PlayerRestoreBranch.class);

        // Directory
        this.saveDirectory = new File(internal.path, EngineSetting.SAVE_DIRECTORY);
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
    }

    // Management \\

    public void savePlayer(WindowInstance window, EntityInstance player) {

        if (!isSaveWindow(window))
            return;

        playerSaveBranch.save(getPlayerSaveFile(), player);
    }

    public void restorePlayer(WindowInstance window, EntityInstance player) {

        if (!isSaveWindow(window))
            return;

        File playerSaveFile = getPlayerSaveFile();

        if (playerSaveFile.exists())
            restore(playerSaveFile, player);
    }

    // Load \\

    private void restore(File playerSaveFile, EntityInstance player) {

        JsonObject playerJson = JsonUtility.tryLoadJsonObject(playerSaveFile);

        if (playerJson != null && playerRestoreBranch.restore(playerJson, player))
            return;

        errorLog("Player save is unreadable, malformed, or no longer matches the game and was skipped: "
                + playerSaveFile.getAbsolutePath());
    }

    // Utility \\

    private File getPlayerSaveFile() {
        return new File(saveDirectory, EngineSetting.PLAYER_SAVE_FILE_NAME);
    }

    private boolean isSaveWindow(WindowInstance window) {
        return window == windowManager.getMainWindow();
    }
}