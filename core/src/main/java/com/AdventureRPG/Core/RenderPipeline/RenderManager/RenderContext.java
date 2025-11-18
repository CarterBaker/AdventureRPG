package com.AdventureRPG.Core.RenderPipeline.RenderManager;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.RenderPipeline.CameraSystem.CameraSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;

public class RenderContext extends SystemFrame {

    // Time
    private float deltaTime;

    // Camera
    private CameraSystem cameraSystem;

    // Base \\

    @Override
    protected void create() {

        // Time
        this.deltaTime = 0.0f;
    }

    @Override
    protected void init() {

        // Camera
        this.cameraSystem = gameEngine.get(CameraSystem.class);
    }

    @Override
    protected void update() {

        deltaTime += Gdx.graphics.getDeltaTime();
    }

    // Accessible \\

    public float deltaTime() {
        return deltaTime;
    }

    public Camera getCamera() {
        return cameraSystem.mainCamera().getPerspectiveCamera();
    }
}