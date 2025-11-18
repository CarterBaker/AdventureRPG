package com.AdventureRPG.Core.Bootstrap;

import com.AdventureRPG.LightingSystem.LightingManager;
import com.AdventureRPG.PlayerManager.PlayerManager;
import com.AdventureRPG.SaveManager.SaveManager;
import com.AdventureRPG.TimeSystem.TimeSystem;
import com.AdventureRPG.UISystem.LoadScreen;
import com.AdventureRPG.UISystem.Menu;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldManager.WorldManager;
import com.badlogic.gdx.Screen;

public class GameEngine extends EngineFrame implements Screen {

    // Core
    private SaveManager saveManager;
    private UISystem UISystem;
    private LightingManager lightingManager;
    private TimeSystem timeSystem;
    private PlayerManager playerManager;
    private WorldManager worldManager;

    // UI
    private LoadScreen loadScreen;

    // Base \\

    @Override
    protected void create() {

        // Core
        saveManager = (SaveManager) register(new SaveManager());
        UISystem = (UISystem) register(new UISystem());
        lightingManager = (LightingManager) register(new LightingManager());
        timeSystem = (TimeSystem) register(new TimeSystem());
        playerManager = (PlayerManager) register(new PlayerManager());
        worldManager = (WorldManager) register(new WorldManager());
    }

    @Override
    protected void start() {

        startLoading();
        // UISystem.open(Menu.Main); // TODO: Commented out for debugging
    }

    public void startLoading() {

        worldManager.loadChunks();

        loadScreen = (LoadScreen) UISystem.open(Menu.LoadScreen);
        loadScreen.setMaxProgrss(worldManager.queueSystem.totalQueueSize());

        requestInternalState(InternalState.MENU_EXCLUSIVE);
    }

    @Override
    protected void menuExclusiveUpdate() { // TODO: This whole thing needs a rework
        // Game should be the natural state and these things should just sort of work
        // dynamically
        // Goes with the todo in main classS

        if (worldManager.queueSystem.hasQueue())
            loadScreen.setProgrss(worldManager.queueSystem.totalQueueSize());

        else {

            loadScreen.setProgrss(worldManager.queueSystem.totalQueueSize());
            UISystem.close(loadScreen);

            requestInternalState(InternalState.GAME_EXCLUSIVE);
        }
    }

    // Screen \\

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {

    }

    @Override
    public void resize(int width, int height) {
        cameraSystem.resize(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }
}
