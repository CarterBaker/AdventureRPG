package com.AdventureRPG.Core.RenderPipeline.Util;

public final class GPUHandle {

    public final int vbo;
    public final int ibo;

    public GPUHandle(int vbo, int ibo) {

        this.vbo = vbo;
        this.ibo = ibo;
    }
}