package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public final class StructurePlacementUtility extends EngineUtility {

    /*
     * Pure math behind structure placement: which placement cells can reach
     * a block range on the wrapping world, where a cell's anchor lands, and
     * how offsets and block orientations carry through clockwise quarter
     * turns. Every result is a pure function of (seed, structure, cell), so
     * every chunk a structure touches agrees on it independently.
     */

    private StructurePlacementUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Placement Cells \\

    public static void collectCells(
            long rangeMin,
            long rangeMax,
            long worldSizeBlocks,
            int spacingBlocks,
            IntArrayList outCells) {

        outCells.clear();

        int cellCount = (int) (worldSizeBlocks / spacingBlocks);

        if (cellCount <= 0)
            return;

        if (rangeMax - rangeMin + 1 >= worldSizeBlocks) {
            addCellSpan(0, worldSizeBlocks - 1, spacingBlocks, cellCount, outCells);
            return;
        }

        long start = Math.floorMod(rangeMin, worldSizeBlocks);
        long end = start + (rangeMax - rangeMin);

        if (end < worldSizeBlocks) {
            addCellSpan(start, end, spacingBlocks, cellCount, outCells);
            return;
        }

        addCellSpan(start, worldSizeBlocks - 1, spacingBlocks, cellCount, outCells);
        addCellSpan(0, end - worldSizeBlocks, spacingBlocks, cellCount, outCells);
    }

    private static void addCellSpan(
            long start,
            long end,
            int spacingBlocks,
            int cellCount,
            IntArrayList outCells) {

        int firstCell = (int) (start / spacingBlocks);
        int lastCell = (int) Math.min(end / spacingBlocks, cellCount - 1);

        for (int cell = firstCell; cell <= lastCell; cell++)
            outCells.add(cell);
    }

    public static long computeCellAnchor(int cell, int spacingBlocks, int separationBlocks, float roll) {

        int usableBlocks = spacingBlocks - separationBlocks;
        int offset = Math.min((int) (roll * usableBlocks), usableBlocks - 1);

        return (long) cell * spacingBlocks + separationBlocks / 2 + offset;
    }

    // Hashing \\

    public static float rollCell(long seed, short structureID, int cellX, int cellZ, long salt) {
        return BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                seed ^ EngineSetting.STRUCTURE_PLACEMENT_SEED ^ salt
                        ^ (structureID * EngineSetting.STRUCTURE_ID_HASH_MULTIPLIER),
                cellX, cellZ));
    }

    public static int rollQuarterTurns(float roll) {
        return Math.min(
                (int) (roll * EngineSetting.STRUCTURE_QUARTER_TURN_COUNT),
                EngineSetting.STRUCTURE_QUARTER_TURN_COUNT - 1);
    }

    // Rotation \\

    public static int rotateX(int x, int z, int quarterTurns) {
        return switch (Math.floorMod(quarterTurns, EngineSetting.STRUCTURE_QUARTER_TURN_COUNT)) {
            case 1 -> z;
            case 2 -> -x;
            case 3 -> -z;
            default -> x;
        };
    }

    public static int rotateZ(int x, int z, int quarterTurns) {
        return switch (Math.floorMod(quarterTurns, EngineSetting.STRUCTURE_QUARTER_TURN_COUNT)) {
            case 1 -> -x;
            case 2 -> -z;
            case 3 -> x;
            default -> z;
        };
    }

    // Orientation \\

    public static short encodeOrientation(Direction3Vector facing, int spin) {
        return (short) (facing.ordinal() * EngineSetting.STRUCTURE_ORIENTATION_SPIN_COUNT + spin);
    }

    public static short rotateOrientation(short orientation, int quarterTurns) {

        int spinCount = EngineSetting.STRUCTURE_ORIENTATION_SPIN_COUNT;

        Direction3Vector facing = Direction3Vector.VALUES[orientation / spinCount];
        int spin = orientation % spinCount;

        if (facing == Direction3Vector.UP)
            return encodeOrientation(facing, Math.floorMod(spin + quarterTurns, spinCount));

        if (facing == Direction3Vector.DOWN)
            return encodeOrientation(facing, Math.floorMod(spin - quarterTurns, spinCount));

        Direction3Vector rotatedFacing = Direction3Vector.getDirection(
                rotateX(facing.x, facing.z, quarterTurns),
                facing.y,
                rotateZ(facing.x, facing.z, quarterTurns));

        return encodeOrientation(rotatedFacing, spin);
    }
}
