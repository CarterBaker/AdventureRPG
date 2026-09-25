package application.bootstrap.savepipeline.savemanager;

import java.io.File;

import com.google.gson.JsonObject;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SaveManager extends ManagerPackage {

    /*
     * Owns character saves. The world is persistent on disk and never saved
     * here — every character is its own save file that loads into that same
     * world. closeCharacter() writes and releases whatever character the
     * window was playing, leaving the player where it stands with no active
     * save; newCharacter() does the same and rolls a fresh body for the
     * character creator to shape; createCharacter() names that body and makes
     * it the active character, as continuing the most recently played one or
     * loading a chosen one also do. Continuing only succeeds when a character save exists and loads, and
     * a name is only taken by one save. Becoming a character writes it at once,
     * and the active character is written again before it is replaced and when
     * its context tears down.
     * Only the main window — the one the standalone game pairs its
     * RuntimeContext with — writes saves; editor previews may load a character
     * but never write one. PlayerSaveBranch captures a character,
     * PlayerRestoreBranch applies one, and a save that cannot be read is
     * logged and left on disk untouched.
     */

    // Internal
    private WindowManager windowManager;
    private PlayerManager playerManager;
    private PlayerSaveBranch playerSaveBranch;
    private PlayerRestoreBranch playerRestoreBranch;

    // Directory
    private File characterDirectory;

    // Active
    private String activeCharacterName;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.playerSaveBranch = create(PlayerSaveBranch.class);
        this.playerRestoreBranch = create(PlayerRestoreBranch.class);

        // Directory
        this.characterDirectory = new File(
                internal.path,
                EngineSetting.SAVE_DIRECTORY + "/" + EngineSetting.CHARACTER_SAVE_DIRECTORY);
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.playerManager = get(PlayerManager.class);
    }

    // Management \\

    public void newCharacter(WindowInstance window) {
        closeCharacter(window);
        playerManager.rerollPlayerForWindow(window.getWindowID());
    }

    public void closeCharacter(WindowInstance window) {

        saveCharacter(window);

        if (isSaveWindow(window))
            this.activeCharacterName = null;
    }

    public boolean createCharacter(WindowInstance window, String characterName) {

        if (!isSaveWindow(window))
            return true;

        if (!isCharacterNameAvailable(characterName))
            return false;

        activateCharacter(window, characterName);
        return true;
    }

    public boolean continueCharacter(WindowInstance window) {

        String characterName = getLatestCharacterName();

        return characterName != null && loadCharacter(window, characterName);
    }

    public boolean loadCharacter(WindowInstance window, String characterName) {

        File characterFile = getCharacterFile(characterName);

        if (!characterFile.isFile()) {
            errorLog("Character '" + characterName + "' no longer exists: " + characterFile.getAbsolutePath());
            return false;
        }

        saveCharacter(window);

        if (!restore(window, characterFile))
            return false;

        if (isSaveWindow(window))
            activateCharacter(window, characterName);

        return true;
    }

    public void saveCharacter(WindowInstance window) {

        if (activeCharacterName == null || !isSaveWindow(window))
            return;

        playerSaveBranch.save(
                getCharacterFile(activeCharacterName),
                playerManager.getPlayerForWindow(window.getWindowID()));
    }

    private void activateCharacter(WindowInstance window, String characterName) {
        this.activeCharacterName = characterName;
        saveCharacter(window);
    }

    // Load \\

    private boolean restore(WindowInstance window, File characterFile) {

        int windowID = window.getWindowID();
        JsonObject characterJson = JsonUtility.tryLoadJsonObject(characterFile);

        if (characterJson != null
                && playerRestoreBranch.restore(characterJson, playerManager.getPlayerForWindow(windowID))) {
            playerManager.verifyPlayerPositionForWindow(windowID);
            return true;
        }

        errorLog("Character '" + FileUtility.getFileName(characterFile)
                + "' is unreadable, malformed, or no longer matches the game and was skipped: "
                + characterFile.getAbsolutePath());
        return false;
    }

    // Utility \\

    private File getCharacterFile(String characterName) {
        return new File(characterDirectory, characterName + "." + EngineSetting.CHARACTER_FILE_EXTENSION);
    }

    private String createCharacterName() {

        int characterNumber = EngineSetting.CHARACTER_NAME_FIRST_NUMBER;

        while (getCharacterFile(EngineSetting.CHARACTER_NAME_PREFIX + characterNumber).exists())
            characterNumber++;

        return EngineSetting.CHARACTER_NAME_PREFIX + characterNumber;
    }

    private String getLatestCharacterName() {

        ObjectArrayList<String> characterNames = getCharacterNames();

        return characterNames.isEmpty() ? null : characterNames.get(0);
    }

    private boolean isSaveWindow(WindowInstance window) {
        return window == windowManager.getMainWindow();
    }

    // Accessible \\

    public boolean isCharacterNameAvailable(String characterName) {
        return !getCharacterFile(characterName).exists();
    }

    public String getDefaultCharacterName() {
        return createCharacterName();
    }

    public ObjectArrayList<String> getCharacterNames() {

        ObjectArrayList<File> characterFiles = new ObjectArrayList<>();
        File[] files = characterDirectory.listFiles();

        if (files != null)
            for (File file : files)
                if (file.isFile() && FileUtility.hasExtension(file, EngineSetting.CHARACTER_FILE_EXTENSION))
                    characterFiles.add(file);

        characterFiles.sort((first, second) -> Long.compare(second.lastModified(), first.lastModified()));

        ObjectArrayList<String> characterNames = new ObjectArrayList<>(characterFiles.size());

        for (int i = 0; i < characterFiles.size(); i++)
            characterNames.add(FileUtility.getFileName(characterFiles.get(i)));

        return characterNames;
    }
}