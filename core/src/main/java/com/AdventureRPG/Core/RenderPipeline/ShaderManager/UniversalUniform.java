package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import com.AdventureRPG.Core.Root.SystemFrame;
import com.AdventureRPG.PlayerSystem.PlayerManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;

public class UniversalUniform extends SystemFrame {

    // Time
    private float u_time;

    // Camera
    private Camera camera;

    // Base \\

    @Override
    protected void create() {

        // Time
        this.u_time = 0.0f;
    }

    @Override
    protected void init() {

        // Camera
        PlayerManager playerManager = rootManager.get(PlayerManager.class);
        this.camera = playerManager.getCamera();
    }

    @Override
    protected void update() {

        u_time += Gdx.graphics.getDeltaTime();
    }

    // Universal Uniform \\

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
