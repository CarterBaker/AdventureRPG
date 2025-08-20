package com.AdventureRPG.LightingSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.PassManager.PassManager;
import com.AdventureRPG.RenderManager.RenderPass;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;

public class Sky {

    private final GameManager gameManager;
    private final PassManager passManager;
    private Camera camera;
    private int skyPassID;

    private RenderPass skyPass;

    public Sky(GameManager gameManager) {
        this.gameManager = gameManager;
        this.passManager = gameManager.passManager;
    }

    public void awake() {
        this.camera = gameManager.playerSystem.camera.get();
        skyPassID = passManager.getPassID("sky");
    }

    public void start() {
        skyPass = passManager.createPassInstance(skyPassID);
    }

    public void update() {
        skyPass.setUniform("u_inverseView", new Matrix4(camera.view).inv());
        skyPass.setUniform("u_inverseProjection", new Matrix4(camera.projection).inv());
    }
}
