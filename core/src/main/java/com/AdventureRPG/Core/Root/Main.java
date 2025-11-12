package com.AdventureRPG.Core.Root;

import java.io.File;

import com.AdventureRPG.Core.Exceptions.FileException;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;

// TODO: This needs to be cleaned
public class Main extends Game {

    // Root
    private final File GAME_DIRECTORY;
    private final Settings settings;
    private final Gson gson;

    // Core
    private EngineManager engineManager;

    // fixed interval
    private float fixedInterval;
    private float elapsedTime;
    private int maxSteps;

    // Base \\

    public Main(
            File GAME_DIRECTORY,
            Settings settings,
            Gson gson) {

        // Root
        this.GAME_DIRECTORY = GAME_DIRECTORY;
        this.settings = settings;
        this.gson = gson;
    }

    @Override
    public void create() {

        // Main
        this.engineManager = new EngineManager();

        engineManager.bootKernel(
                settings,
                this,
                GAME_DIRECTORY,
                gson);

        // fixed interval
        this.fixedInterval = settings.FIXED_TIME_STEP;
        this.elapsedTime = 0.0f;
        this.maxSteps = 5;

        // create()
        engineManager.internalCreate(settings, engineManager);

        // init()
        engineManager.internalInit();

        mainAwake();
    }

    @Override
    public void render() {

        stateSwitch();

        mainUpdate();
        mainFixedUpdate();
        mainLateUpdate();

        // render()
        engineManager.internalRender();
    }

    private void stateSwitch() {

        switch (engineManager.getInternalState()) {

            case CONSTRUCTOR -> {
            }

            case FIRST_FRAME ->
                mainStart();

            case MENU_EXCLUSIVE ->
                mainMenuExclusiveUpdate();

            case GAME_EXCLUSIVE ->
                mainGameExclusiveUpdate();

            case EXIT -> {
            }
        }
    }

    // Awake \\

    private void mainAwake() {

        // Start the game
        setScreen(engineManager);

        // awake()
        engineManager.internalAwake();
    }

    // Start \\

    private void mainStart() {

        // start()
        engineManager.internalStart();
    }

    // Menu Exclusive Update \\

    private void mainMenuExclusiveUpdate() {

        // menuExclusiveUpdate()
        engineManager.internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    private void mainGameExclusiveUpdate() {

        // gameExcusiveUpdate()
        engineManager.internalGameExclusiveUpdate();
    }

    // update \\

    private void mainUpdate() {

        // update()
        engineManager.internalUpdate();
    }

    // Fixed Update \\

    private void mainFixedUpdate() {

        elapsedTime += Gdx.graphics.getDeltaTime();
        int steps = 0;

        while (elapsedTime >= fixedInterval && steps < maxSteps) {

            elapsedTime -= fixedInterval;
            steps++;

            engineManager.internalFixedUpdate();
        }
    }

    // Late Update \\

    private void mainLateUpdate() {

        // lateUpdate()
        engineManager.internalLateUpdate();
    }

    // Dispose \\

    @Override
    public void dispose() {

        // dispse()
        engineManager.internalDispose();

        // Settings
        HandleGameWindow();
        HandleSettingsFile();

        // Game
        super.dispose();
    }

    // Close the main window on game close
    private void HandleGameWindow() {

        if (getScreen() != null)
            getScreen().dispose();
    }

    // Save the window size to the settings file
    private void HandleSettingsFile() {

        if (!settings.debug)
            return;

        File settingsFile = new File(GAME_DIRECTORY, "settings.json");

        if (settingsFile.exists()) {

            boolean deleted = settingsFile.delete();

            if (!deleted)
                throw new FileException.FileNotFoundException(settingsFile);
        }
    }
}
