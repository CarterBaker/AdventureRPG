package application.bootstrap.geometrypipeline.subvoxelmanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;

class SubVoxelRaycastUtility extends EngineUtility {

    /*
     * Walks a block-space ray through a model's sub-voxel grid, one block or
     * many, cell by cell, checking each plane it crosses for a wall. The first
     * filled cell or wall is the target and the cell before it the placement;
     * a ray leaving through the floor places onto the floor cell it left from.
     * A new wall covers the placement cell's struck face, or stands on that
     * face's nearest edge when the ray lands within SUB_VOXEL_WALL_EDGE_SNAP
     * of it.
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
        float[] size = { model.getSizeX(), model.getSizeY(), model.getSizeZ() };
        float[] rayDirection = { direction.x, direction.y, direction.z };

        float tEnter = 0f;
        float tExit = Float.MAX_VALUE;
        int entryAxis = EngineSetting.INDEX_NOT_FOUND;

        for (int axis = 0; axis < 3; axis++) {

            if (rayDirection[axis] == 0f) {

                if (rayOrigin[axis] < 0f || rayOrigin[axis] > size[axis])
                    return false;

                continue;
            }

            float t0 = (0f - rayOrigin[axis]) / rayDirection[axis];
            float t1 = (size[axis] - rayOrigin[axis]) / rayDirection[axis];

            if (Math.min(t0, t1) > tEnter) {
                tEnter = Math.min(t0, t1);
                entryAxis = axis;
            }

            tExit = Math.min(tExit, Math.max(t0, t1));
        }

        if (tExit < tEnter)
            return false;

        return traverse(model, rayOrigin, rayDirection, tEnter, entryAxis, hit);
    }

    // Traversal \\

    private static boolean traverse(
            SubVoxelModelStruct model,
            float[] rayOrigin,
            float[] rayDirection,
            float tStart,
            int entryAxis,
            SubVoxelHitStruct hit) {

        int[] size = { model.getSizeX(), model.getSizeY(), model.getSizeZ() };
        int[] cell = new int[3];
        int[] step = new int[3];
        float[] tMax = new float[3];
        float[] tDelta = new float[3];

        for (int axis = 0; axis < 3; axis++) {

            float entry = rayOrigin[axis] + rayDirection[axis] * tStart;
            cell[axis] = Math.max(0, Math.min(size[axis] - 1, (int) Math.floor(entry)));

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

        if (entryAxis != EngineSetting.INDEX_NOT_FOUND
                && hasWallOnPlane(model, cell, entryAxis, step[entryAxis] > 0 ? 0 : size[entryAxis], hit))
            return true;

        int[] previous = { EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND,
                EngineSetting.INDEX_NOT_FOUND };
        int struckAxis = entryAxis;
        float tEntered = tStart;

        while (true) {

            if (model.isFilled(cell[0], cell[1], cell[2])) {

                hit.setTarget(cell[0], cell[1], cell[2]);

                if (model.isInside(previous[0], previous[1], previous[2]))
                    place(hit, previous, struckAxis, step, rayOrigin, rayDirection, tEntered);

                return true;
            }

            int axis = resolveNextAxis(tMax);
            float tCross = tMax[axis];
            int plane = step[axis] > 0 ? cell[axis] + 1 : cell[axis];

            if (hasWallOnPlane(model, cell, axis, plane, hit)) {
                place(hit, cell, axis, step, rayOrigin, rayDirection, tCross);
                return true;
            }

            previous[0] = cell[0];
            previous[1] = cell[1];
            previous[2] = cell[2];

            cell[axis] += step[axis];
            tMax[axis] += tDelta[axis];
            struckAxis = axis;
            tEntered = tCross;

            if (cell[axis] >= 0 && cell[axis] < size[axis])
                continue;

            if (axis == 1 && step[axis] < 0) {
                place(hit, previous, axis, step, rayOrigin, rayDirection, tCross);
                return true;
            }

            return false;
        }
    }

    private static boolean hasWallOnPlane(
            SubVoxelModelStruct model,
            int[] cell,
            int axis,
            int plane,
            SubVoxelHitStruct hit) {

        int x = axis == 0 ? plane : cell[0];
        int y = axis == 1 ? plane : cell[1];
        int z = axis == 2 ? plane : cell[2];

        if (!model.hasWall(axis, x, y, z))
            return false;

        hit.setWallTarget(axis, x, y, z);
        return true;
    }

    // Placement \\

    // The placement cell, and the wall covering the face the ray struck or standing on its nearest edge
    private static void place(
            SubVoxelHitStruct hit,
            int[] cell,
            int struckAxis,
            int[] step,
            float[] rayOrigin,
            float[] rayDirection,
            float t) {

        hit.setPlacement(cell[0], cell[1], cell[2]);

        if (struckAxis == EngineSetting.INDEX_NOT_FOUND)
            return;

        int uAxis = (struckAxis + 1) % 3;
        int vAxis = (struckAxis + 2) % 3;
        float u = clampUnit(rayOrigin[uAxis] + rayDirection[uAxis] * t - cell[uAxis]);
        float v = clampUnit(rayOrigin[vAxis] + rayDirection[vAxis] * t - cell[vAxis]);
        float nearest = Math.min(Math.min(u, 1f - u), Math.min(v, 1f - v));

        int wallAxis = struckAxis;
        int plane = step[struckAxis] > 0 ? cell[struckAxis] + 1 : cell[struckAxis];

        if (nearest < EngineSetting.SUB_VOXEL_WALL_EDGE_SNAP) {

            boolean alongU = Math.min(u, 1f - u) <= Math.min(v, 1f - v);
            float edge = alongU ? u : v;

            wallAxis = alongU ? uAxis : vAxis;
            plane = cell[wallAxis] + (edge < 0.5f ? 0 : 1);
        }

        hit.setWallPlacement(
                wallAxis,
                wallAxis == 0 ? plane : cell[0],
                wallAxis == 1 ? plane : cell[1],
                wallAxis == 2 ? plane : cell[2]);
    }

    // Utility \\

    private static float clampUnit(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static int resolveNextAxis(float[] tMax) {

        if (tMax[0] < tMax[1])
            return tMax[0] < tMax[2] ? 0 : 2;

        return tMax[1] < tMax[2] ? 1 : 2;
    }
}
