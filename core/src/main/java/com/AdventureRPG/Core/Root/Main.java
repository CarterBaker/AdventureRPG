package com.AdventureRPG.Core.Root;

import java.io.File;

import com.AdventureRPG.Core.Exceptions.FileException;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.google.gson.Gson;

// TODO: This needs to be cleaned but is functional for now
public class Main extends Game {

    // Root
    private final File GAME_DIRECTORY;
    private final Settings settings;
    private final Gson gson;

    // Rendering
    private ShaderProvider shaderProvider;
    private Environment environment;
    private SpriteBatch spriteBatch;
    private ModelBatch modelBatch;

    // Core
    private RootManager rootManager;

    // fixed interval
    private float fixedInterval;
    private float elapsedTime;
    private int maxSteps;

    // Base \\

    public Main(
            File GAME_DIRECTORY,
            Settings settings,
            Gson gson) {

        // Settings
        this.GAME_DIRECTORY = GAME_DIRECTORY;
        this.settings = settings;
        this.gson = gson;
    }

    @Override
    public void create() {

        // Rendering
        this.shaderProvider = new DefaultShaderProvider();
        this.environment = new Environment();
        this.spriteBatch = new SpriteBatch();
        this.modelBatch = new ModelBatch();

        // Main
        this.rootManager = new RootManager(
                this,
                GAME_DIRECTORY,
                gson,
                shaderProvider,
                environment,
                spriteBatch,
                modelBatch);

        // fixed interval
        this.fixedInterval = settings.FIXED_TIME_STEP;
        this.elapsedTime = 0.0f;
        this.maxSteps = 5;

        // create()
        rootManager.internalCreate(this.settings, this.rootManager);

        // init()
        rootManager.internalInit();

        mainAwake();
    }

    @Override
    public void render() {

        stateSwitch();

        mainUpdate();
        mainFixedUpdate();
        mainLateUpdate();

        // render()
        rootManager.internalRender();
    }

    private void stateSwitch() {

        switch (rootManager.getInternalState()) {

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
        setScreen(rootManager);

        // awake()
        rootManager.internalAwake();
    }

    // Start \\

    private void mainStart() {

        // start()
        rootManager.internalStart();
    }

    // Menu Exclusive Update \\

    private void mainMenuExclusiveUpdate() {

        // menuExclusiveUpdate()
        rootManager.internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    private void mainGameExclusiveUpdate() {

        // gameExcusiveUpdate()
        rootManager.internalGameExclusiveUpdate();
    }

    // update \\

    private void mainUpdate() {

        // update()
        rootManager.internalUpdate();
    }

    // Fixed Update \\

    private void mainFixedUpdate() {

        elapsedTime += Gdx.graphics.getDeltaTime();
        int steps = 0;

        while (elapsedTime >= fixedInterval && steps < maxSteps) {

            elapsedTime -= fixedInterval;
            steps++;

            rootManager.internalFixedUpdate();
        }
    }

    // Late Update \\

    private void mainLateUpdate() {

        // lateUpdate()
        rootManager.internalLateUpdate();
    }

    // Dispose \\

    @Override
    public void dispose() {

        // dispse()
        rootManager.internalDispose();

        // Settings
        HandleGameWindow();
        HandleSettingsFile();

        // Game
        spriteBatch.dispose();
        modelBatch.dispose();
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
