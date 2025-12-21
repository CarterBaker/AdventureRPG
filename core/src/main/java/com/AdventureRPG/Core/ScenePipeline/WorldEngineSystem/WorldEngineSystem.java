package com.AdventureRPG.core.scenepipeline.worldenginesystem;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.AdventureRPG.core.engine.EngineSetting;
import com.AdventureRPG.core.engine.SystemFrame;
import com.AdventureRPG.core.util.Mathematics.Extras.Coordinate2Int;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector2Int;
import com.badlogic.gdx.Gdx;

public class WorldEngineSystem extends SystemFrame {

    // Root
    private final Pixmap world;
    private final Vector2Int WORLD_SCALE;

    // Base \\

    public WorldEngineSystem() {

        // Root
        world = new Pixmap(Gdx.files.internal(EngineSetting.REGION_IMAGE_PATH));
        WORLD_SCALE = getWorldScale(world);
    }

    private Vector2Int getWorldScale(Pixmap pixMap) {

        int width = pixMap.getWidth();
        int height = pixMap.getHeight();

        int chunksPerPixel = EngineSetting.CHUNKS_PER_PIXEL;
        int chunkSize = EngineSetting.CHUNK_SIZE;

        int worldWidth = width * chunksPerPixel * chunkSize;
        int worldHeight = height * chunksPerPixel * chunkSize;

        return new Vector2Int(worldWidth, worldHeight);
    }

    // Wrap Logic \\

    public Vector3 wrapAroundChunk(Vector3 input) {

        float x = input.x % EngineSetting.CHUNK_SIZE;

        if (x < 0)
            x += EngineSetting.CHUNK_SIZE;

        float z = input.z % EngineSetting.CHUNK_SIZE;

        if (z < 0)
            z += EngineSetting.CHUNK_SIZE;

        input.x = x;
        input.z = z;

        return input;
    }

    public long wrapAroundWorld(long input) {

        int maxX = WORLD_SCALE.x / EngineSetting.CHUNK_SIZE;
        int maxY = WORLD_SCALE.y / EngineSetting.CHUNK_SIZE;

        int inputX = Coordinate2Int.unpackX(input);
        int inputY = Coordinate2Int.unpackY(input);

        int x = inputX % maxX;

        if (x < 0)
            x += maxX;

        int y = inputY % maxY;

        if (y < 0)
            y += maxY;

        inputX = x;
        inputY = y;

        return Coordinate2Int.pack(inputX, inputY);
    }

    public Vector2Int wrapAroundWorld(Vector2Int input) {

        int maxX = WORLD_SCALE.x / EngineSetting.CHUNK_SIZE;
        int maxY = WORLD_SCALE.y / EngineSetting.CHUNK_SIZE;

        int x = input.x % maxX;

        if (x < 0)
            x += maxX;

        int y = input.y % maxY;

        if (y < 0)
            y += maxY;

        input.x = x;
        input.y = y;

        return input;
    }

    public Vector3 wrapAroundGrid(Vector3 input) {

        float maxX = settings.maxRenderDistance * EngineSetting.CHUNK_SIZE;
        float maxZ = settings.maxRenderDistance * EngineSetting.CHUNK_SIZE;

        input.x = ((input.x + maxX / 2) % maxX + maxX) % maxX - maxX / 2;
        input.z = ((input.z + maxZ / 2) % maxZ + maxZ) % maxZ - maxZ / 2;

        return input;
    }

    public long wrapAroundImageRegion(long input) {

        int maxX = (WORLD_SCALE.x / EngineSetting.CHUNKS_PER_PIXEL / EngineSetting.CHUNK_SIZE);
        int maxY = (WORLD_SCALE.y / EngineSetting.CHUNKS_PER_PIXEL / EngineSetting.CHUNK_SIZE);

        int inputX = Coordinate2Int.unpackX(input);
        int inputY = Coordinate2Int.unpackY(input);

        int x = (int) Math.floor(inputX) % maxX;

        if (x < 0)
            x += maxX;

        int y = (int) Math.floor(inputY) % maxY;

        if (y < 0)
            y += maxY;

        inputX = x;
        inputY = y;

        return Coordinate2Int.pack(inputX, inputY);
    }

    // Accessibe \\

    public Pixmap getWorld() {
        return world;
    }

    public Vector2Int getWorldScale() {
        return WORLD_SCALE;
    }
}
