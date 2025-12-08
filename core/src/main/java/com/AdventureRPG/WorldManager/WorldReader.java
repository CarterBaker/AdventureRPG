package com.AdventureRPG.worldmanager;

import com.AdventureRPG.core.ScenePipeline.WorldEngineSystem.WorldEngineSystem;
import com.AdventureRPG.core.kernel.EngineSetting;
import com.AdventureRPG.core.kernel.SystemFrame;
import com.AdventureRPG.core.util.Methematics.Extras.Coordinate2Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

public class WorldReader extends SystemFrame {

    // Root
    private WorldEngineSystem worldEngineSystem;
    private Pixmap world;

    // Base \\

    @Override
    protected void create() {

        // Root
        this.world = new Pixmap(Gdx.files.internal(EngineSetting.REGION_IMAGE_PATH));
    }

    public void init() {

        // Root
        worldEngineSystem = gameEngine.get(WorldEngineSystem.class);
        world = gameEngine.get(WorldEngineSystem.class).getWorld();
    }

    // World Reader \\

    public WorldRegion worldRegionFromPosition(long position) {

        // TODO: This needs to be verified working correctly
        position = worldEngineSystem.wrapAroundImageRegion(position);

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
