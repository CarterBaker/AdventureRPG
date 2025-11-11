package com.AdventureRPG.WorldManager;

import com.AdventureRPG.Core.Root.SystemFrame;
import com.AdventureRPG.Core.Util.Coordinate2Int;
import com.AdventureRPG.Core.Util.GlobalConstant;
import com.AdventureRPG.Core.Util.Vector2Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

public class WorldReader extends SystemFrame {

    // Root
    private WorldManager worldManager;
    private Pixmap world;

    // Base \\

    @Override
    protected void create() {

        // Root
        this.world = new Pixmap(Gdx.files.internal(GlobalConstant.REGION_IMAGE_PATH));
    }

    public void init() {

        // Root
        worldManager = rootManager.get(WorldManager.class);
    }

    // World Reader \\

    public Vector2Int getWorldScale() {

        int width = world.getWidth();
        int height = world.getHeight();

        int chunksPerPixel = GlobalConstant.CHUNKS_PER_PIXEL;
        int chunkSize = GlobalConstant.CHUNK_SIZE;

        int worldWidth = width * chunksPerPixel * chunkSize;
        int worldHeight = height * chunksPerPixel * chunkSize;

        Vector2Int worldScale = new Vector2Int(worldWidth, worldHeight);

        return worldScale;
    }

    public WorldRegion worldRegionFromPosition(long position) {

        // TODO: This needs to be verified working correctly
        position = worldManager.wrapAroundImageRegion(position);

        int x = Coordinate2Int.unpackX(position);
        int y = Coordinate2Int.unpackY(position);

        // Clamp to image boundaries just in case
        x = Math.min(Math.max(x, 0), world.getWidth() - 1);
        y = Math.min(Math.max(y, 0), world.getHeight() - 1);

        int pixel = world.getPixel(x, y); // returns RGBA8888 packed int

        // Extract channels (RGBA8888 format - 8 bits each)
        int r = (pixel >> 24) & 0xFF;
        int g = (pixel >> 16) & 0xFF;
        int b = (pixel >> 8) & 0xFF;

        return new WorldRegion(r, g, b);
    }
}
