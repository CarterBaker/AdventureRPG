package com.AdventureRPG.WorldSystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.SettingsSystem.Settings;

public class WorldReader {

    private final WorldSystem WorldSystem;
    private final Settings settings;
    private final Pixmap world;

    public WorldReader(WorldSystem WorldSystem) {
        this.WorldSystem = WorldSystem;
        this.settings = WorldSystem.settings;
        this.world = new Pixmap(Gdx.files.internal(settings.REGION_IMAGE_PATH));
    }

    public Vector2Int GetWorldScale() {
        int width = world.getWidth();
        int height = world.getHeight();

        int chunksPerPixel = settings.CHUNKS_PER_PIXEL;
        int chunkSize = settings.CHUNK_SIZE;

        int worldWidth = width * chunksPerPixel * chunkSize;
        int worldHeight = height * chunksPerPixel * chunkSize;

        Vector2Int worldScale = new Vector2Int(worldWidth, worldHeight);

        if (settings.debug)
            System.out.println("World Scale: " + worldScale);

        return worldScale;
    }

    public WorldRegion WorldRegionFromPosition(Vector3Int input) {
        Vector2 worldPosition = new Vector2(input.x, input.z);
        Vector2Int imagePixel = WorldSystem.WrapAroundImageRegion(worldPosition);

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
