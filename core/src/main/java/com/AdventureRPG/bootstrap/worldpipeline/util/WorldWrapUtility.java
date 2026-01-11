package com.AdventureRPG.bootstrap.worldpipeline.util;

import com.AdventureRPG.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.AdventureRPG.core.engine.UtilityPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.util.mathematics.Extras.Coordinate2Int;
import com.AdventureRPG.core.util.mathematics.vectors.Vector2Int;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;

public class WorldWrapUtility extends UtilityPackage {

    public static Vector3 wrapAroundChunk(Vector3 input) {
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

    public static long wrapAroundWorld(WorldHandle worldHandle, long input) {
        Vector2Int worldScale = worldHandle.getWorldScale();

        int maxX = worldScale.x / EngineSetting.CHUNK_SIZE;
        int maxY = worldScale.y / EngineSetting.CHUNK_SIZE;

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

    public static Vector2Int wrapAroundWorld(WorldHandle worldHandle, Vector2Int input) {
        Vector2Int worldScale = worldHandle.getWorldScale();

        int maxX = worldScale.x / EngineSetting.CHUNK_SIZE;
        int maxY = worldScale.y / EngineSetting.CHUNK_SIZE;

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

    public static Vector3 wrapAroundGrid(int maxRenderDistance, Vector3 input) {
        float maxX = maxRenderDistance * EngineSetting.CHUNK_SIZE;
        float maxZ = maxRenderDistance * EngineSetting.CHUNK_SIZE;

        input.x = ((input.x + maxX / 2) % maxX + maxX) % maxX - maxX / 2;
        input.z = ((input.z + maxZ / 2) % maxZ + maxZ) % maxZ - maxZ / 2;

        return input;
    }

    public static long wrapAroundImageRegion(WorldHandle worldHandle, long input) {
        Vector2Int worldScale = worldHandle.getWorldScale();

        int maxX = (worldScale.x / EngineSetting.CHUNKS_PER_PIXEL / EngineSetting.CHUNK_SIZE);
        int maxY = (worldScale.y / EngineSetting.CHUNKS_PER_PIXEL / EngineSetting.CHUNK_SIZE);

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
}