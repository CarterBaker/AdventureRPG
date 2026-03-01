package com.internal.bootstrap.shaderpipeline.texturemanager;

import com.internal.core.engine.HandlePackage;

/*
 * Persistent resource owned by TextureManager. Carries all data needed for a
 * single named texture tile: its tile ID, parent array ID, GPU handle, atlas
 * size, and normalised UV region. Handed out at runtime; never cloned.
 */
public class TextureHandle extends HandlePackage {

    // Internal
    private int tileID;
    private int arrayID;
    private int gpuHandle;
    private int atlasSize;
    private float u0;
    private float v0;
    private float u1;
    private float v1;

    // Internal \\

    void constructor(int tileID, int arrayID, int gpuHandle, int atlasSize,
            float u0, float v0, float u1, float v1) {
        this.tileID = tileID;
        this.arrayID = arrayID;
        this.gpuHandle = gpuHandle;
        this.atlasSize = atlasSize;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
    }

    // Accessible \\

    public int getTileID() {
        return tileID;
    }

    public int getArrayID() {
        return arrayID;
    }

    public int getGPUHandle() {
        return gpuHandle;
    }

    public int getAtlasSize() {
        return atlasSize;
    }

    public float getU0() {
        return u0;
    }

    public float getV0() {
        return v0;
    }

    public float getU1() {
        return u1;
    }

    public float getV1() {
        return v1;
    }
}