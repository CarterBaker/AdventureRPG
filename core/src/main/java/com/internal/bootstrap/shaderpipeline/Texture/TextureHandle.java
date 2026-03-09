package com.internal.bootstrap.shaderpipeline.Texture;

import com.internal.core.engine.HandlePackage;

/*
 * Persistent resource owned by TextureManager. Carries all data needed for a
 * single named texture tile: its tile ID, parent array ID, GPU handle, atlas
 * pixel size, individual tile pixel dimensions, and normalised UV region via
 * UVHandle. Handed out at runtime; never cloned.
 */
public class TextureHandle extends HandlePackage {

    // Internal
    private int tileID;
    private int arrayID;
    private int gpuHandle;
    private int atlasPixelSize;
    private int tileWidth;
    private int tileHeight;
    private UVHandle uvHandle;

    // Internal \\

    public void constructor(
            int tileID, int arrayID, int gpuHandle,
            int atlasPixelSize, int tileWidth, int tileHeight,
            UVHandle uvHandle) {
        this.tileID = tileID;
        this.arrayID = arrayID;
        this.gpuHandle = gpuHandle;
        this.atlasPixelSize = atlasPixelSize;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
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

    public int getAtlasPixelSize() {
        return atlasPixelSize;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public UVHandle getUVHandle() {
        return uvHandle;
    }
}