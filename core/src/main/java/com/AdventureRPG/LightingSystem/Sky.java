package com.AdventureRPG.LightingSystem;

import com.AdventureRPG.Core.RenderPipeline.PassSystem.PassData;
import com.AdventureRPG.Core.RenderPipeline.PassSystem.PassSystem;
import com.AdventureRPG.Core.RenderPipeline.ShaderManager.UniversalUniformType;
import com.AdventureRPG.Core.Root.SystemFrame;
import com.AdventureRPG.TimeSystem.TimeSystem;

public class Sky extends SystemFrame {

    // Root
    private PassSystem passSystem;
    private TimeSystem timeSystem;

    // Shader
    private int skyPassID;
    private PassData skyPass;

    // Uniforms
    private float u_overcast;

    // Base \\

    @Override
    protected void init() {

        // Root
        this.passSystem = rootManager.get(PassSystem.class);
        this.timeSystem = rootManager.get(TimeSystem.class);
    }

    @Override
    protected void awake() {

        // Shader
        skyPassID = passSystem.getPassID("sky");
        skyPass = passSystem.createPassInstance(skyPassID, -5);
        skyPass.setUniform("u_overcast", u_overcast);

        // Uniforms
        this.u_overcast = 0;
    }

    @Override
    protected void update() {

        if (skyPass == null)
            return;

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

        if (skyPass != null)
            skyPass.setUniform("u_randomNoiseFromDay", noise);
    }
}
