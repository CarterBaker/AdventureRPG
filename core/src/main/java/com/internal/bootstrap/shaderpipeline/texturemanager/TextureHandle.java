package com.internal.bootstrap.shaderpipeline.texturemanager;

import com.internal.core.engine.HandlePackage;

/*
 * Persistent resource owned by TextureManager. Carries all data needed for a
 * single named texture tile: its tile ID, parent array ID, GPU handle, atlas
 * size, and normalised UV region via UVHandle. Handed out at runtime; never cloned.
 */
public class TextureHandle extends HandlePackage {

    // Internal
    private int tileID;
    private int arrayID;
    private int gpuHandle;
    private int atlasSize;
    private UVHandle uvHandle;

    // Internal \\

    void constructor(int tileID, int arrayID, int gpuHandle, int atlasSize, UVHandle uvHandle) {
        this.tileID = tileID;
        this.arrayID = arrayID;
        this.gpuHandle = gpuHandle;
        this.atlasSize = atlasSize;
        this.uvHandle = uvHandle;
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

    public UVHandle getUVHandle() {
        return uvHandle;
    }
}