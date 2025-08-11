package com.AdventureRPG.WorldSystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.SettingsSystem.Settings;

public class WorldReader {

    // Game Manager
    private final WorldSystem worldSystem;
    private final Settings settings;
    private final Pixmap world;

    // Temp
    private final Vector2Int worldPosition;

    // Base \\

    public WorldReader(WorldSystem worldSystem) {

        // Game Manager
        this.worldSystem = worldSystem;
        this.settings = worldSystem.settings;
        this.world = new Pixmap(Gdx.files.internal(settings.REGION_IMAGE_PATH));

        // Temp
        this.worldPosition = new Vector2Int();
    }

    // World Reader \\

    public Vector2Int GetWorldScale() {
        int width = world.getWidth();
        int height = world.getHeight();

        int chunksPerPixel = settings.CHUNKS_PER_PIXEL;
        int chunkSize = settings.CHUNK_SIZE;

        int worldWidth = width * chunksPerPixel * chunkSize;
        int worldHeight = height * chunksPerPixel * chunkSize;

        Vector2Int worldScale = new Vector2Int(worldWidth, worldHeight);

        if (settings.debug)
            System.out.println("World Scale: " + worldScale); // TODO: Remove debug line

        return worldScale;
    }

    public WorldRegion WorldRegionFromPosition(Vector3Int input) {

        worldPosition.set(input.x, input.z);
        Vector2Int imagePixel = worldSystem.WrapAroundImageRegion(worldPosition);

        int x = imagePixel.x;
        int y = imagePixel.y;

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
