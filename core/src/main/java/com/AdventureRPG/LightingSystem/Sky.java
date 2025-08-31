package com.AdventureRPG.LightingSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.PassManager.PassManager;
import com.AdventureRPG.PassManager.PassData;
import com.AdventureRPG.TimeSystem.TimeSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;

public class Sky {

    private final GameManager gameManager;
    private final PassManager passManager;
    private final TimeSystem timeSystem;
    private Camera camera;
    private int skyPassID;

    private PassData skyPass;

    private float elapsedTime;
    private float u_overcast;
    

    public Sky(GameManager gameManager) {
        this.gameManager = gameManager;
        this.passManager = gameManager.passManager;
        this.timeSystem = gameManager.timeSystem;

        this.elapsedTime = 0.0f;
        this.u_overcast = 0;
    }

    public void awake() {
        this.camera = gameManager.playerSystem.camera.get();
        skyPassID = passManager.getPassID("sky");
    }

    public void start() {
        skyPass = passManager.createPassInstance(skyPassID, -5);

        skyPass.setUniform("u_overcast", u_overcast);
    }

    public void update() {
        elapsedTime += Gdx.graphics.getDeltaTime();

        skyPass.setUniform("u_inverseView", new Matrix4(camera.view).inv());
        skyPass.setUniform("u_inverseProjection", new Matrix4(camera.projection).inv());
        skyPass.setUniform("u_timeOfDay", (float) timeSystem.getTimeOfDay());
        skyPass.setUniform("u_time", elapsedTime);
    }

    public void generateRandomOffsetFromDay(long day) {

        long mixed = day ^ System.currentTimeMillis();

        // Simple hash
        mixed ^= (mixed >>> 33);
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= (mixed >>> 33);

        // Normalize to [0,1)
        double normalized = (mixed & 0xFFFFFFL) / (double) (1 << 24);

        float noise = (float) Math.max(0.001, normalized);
        skyPass.setUniform("u_randomNoiseFromDay", noise);
    }
}
