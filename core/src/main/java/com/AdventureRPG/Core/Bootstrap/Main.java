package com.AdventureRPG.Core.Bootstrap;

import java.io.File;

import com.AdventureRPG.Core.Util.Exceptions.FileException;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;

public class Main extends Game {

    // Root
    private final File GAME_DIRECTORY;
    private final Settings settings;
    private final Gson gson;

    // Core
    private GameEngine gameEngine;

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
        this.gameEngine = new GameEngine(
                settings,
                this,
                GAME_DIRECTORY,
                gson);

        // fixed interval
        this.fixedInterval = settings.FIXED_TIME_STEP;
        this.elapsedTime = 0.0f;
        this.maxSteps = 5;

        stateSwitch();
    }

    @Override
    public void render() {
        stateSwitch();
    }

    // State Management \\

    private void stateSwitch() {

        switch (gameEngine.getInternalState()) {

            case CONSTRUCTOR -> {
                bootCycle();
            }

            case FIRST_FRAME ->
                startCycle();

            case MENU_EXCLUSIVE ->
                menuCycle();

            case GAME_EXCLUSIVE ->
                gameCycle();

            case EXIT -> {
                exitCycle();
            }
        }
    }

    // Boot Cycle \\

    private void bootCycle() {

        // Start the game
        gameEngine.internalBootKernel();

        setScreen(gameEngine);

        internalCreate();
        internalInit();
        internalAwake();

        gameEngine.setInternalState(InternalState.FIRST_FRAME);
    }

    // Start Cycle \\

    private void startCycle() {

        internalStart();

        // TODO: I want the game to start on game and work dynamically.
        gameEngine.setInternalState(InternalState.MENU_EXCLUSIVE);
    }

    // Menu Cycle \\

    private void menuCycle() {

        internalMenuExclusiveUpdate();

        updateCycle();
    }

    // Game Cycle \\

    private void gameCycle() {

        internalGameExclusiveUpdate();

        updateCycle();
    }

    // Update Cycle \\

    private void updateCycle() {

        internalUpdate();
        internalFixedUpdate();
        internalLateUpdate();
        internalRender();
    }

    // Exit Cycle \\

    private void exitCycle() {

        internalDispose();
    }

    // Create \\

    private void internalCreate() {
        gameEngine.internalCreate();
    }

    // Init \\

    private void internalInit() {
        gameEngine.internalInit();
    }

    // Awake \\

    private void internalAwake() {
        gameEngine.internalAwake();
    }

    // Start \\

    private void internalStart() {
        gameEngine.internalStart();
    }

    // Menu Exclusive Update \\

    private void internalMenuExclusiveUpdate() {
        gameEngine.internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    private void internalGameExclusiveUpdate() {
        gameEngine.internalGameExclusiveUpdate();
    }

    // update \\

    private void internalUpdate() {
        gameEngine.internalUpdate();
    }

    // Fixed Update \\

    private void internalFixedUpdate() {

        // TODO: Micro optimiztion read and cache the delta and then send it through to
        // the gameEngine per frame instead of reading it twice.
        elapsedTime += Gdx.graphics.getDeltaTime();
        int steps = 0;

        while (elapsedTime >= fixedInterval && steps < maxSteps) {

            elapsedTime -= fixedInterval;
            steps++;

            gameEngine.internalFixedUpdate();
        }
    }

    // Late Update \\

    private void internalLateUpdate() {
        gameEngine.internalLateUpdate();
    }

    // Render \\

    private void internalRender() {
        gameEngine.internalRender();
    }

    // Dispose \\

    private void internalDispose() {
        gameEngine.internalDispose();
    }

    @Override
    public void dispose() {

        // Internal Engine
        gameEngine.setInternalState(InternalState.EXIT);

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
