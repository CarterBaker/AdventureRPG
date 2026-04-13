package program.bootstrap.worldpipeline.util;

import program.bootstrap.worldpipeline.world.WorldHandle;
import program.core.engine.EngineUtility;
import program.core.settings.EngineSetting;
import program.core.util.mathematics.extras.Coordinate2Long;
import program.core.util.mathematics.vectors.Vector2Int;
import program.core.util.mathematics.vectors.Vector3;

public class WorldWrapUtility extends EngineUtility {

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
        int maxX = worldScale.x;
        int maxY = worldScale.y;

        int inputX = Coordinate2Long.unpackX(input);
        int inputY = Coordinate2Long.unpackY(input);

        int x = inputX % maxX;
        if (x < 0)
            x += maxX;

        int y = inputY % maxY;
        if (y < 0)
            y += maxY;

        return Coordinate2Long.pack(x, y);
    }
}