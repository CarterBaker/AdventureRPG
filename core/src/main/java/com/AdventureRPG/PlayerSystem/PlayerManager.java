package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.Core.Root.ManagerFrame;
import com.AdventureRPG.Util.Vector2Int;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class PlayerManager extends ManagerFrame {

    // Player
    private Statistics statistics;
    private PlayerCamera playerCamera;
    private PositionManager positionManager;

    // Base \\

    @Override
    protected void create() {

        // Player
        this.statistics = (Statistics) register(new Statistics());
        this.playerCamera = (PlayerCamera) register(new PlayerCamera(
                settings.FOV,
                settings.windowWidth,
                settings.windowHeight));
        this.positionManager = (PositionManager) register(new PositionManager(statistics));
    }

    // Accessible \\

    public PerspectiveCamera getCamera() {
        return playerCamera.get();
    }

    public void updateViewport(int width, int height) {
        playerCamera.updateViewport(width, height);
    }

    public Vector3 currentPosition() {
        return positionManager.currentPosition();
    }

    public Vector2Int chunkCoordinate() {
        return positionManager.chunkCoordinate();
    }
}
