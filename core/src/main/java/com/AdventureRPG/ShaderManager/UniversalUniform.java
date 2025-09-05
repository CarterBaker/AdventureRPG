package com.AdventureRPG.ShaderManager;

import com.AdventureRPG.GameManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;

public class UniversalUniform {

    // Game Manager
    private final GameManager gameManager;

    // Camera
    private Camera camera;

    // Time
    private float u_time;

    // Base \\

    public UniversalUniform(GameManager gameManager) {

        // Game Manager
        this.gameManager = gameManager;

        // Time
        this.u_time = 0.0f;
    }

    public void awake() {

        this.camera = gameManager.playerSystem.camera.get();
    }

    public void update() {

        u_time += Gdx.graphics.getDeltaTime();
    }

    public void setUniversalUniform(ShaderData data, UniversalUniformType uniform) {

        switch (uniform) {

            case u_inverseView ->
                data.setUniform("u_inverseView", new Matrix4(camera.view).inv());

            case u_inverseProjection ->
                data.setUniform("u_inverseProjection", new Matrix4(camera.projection).inv());

            case u_time ->
                data.setUniform("u_time", u_time);
        }
    }
}
