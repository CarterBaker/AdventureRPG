package application.bootstrap.geometrypipeline.subvoxelmanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;

class SubVoxelRaycastUtility extends EngineUtility {

    /*
     * Walks a block-space ray through a sub-voxel grid cell by cell. The first
     * filled cell is the target and the cell before it the placement; a ray
     * leaving through the floor places onto the floor cell it left from.
     */

    // Raycast \\

    static boolean raycast(
            SubVoxelModelStruct model,
            Vector3 origin,
            Vector3 direction,
            SubVoxelHitStruct hit) {

        hit.clear();

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        float[] rayOrigin = { origin.x * resolution, origin.y * resolution, origin.z * resolution };
        float[] rayDirection = { direction.x, direction.y, direction.z };

        float tEnter = 0f;
        float tExit = Float.MAX_VALUE;

        for (int axis = 0; axis < 3; axis++) {

            if (rayDirection[axis] == 0f) {

                if (rayOrigin[axis] < 0f || rayOrigin[axis] > resolution)
                    return false;

                continue;
            }

            float t0 = (0f - rayOrigin[axis]) / rayDirection[axis];
            float t1 = (resolution - rayOrigin[axis]) / rayDirection[axis];

            tEnter = Math.max(tEnter, Math.min(t0, t1));
            tExit = Math.min(tExit, Math.max(t0, t1));
        }

        if (tExit < tEnter)
            return false;

        return traverse(model, rayOrigin, rayDirection, tEnter, hit);
    }

    // Traversal \\

    private static boolean traverse(
            SubVoxelModelStruct model,
            float[] rayOrigin,
            float[] rayDirection,
            float tStart,
            SubVoxelHitStruct hit) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int[] cell = new int[3];
        int[] step = new int[3];
        float[] tMax = new float[3];
        float[] tDelta = new float[3];

        for (int axis = 0; axis < 3; axis++) {

            float entry = rayOrigin[axis] + rayDirection[axis] * tStart;
            cell[axis] = Math.max(0, Math.min(resolution - 1, (int) Math.floor(entry)));

            if (rayDirection[axis] > 0f) {
                step[axis] = 1;
                tMax[axis] = (cell[axis] + 1 - rayOrigin[axis]) / rayDirection[axis];
                tDelta[axis] = 1f / rayDirection[axis];
            } else if (rayDirection[axis] < 0f) {
                step[axis] = -1;
                tMax[axis] = (cell[axis] - rayOrigin[axis]) / rayDirection[axis];
                tDelta[axis] = -1f / rayDirection[axis];
            } else {
                step[axis] = 0;
                tMax[axis] = Float.MAX_VALUE;
                tDelta[axis] = Float.MAX_VALUE;
            }
        }

        int previousX = EngineSetting.INDEX_NOT_FOUND;
        int previousY = EngineSetting.INDEX_NOT_FOUND;
        int previousZ = EngineSetting.INDEX_NOT_FOUND;

        while (true) {

            if (model.isFilled(cell[0], cell[1], cell[2])) {

                hit.setTarget(cell[0], cell[1], cell[2]);

                if (model.isInside(previousX, previousY, previousZ))
                    hit.setPlacement(previousX, previousY, previousZ);

                return true;
            }

            int axis = resolveNextAxis(tMax);

            previousX = cell[0];
            previousY = cell[1];
            previousZ = cell[2];

            cell[axis] += step[axis];
            tMax[axis] += tDelta[axis];

            if (cell[axis] >= 0 && cell[axis] < resolution)
                continue;

            if (axis == 1 && step[axis] < 0) {
                hit.setPlacement(previousX, previousY, previousZ);
                return true;
            }

            return false;
        }
    }

    private static int resolveNextAxis(float[] tMax) {

        if (tMax[0] < tMax[1])
            return tMax[0] < tMax[2] ? 0 : 2;

        return tMax[1] < tMax[2] ? 1 : 2;
    }
}
