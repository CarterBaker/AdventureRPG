package application.bootstrap.physicspipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;

public class RayBoxUtility extends EngineUtility {

    /*
     * The one ray-against-box test: how far along a ray it enters an
     * axis-aligned box, zero from inside, Float.MAX_VALUE on a miss. World
     * item picking and combat targeting both measure through here.
     */

    // Intersect \\

    public static float intersect(
            Vector3 origin,
            Vector3 direction,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {

        float near = 0f;
        float far = Float.MAX_VALUE;

        float[] origins = { origin.x, origin.y, origin.z };
        float[] directions = { direction.x, direction.y, direction.z };
        float[] mins = { minX, minY, minZ };
        float[] maxs = { maxX, maxY, maxZ };

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            if (directions[axis] == 0f) {

                if (origins[axis] < mins[axis] || origins[axis] > maxs[axis])
                    return Float.MAX_VALUE;

                continue;
            }

            float first = (mins[axis] - origins[axis]) / directions[axis];
            float second = (maxs[axis] - origins[axis]) / directions[axis];

            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
        }

        return near <= far ? near : Float.MAX_VALUE;
    }
}
