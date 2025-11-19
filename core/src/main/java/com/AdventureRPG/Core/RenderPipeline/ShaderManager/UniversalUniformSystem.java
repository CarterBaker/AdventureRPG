package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.RenderPipeline.CameraSystem.CameraSystem;
import com.badlogic.gdx.math.Matrix4;

// TODO: I refactored this a bit and it is not verified working
public class UniversalUniformSystem extends SystemFrame {

    // Time
    private float u_time;

    // Camera
    private CameraSystem cameraSystem;
    private Matrix4 cameraMatrix;

    // Base \\

    @Override
    protected void create() {

        // Time
        this.u_time = 0.0f;

        // Camera
        this.cameraMatrix = new Matrix4();
    }

    @Override
    protected void init() {

        // Camera
        this.cameraSystem = gameEngine.get(CameraSystem.class);
    }

    @Override
    protected void update() {
        u_time += gameEngine.getDeltaTime();
    }

    // Universal Uniform \\

    public void setUniversalUniform(ShaderData data, UniversalUniformType uniform) {

        // TODO: Do not like the switch statement, we can do better
        switch (uniform) {

            case u_inverseView -> {

                cameraMatrix.set(cameraSystem.mainCamera().getPerspectiveCamera().view);
                // cameraMatrix.inv(); TODO: Not sure why I'd do this

                data.setUniform("u_inverseView", cameraMatrix);
            }

            case u_inverseProjection -> {

                cameraMatrix.set(cameraSystem.mainCamera().getPerspectiveCamera().projection);
                cameraMatrix.inv();

                data.setUniform("u_inverseProjection", cameraMatrix);
            }

            case u_time ->
                data.setUniform("u_time", u_time);
        }
    }
}
