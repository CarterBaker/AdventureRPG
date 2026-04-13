package application.bootstrap.shaderpipeline.texture;

import engine.root.DataPackage;

public class TextureData extends DataPackage {

    /*
     * Persistent tile record. Holds tile identity, parent array identity,
     * GPU handle, atlas and tile pixel dimensions, and normalised UV region.
     * Owned by TextureHandle for the full engine session — nothing is discarded.
     */

    // Tile Identity
    private final String tileName;
    private final int tileID;

    // Array Identity
    private final int arrayID;
    private final String arrayName;

    // GPU
    private final int gpuHandle;
    private final int atlasPixelSize;
    private final int tileWidth;
    private final int tileHeight;

    // UV — normalised, bottom-left origin
    private final float u0;
    private final float v0;
    private final float u1;
    private final float v1;

    // Constructor \\

    public TextureData(
            String tileName,
            int tileID,
            int arrayID,
            String arrayName,
            int gpuHandle,
            int atlasPixelSize,
            int tileWidth,
            int tileHeight,
            float u0, float v0,
            float u1, float v1) {

        this.tileName = tileName;
        this.tileID = tileID;
        this.arrayID = arrayID;
        this.arrayName = arrayName;
        this.gpuHandle = gpuHandle;
        this.atlasPixelSize = atlasPixelSize;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
    }

    // Accessible \\

    public String getTileName() {
        return tileName;
    }

    public int getTileID() {
        return tileID;
    }

    public int getArrayID() {
        return arrayID;
    }

    public String getArrayName() {
        return arrayName;
    }

    public int getGpuHandle() {
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