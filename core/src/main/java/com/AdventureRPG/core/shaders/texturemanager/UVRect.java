package com.AdventureRPG.core.shaders.texturemanager;

public class UVRect {

    public final float u0, v0; // bottom-left
    public final float u1, v1; // top-right

    public UVRect(float u0, float v0, float u1, float v1) {
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
    }
}