package com.AdventureRPG.LightingSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.PassManager.PassManager;
import com.AdventureRPG.ShaderManager.UniversalUniformType;
import com.AdventureRPG.PassManager.PassData;
import com.AdventureRPG.TimeSystem.TimeSystem;

public class Sky {

    private final PassManager passManager;
    private final TimeSystem timeSystem;
    private int skyPassID;

    private PassData skyPass;

    private float u_overcast;

    public Sky(GameManager gameManager) {

        this.passManager = gameManager.passManager;
        this.timeSystem = gameManager.timeSystem;

        this.u_overcast = 0;
    }

    public void awake() {

        skyPassID = passManager.getPassID("sky");
    }

    public void start() {

        skyPass = passManager.createPassInstance(skyPassID, -5);
        skyPass.setUniform("u_overcast", u_overcast);
    }

    public void update() {
        skyPass.setUniversalUniform(UniversalUniformType.u_inverseView);
        skyPass.setUniversalUniform(UniversalUniformType.u_inverseProjection);
        skyPass.setUniform("u_timeOfDay", (float) timeSystem.getTimeOfDay());
        skyPass.setUniversalUniform(UniversalUniformType.u_time);
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
