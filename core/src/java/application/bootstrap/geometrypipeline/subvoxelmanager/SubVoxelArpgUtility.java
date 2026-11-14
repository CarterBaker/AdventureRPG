package application.bootstrap.geometrypipeline.subvoxelmanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class SubVoxelArpgUtility extends EngineUtility {

    /*
     * The single definition of the sub-voxel mesh format: a mesh ARPG file whose
     * "subvoxels" block holds a resolution, its "size" in blocks when it spans
     * more than one, and named, textured parts, each listing its cubes as
     * inclusive "from"/"to" boxes of cells and, optionally, its double-sided
     * walls as boxes lying on one plane, grouped by the axis they face along.
     * Every model is written with its cubes and
     * walls merged greedily into as few boxes as cover them exactly, so a file
     * holds its shape rather than every cell of it. MeshBuilder parses through
     * here and editor tools read and write through here.
     */

    // Detection \\

    static boolean hasSubVoxels(ArpgObjectStruct meshArpg) {
        return ArpgUtility.hasObject(meshArpg, "subvoxels");
    }

    // Parse \\

    static SubVoxelModelStruct parse(ArpgObjectStruct meshArpg) {

        ArpgObjectStruct subVoxelArpg = ArpgUtility.validateObject(meshArpg, "subvoxels");
        int resolution = ArpgUtility.validateInt(subVoxelArpg, "resolution");

        if (resolution != EngineSetting.SUB_VOXEL_RESOLUTION)
            throwException("Sub-voxel resolution " + resolution + " does not match the engine resolution "
                    + EngineSetting.SUB_VOXEL_RESOLUTION + ".");

        ArpgArrayStruct partsArpg = ArpgUtility.validateArray(subVoxelArpg, "parts");
        SubVoxelModelStruct model = parseSize(subVoxelArpg);

        for (int i = 0; i < partsArpg.size(); i++)
            parsePart(model, partsArpg.get(i).getAsObject());

        return model;
    }

    // A model of the blocks its "size" gives on x, y and z, or one block when it gives none
    private static SubVoxelModelStruct parseSize(ArpgObjectStruct subVoxelArpg) {

        if (!ArpgUtility.hasArray(subVoxelArpg, "size"))
            return new SubVoxelModelStruct();

        ArpgArrayStruct sizeArpg = ArpgUtility.validateArray(subVoxelArpg, "size", EngineSetting.AXIS_COUNT);

        return new SubVoxelModelStruct(
                sizeArpg.get(EngineSetting.AXIS_X).getAsInt(),
                sizeArpg.get(EngineSetting.AXIS_Y).getAsInt(),
                sizeArpg.get(EngineSetting.AXIS_Z).getAsInt());
    }

    private static void parsePart(SubVoxelModelStruct model, ArpgObjectStruct partArpg) {

        String partName = ArpgUtility.validateString(partArpg, "name");
        String textureName = ArpgUtility.validateString(partArpg, "texture");
        ArpgArrayStruct boxesArpg = ArpgUtility.validateArray(partArpg, "boxes");
        int partIndex = model.addPart(new SubVoxelPartStruct(partName, textureName));
        int[] min = new int[EngineSetting.AXIS_COUNT];
        int[] max = new int[EngineSetting.AXIS_COUNT];

        for (int i = 0; i < boxesArpg.size(); i++) {

            parseBox(boxesArpg.get(i).getAsObject(), min, max);

            for (int z = min[EngineSetting.AXIS_Z]; z <= max[EngineSetting.AXIS_Z]; z++)
                for (int y = min[EngineSetting.AXIS_Y]; y <= max[EngineSetting.AXIS_Y]; y++)
                    for (int x = min[EngineSetting.AXIS_X]; x <= max[EngineSetting.AXIS_X]; x++) {

                        if (!model.isInside(x, y, z))
                            throwException("Sub-voxel cell (" + x + ", " + y + ", " + z + ") in part '" + partName
                                    + "' lies outside the model grid.");

                        if (model.isFilled(x, y, z))
                            throwException("Sub-voxel cell (" + x + ", " + y + ", " + z + ") in part '"
                                    + partName + "' is already owned by another cube.");

                        model.setCell(x, y, z, partIndex);
                    }
        }

        if (ArpgUtility.hasObject(partArpg, "walls"))
            parseWalls(model, partArpg.getAsObject("walls"), partName, partIndex);
    }

    private static void parseWalls(
            SubVoxelModelStruct model,
            ArpgObjectStruct wallsArpg,
            String partName,
            int partIndex) {

        int[] min = new int[EngineSetting.AXIS_COUNT];
        int[] max = new int[EngineSetting.AXIS_COUNT];

        for (int axis = 0; axis < EngineSetting.SUB_VOXEL_AXIS_COUNT; axis++) {

            String axisKey = EngineSetting.SUB_VOXEL_WALL_AXIS_KEYS[axis];

            if (!wallsArpg.has(axisKey))
                continue;

            ArpgArrayStruct axisArpg = ArpgUtility.validateArray(wallsArpg, axisKey);

            for (int i = 0; i < axisArpg.size(); i++) {

                parseBox(axisArpg.get(i).getAsObject(), min, max);

                if (min[axis] != max[axis])
                    throwException("Sub-voxel walls facing " + axisKey + " in part '" + partName
                            + "' must lie on one plane, but span " + min[axis] + " to " + max[axis] + ".");

                for (int z = min[EngineSetting.AXIS_Z]; z <= max[EngineSetting.AXIS_Z]; z++)
                    for (int y = min[EngineSetting.AXIS_Y]; y <= max[EngineSetting.AXIS_Y]; y++)
                        for (int x = min[EngineSetting.AXIS_X]; x <= max[EngineSetting.AXIS_X]; x++)
                            addWall(model, axis, x, y, z, partName, partIndex);
            }
        }
    }

    private static void addWall(
            SubVoxelModelStruct model,
            int axis,
            int x,
            int y,
            int z,
            String partName,
            int partIndex) {

        String axisKey = EngineSetting.SUB_VOXEL_WALL_AXIS_KEYS[axis];

        if (!model.isWallInside(axis, x, y, z))
            throwException("Sub-voxel wall (" + x + ", " + y + ", " + z + ") facing " + axisKey
                    + " in part '" + partName + "' lies outside the model grid.");

        if (model.hasWall(axis, x, y, z))
            throwException("Sub-voxel wall (" + x + ", " + y + ", " + z + ") facing " + axisKey
                    + " in part '" + partName + "' is already owned by another wall.");

        model.setWall(axis, x, y, z, partIndex);
    }

    // An inclusive "from"/"to" box, its corners sorted into the minimum and maximum given
    private static void parseBox(ArpgObjectStruct boxArpg, int[] min, int[] max) {

        ArpgArrayStruct from = ArpgUtility.validateArray(boxArpg, "from", EngineSetting.AXIS_COUNT);
        ArpgArrayStruct to = ArpgUtility.validateArray(boxArpg, "to", EngineSetting.AXIS_COUNT);

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            int first = from.get(axis).getAsInt();
            int second = to.get(axis).getAsInt();

            min[axis] = Math.min(first, second);
            max[axis] = Math.max(first, second);
        }
    }

    // Serialize \\

    static ArpgObjectStruct toMeshArpg(SubVoxelModelStruct model) {

        ArpgObjectStruct meshArpg = new ArpgObjectStruct();
        meshArpg.addProperty("vao", EngineSetting.SUB_VOXEL_VAO);
        meshArpg.add("subvoxels", toSubVoxelArpg(model));
        return meshArpg;
    }

    private static ArpgObjectStruct toSubVoxelArpg(SubVoxelModelStruct model) {

        ArpgArrayStruct partsArpg = new ArpgArrayStruct();

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            partsArpg.add(toPartArpg(model, partIndex));

        ArpgObjectStruct subVoxelArpg = new ArpgObjectStruct();
        subVoxelArpg.addProperty("resolution", EngineSetting.SUB_VOXEL_RESOLUTION);

        if (!model.isSingleBlock()) {

            ArpgArrayStruct sizeArpg = new ArpgArrayStruct();

            for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
                sizeArpg.add(model.getBlocks(axis));

            subVoxelArpg.add("size", sizeArpg);
        }

        subVoxelArpg.add("parts", partsArpg);
        return subVoxelArpg;
    }

    private static ArpgObjectStruct toPartArpg(SubVoxelModelStruct model, int partIndex) {

        int[] size = { model.getSizeX(), model.getSizeY(), model.getSizeZ() };
        boolean[] members = new boolean[size[0] * size[1] * size[2]];

        for (int z = 0; z < size[EngineSetting.AXIS_Z]; z++)
            for (int y = 0; y < size[EngineSetting.AXIS_Y]; y++)
                for (int x = 0; x < size[EngineSetting.AXIS_X]; x++)
                    members[toIndex(size, x, y, z)] = model.getCellPart(x, y, z) == partIndex;

        SubVoxelPartStruct part = model.getPart(partIndex);
        ArpgObjectStruct partArpg = new ArpgObjectStruct();
        partArpg.addProperty("name", part.getPartName());
        partArpg.addProperty("texture", part.getTextureName());
        partArpg.add("boxes", toBoxesArpg(mergeBoxes(members, size, EngineSetting.INDEX_NOT_FOUND)));

        ArpgObjectStruct wallsArpg = toWallsArpg(model, partIndex);

        if (!wallsArpg.isEmpty())
            partArpg.add("walls", wallsArpg);

        return partArpg;
    }

    private static ArpgObjectStruct toWallsArpg(SubVoxelModelStruct model, int partIndex) {

        ArpgObjectStruct wallsArpg = new ArpgObjectStruct();

        for (int axis = 0; axis < EngineSetting.SUB_VOXEL_AXIS_COUNT; axis++) {

            int[] size = { model.getSizeX(), model.getSizeY(), model.getSizeZ() };
            size[axis]++;
            boolean[] members = new boolean[size[0] * size[1] * size[2]];
            boolean any = false;

            for (int z = 0; z < size[EngineSetting.AXIS_Z]; z++)
                for (int y = 0; y < size[EngineSetting.AXIS_Y]; y++)
                    for (int x = 0; x < size[EngineSetting.AXIS_X]; x++) {

                        boolean member = model.getWallPart(axis, x, y, z) == partIndex;

                        members[toIndex(size, x, y, z)] = member;
                        any |= member;
                    }

            if (any)
                wallsArpg.add(EngineSetting.SUB_VOXEL_WALL_AXIS_KEYS[axis],
                        toBoxesArpg(mergeBoxes(members, size, axis)));
        }

        return wallsArpg;
    }

    private static ArpgArrayStruct toBoxesArpg(IntArrayList boxes) {

        ArpgArrayStruct boxesArpg = new ArpgArrayStruct();

        for (int box = 0; box < boxes.size(); box += EngineSetting.BOX_INT_STRIDE) {

            ArpgArrayStruct from = new ArpgArrayStruct();
            ArpgArrayStruct to = new ArpgArrayStruct();

            for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {
                from.add(boxes.getInt(box + EngineSetting.BOX_MIN_X + axis));
                to.add(boxes.getInt(box + EngineSetting.BOX_MAX_X + axis));
            }

            ArpgObjectStruct boxArpg = new ArpgObjectStruct();
            boxArpg.add("from", from);
            boxArpg.add("to", to);
            boxesArpg.add(boxArpg);
        }

        return boxesArpg;
    }

    // Merge \\

    // Every member covered by as few boxes as a greedy sweep finds, each grown along x, then y, then z, and written
    // as an inclusive minimum and maximum, six ints a box; the locked axis never grows, so walls keep to their plane
    private static IntArrayList mergeBoxes(boolean[] members, int[] size, int lockedAxis) {

        IntArrayList boxes = new IntArrayList();
        boolean[] covered = new boolean[members.length];
        int[] min = new int[EngineSetting.AXIS_COUNT];
        int[] max = new int[EngineSetting.AXIS_COUNT];

        for (int z = 0; z < size[EngineSetting.AXIS_Z]; z++)
            for (int y = 0; y < size[EngineSetting.AXIS_Y]; y++)
                for (int x = 0; x < size[EngineSetting.AXIS_X]; x++) {

                    int index = toIndex(size, x, y, z);

                    if (!members[index] || covered[index])
                        continue;

                    min[EngineSetting.AXIS_X] = x;
                    min[EngineSetting.AXIS_Y] = y;
                    min[EngineSetting.AXIS_Z] = z;
                    max[EngineSetting.AXIS_X] = x;
                    max[EngineSetting.AXIS_Y] = y;
                    max[EngineSetting.AXIS_Z] = z;

                    for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
                        while (axis != lockedAxis && canGrow(members, covered, size, min, max, axis))
                            max[axis]++;

                    cover(covered, size, min, max);

                    for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
                        boxes.add(min[axis]);

                    for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
                        boxes.add(max[axis]);
                }

        return boxes;
    }

    // True when the whole layer just past the box along the axis is made of members not yet covered
    private static boolean canGrow(
            boolean[] members,
            boolean[] covered,
            int[] size,
            int[] min,
            int[] max,
            int axis) {

        int layer = max[axis] + 1;

        if (layer >= size[axis])
            return false;

        int[] cell = new int[EngineSetting.AXIS_COUNT];

        for (int z = min[EngineSetting.AXIS_Z]; z <= max[EngineSetting.AXIS_Z]; z++)
            for (int y = min[EngineSetting.AXIS_Y]; y <= max[EngineSetting.AXIS_Y]; y++)
                for (int x = min[EngineSetting.AXIS_X]; x <= max[EngineSetting.AXIS_X]; x++) {

                    cell[EngineSetting.AXIS_X] = x;
                    cell[EngineSetting.AXIS_Y] = y;
                    cell[EngineSetting.AXIS_Z] = z;
                    cell[axis] = layer;

                    int index = toIndex(size, cell[0], cell[1], cell[2]);

                    if (!members[index] || covered[index])
                        return false;
                }

        return true;
    }

    private static void cover(boolean[] covered, int[] size, int[] min, int[] max) {

        for (int z = min[EngineSetting.AXIS_Z]; z <= max[EngineSetting.AXIS_Z]; z++)
            for (int y = min[EngineSetting.AXIS_Y]; y <= max[EngineSetting.AXIS_Y]; y++)
                for (int x = min[EngineSetting.AXIS_X]; x <= max[EngineSetting.AXIS_X]; x++)
                    covered[toIndex(size, x, y, z)] = true;
    }

    // Utility \\

    private static int toIndex(int[] size, int x, int y, int z) {
        return x + size[EngineSetting.AXIS_X] * (y + size[EngineSetting.AXIS_Y] * z);
    }
}
