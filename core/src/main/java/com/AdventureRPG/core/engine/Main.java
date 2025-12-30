package com.AdventureRPG.core.engine;

import java.io.File;

import com.AdventureRPG.core.engine.settings.Settings;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;

public class Main extends Game {

    // Root
    private final File GAME_DIRECTORY;
    private final Settings settings;
    private final Gson gson;

    // Internal
    private GameEngine internal;

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

        // Internal
        this.internal = new GameEngine(
                settings,
                this,
                GAME_DIRECTORY,
                gson);

        this.fixedInterval = settings.FIXED_TIME_STEP;
        this.elapsedTime = 0.0f;
        this.maxSteps = 5;

        stateSwitch();
    }

    @Override
    public void render() {

        setDeltaTime();

        super.render();
        stateSwitch();
    }

    // State Management \\

    private void stateSwitch() {

        switch (internal.getInternalState()) {

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
        internal.internalBootKernel();
        setScreen(internal.setScreen());

        internalCreate();
        internalInit();
        internalAwake();
        internalFreeMemory();

        internal.setInternalState(InternalState.FIRST_FRAME);
    }

    // Start Cycle \\

    private void startCycle() {

        internalStart();

        // TODO: I want the game to start on game and work dynamically.
        internal.setInternalState(InternalState.MENU_EXCLUSIVE);
    }

    // Menu Cycle \\

    private void menuCycle() {
        internalUpdate();
        internalMenuExclusiveUpdate();
        internalFixedUpdate();
        internalLateUpdate();
        internalRender();
        internalDraw();
    }

    // Game Cycle \\

    private void gameCycle() {
        internalUpdate();
        internalGameExclusiveUpdate();
        internalFixedUpdate();
        internalLateUpdate();
        internalRender();
        internalDraw();
    }

    // Exit Cycle \\

    private void exitCycle() {

        internalDispose();
    }

    // Create \\

    private void internalCreate() {
        internal.internalCreate();
    }

    // Init \\

    private void internalInit() {
        internal.internalInit();
    }

    // Awake \\

    private void internalAwake() {
        internal.internalAwake();
    }

    // Free Memory \\

    private void internalFreeMemory() {
        internal.internalFreeMemory();
    }

    // Start \\

    private void internalStart() {
        internal.internalStart();
    }

    // update \\

    private void internalUpdate() {
        internal.internalUpdate();
    }

    // Menu Exclusive Update \\

    private void internalMenuExclusiveUpdate() {
        internal.internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    private void internalGameExclusiveUpdate() {
        internal.internalGameExclusiveUpdate();
    }

    // Fixed Update \\

    private void internalFixedUpdate() {

        // TODO: Micro optimiztion read and cache the delta and then send it through to
        // the internal per frame instead of reading it twice.
        elapsedTime += Gdx.graphics.getDeltaTime();
        int steps = 0;

        while (elapsedTime >= fixedInterval && steps < maxSteps) {

            elapsedTime -= fixedInterval;
            steps++;

            internal.internalFixedUpdate();
        }
    }

    // Late Update \\

    private void internalLateUpdate() {
        internal.internalLateUpdate();
    }

    // Render \\

    private void internalRender() {
        internal.internalRender();
    }

    // Draw \\

    private void internalDraw() {
        internal.internalDraw();
    }

    // Dispose \\

    private void internalDispose() {
        internal.internalDispose();
    }

    @Override
    public void dispose() {

        // Internal Engine
        internal.setInternalState(InternalState.EXIT);

        // Settings
        HandleGameWindow();
        HandleSettingsFile();

        // Game
        super.dispose();
    }

    // Utility \\

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
                throw new RuntimeException(
                        "File: " + settingsFile.getName() + ", Failed to delete on game close");
        }
    }

    // Set the games delta time for ease of access across all systems
    private void setDeltaTime() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        internal.setDeltaTime(deltaTime);
    }
}