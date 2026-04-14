package engine.util.assets.atlas;

import engine.root.DataPackage;

public abstract class AtlasTileData extends DataPackage {

    /*
     * Abstract base for any bootstrap-only tile container that feeds into
     * AtlasUtility. Carries the tile's pixel dimensions and receives its
     * packed pixel-space atlas position back from the packer. Extended by
     * TextureTileData and FontTileData — AtlasUtility sees only this type,
     * keeping the texture and font pipelines fully decoupled from each other.
     * Must not be held after bootstrap completes.
     */

    // Dimensions
    private int tileWidth;
    private int tileHeight;

    // Atlas Position
    private int atlasX;
    private int atlasY;

    // Dimensions \\

    protected void setTileDimensions(int width, int height) {
        this.tileWidth = width;
        this.tileHeight = height;
    }

    // Atlas Position \\

    public void setAtlasPosition(int x, int y) {
        this.atlasX = x;
        this.atlasY = y;
    }

    // Accessible \\

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getAtlasX() {
        return atlasX;
    }

    public int getAtlasY() {
        return atlasY;
    }
}