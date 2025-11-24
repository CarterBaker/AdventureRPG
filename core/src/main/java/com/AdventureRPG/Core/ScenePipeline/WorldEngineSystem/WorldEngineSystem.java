package com.AdventureRPG.Core.ScenePipeline.WorldEngineSystem;

import com.AdventureRPG.Core.Bootstrap.EngineConstant;
import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.Util.Coordinate2Int;
import com.AdventureRPG.Core.Util.Vector2Int;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.Gdx;

public class WorldEngineSystem extends SystemFrame {

    // Root
    private final Pixmap world;
    private final Vector2Int WORLD_SCALE;

    // Base \\

    public WorldEngineSystem() {

        // Root
        world = new Pixmap(Gdx.files.internal(EngineConstant.REGION_IMAGE_PATH));
        WORLD_SCALE = getWorldScale(world);
    }

    private Vector2Int getWorldScale(Pixmap pixMap) {

        int width = pixMap.getWidth();
        int height = pixMap.getHeight();

        int chunksPerPixel = EngineConstant.CHUNKS_PER_PIXEL;
        int chunkSize = EngineConstant.CHUNK_SIZE;

        int worldWidth = width * chunksPerPixel * chunkSize;
        int worldHeight = height * chunksPerPixel * chunkSize;

        return new Vector2Int(worldWidth, worldHeight);
    }

    // Wrap Logic \\

    public Vector3 wrapAroundChunk(Vector3 input) {

        float x = input.x % EngineConstant.CHUNK_SIZE;

        if (x < 0)
            x += EngineConstant.CHUNK_SIZE;

        float z = input.z % EngineConstant.CHUNK_SIZE;

        if (z < 0)
            z += EngineConstant.CHUNK_SIZE;

        input.x = x;
        input.z = z;

        return input;
    }

    public long wrapAroundWorld(long input) {

        int maxX = WORLD_SCALE.x / EngineConstant.CHUNK_SIZE;
        int maxY = WORLD_SCALE.y / EngineConstant.CHUNK_SIZE;

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

        int maxX = WORLD_SCALE.x / EngineConstant.CHUNK_SIZE;
        int maxY = WORLD_SCALE.y / EngineConstant.CHUNK_SIZE;

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

        float maxX = settings.maxRenderDistance * EngineConstant.CHUNK_SIZE;
        float maxZ = settings.maxRenderDistance * EngineConstant.CHUNK_SIZE;

        input.x = ((input.x + maxX / 2) % maxX + maxX) % maxX - maxX / 2;
        input.z = ((input.z + maxZ / 2) % maxZ + maxZ) % maxZ - maxZ / 2;

        return input;
    }

    public long wrapAroundImageRegion(long input) {

        int maxX = (WORLD_SCALE.x / EngineConstant.CHUNKS_PER_PIXEL / EngineConstant.CHUNK_SIZE);
        int maxY = (WORLD_SCALE.y / EngineConstant.CHUNKS_PER_PIXEL / EngineConstant.CHUNK_SIZE);

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
