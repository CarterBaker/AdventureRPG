package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.Shaders.Shader;

public class ShaderManager extends ManagerFrame {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());
    }

    @Override
    protected void awake() {
        compileShaders();
    }

    @Override
    protected void freeMemory() {
        internalLoadManager = (InternalLoadManager) release(internalLoadManager);
    }

    // Shader Management \\

    private void compileShaders() {
        internalLoadManager.loadShaders();
    }

    // Utility \\

    public void compileShader(Shader shader) {

    }
}
