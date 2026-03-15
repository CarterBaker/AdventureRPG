package com.internal.bootstrap.shaderpipeline.texture;

import com.internal.core.engine.HandlePackage;

public class TextureHandle extends HandlePackage {

    /*
     * Persistent texture tile record. Wraps TextureData and delegates all
     * access through it. Registered in TextureManager from bootstrap to shutdown.
     * UV coordinates are accessed directly — no separate UV object.
     */

    // Internal
    private TextureData textureData;

    // Internal \\

    public void constructor(TextureData textureData) {
        this.textureData = textureData;
    }

    // Accessible \\

    public TextureData getTextureData() {
        return textureData;
    }

    public String getTileName() {
        return textureData.getTileName();
    }

    public int getTileID() {
        return textureData.getTileID();
    }

    public int getArrayID() {
        return textureData.getArrayID();
    }

    public String getArrayName() {
        return textureData.getArrayName();
    }

    public int getGpuHandle() {
        return textureData.getGpuHandle();
    }

    public int getAtlasPixelSize() {
        return textureData.getAtlasPixelSize();
    }

    public int getTileWidth() {
        return textureData.getTileWidth();
    }

    public int getTileHeight() {
        return textureData.getTileHeight();
    }

    public float getU0() {
        return textureData.getU0();
    }

    public float getV0() {
        return textureData.getV0();
    }

    public float getU1() {
        return textureData.getU1();
    }

    public float getV1() {
        return textureData.getV1();
    }
}