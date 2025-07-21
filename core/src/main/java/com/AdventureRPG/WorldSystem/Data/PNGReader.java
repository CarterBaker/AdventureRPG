package com.AdventureRPG.WorldSystem.Data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;

import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.SettingsSystem.Settings;

public class PNGReader {

    public Settings settings;

    public PNGReader(Settings settings) {
        this.settings = settings;
    }

    public Vector2Int GetWorldScale() {
        FileHandle imageFile = Gdx.files.internal(settings.REGION_IMAGE_PATH);
        Pixmap pixmap = new Pixmap(imageFile);

        int width = pixmap.getWidth();
        int height = pixmap.getHeight();

        int chunksPerPixel = settings.CHUNKS_PER_PIXEL;
        int chunkSize = settings.CHUNK_SIZE;

        int worldWidth = width * chunksPerPixel * chunkSize;
        int worldHeight = height * chunksPerPixel * chunkSize;

        pixmap.dispose();

        Vector2Int worldScale = new Vector2Int(worldWidth, worldHeight);

        if (settings.debug)
            System.out.println("World Scale: " + worldScale);

        return worldScale;
    }
}
