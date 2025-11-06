package com.AdventureRPG.LightingSystem;

import com.AdventureRPG.PassManager.PassManager;
import com.AdventureRPG.ShaderManager.UniversalUniformType;
import com.AdventureRPG.Core.GameSystem;
import com.AdventureRPG.PassManager.PassData;
import com.AdventureRPG.TimeSystem.TimeSystem;

public class Sky extends GameSystem {

    // Root
    private PassManager passManager;
    private TimeSystem timeSystem;

    // Shader
    private int skyPassID;
    private PassData skyPass;

    // Uniforms
    private float u_overcast;

    // Base \\

    @Override
    public void init() {

        // Root
        this.passManager = rootManager.passManager;
        this.timeSystem = rootManager.timeSystem;

        // Uniforms
        this.u_overcast = 0;
    }

    @Override
    public void awake() {

        // Shader
        skyPassID = passManager.getPassID("sky");
    }

    @Override
    public void start() {

        // Shader
        skyPass = passManager.createPassInstance(skyPassID, -5);
        skyPass.setUniform("u_overcast", u_overcast);
    }

    @Override
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
