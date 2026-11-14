package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.structure.StructureHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public final class StructurePlacementUtility extends EngineUtility {

    /*
     * Pure math behind structure placement: which placement cells can reach
     * a block range on the wrapping world, where a cell's anchor lands, and
     * how offsets, directions, block orientations and sub-block masks carry
     * through clockwise quarter turns, and which turn faces a direction one
     * way, and which column of a turned structure's footprint a point lands
     * in. Every result is a pure function of (seed, structure, cell), so
     * every chunk a structure touches agrees on it independently.
     */

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

    public static float rollCell(long seed, int nameSeed, int cellX, int cellZ, long salt) {
        return BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                seed ^ EngineSetting.STRUCTURE_PLACEMENT_SEED ^ salt
                        ^ (nameSeed * EngineSetting.STRUCTURE_NAME_SEED_MULTIPLIER),
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

        Direction3Vector rotatedFacing = rotateDirection(facing, quarterTurns);

        return encodeOrientation(rotatedFacing, spin);
    }

    // Directions \\

    public static Direction3Vector rotateDirection(Direction3Vector direction, int quarterTurns) {
        return Direction3Vector.getDirection(
                rotateX(direction.x, direction.z, quarterTurns),
                direction.y,
                rotateZ(direction.x, direction.z, quarterTurns));
    }

    // The quarter turns that bring a horizontal direction closest onto a heading
    public static int resolveQuarterTurns(Direction3Vector direction, double headingX, double headingZ) {

        int best = 0;
        double bestAlignment = -Double.MAX_VALUE;

        for (int quarterTurns = 0; quarterTurns < EngineSetting.STRUCTURE_QUARTER_TURN_COUNT; quarterTurns++) {

            Direction3Vector turned = rotateDirection(direction, quarterTurns);
            double alignment = turned.x * headingX + turned.z * headingZ;

            if (alignment <= bestAlignment)
                continue;

            bestAlignment = alignment;
            best = quarterTurns;
        }

        return best;
    }

    // Sub-Block Masks \\

    // Each octant's centre turned about the cell's vertical axis, as the cell itself turns
    public static int rotateMask(int mask, int quarterTurns) {

        if (!SubBlockUtility.isSubdivided(mask)
                || Math.floorMod(quarterTurns, EngineSetting.STRUCTURE_QUARTER_TURN_COUNT) == 0)
            return mask;

        int rotated = SubBlockUtility.MASK_EMPTY;

        for (int octant = 0; octant < SubBlockUtility.OCTANT_COUNT; octant++) {

            if (!SubBlockUtility.hasOctant(mask, octant))
                continue;

            int centreX = 2 * SubBlockUtility.getOctantX(octant) - 1;
            int centreZ = 2 * SubBlockUtility.getOctantZ(octant) - 1;

            rotated |= SubBlockUtility.getOctantBit(SubBlockUtility.getOctant(
                    (rotateX(centreX, centreZ, quarterTurns) + 1) / 2,
                    SubBlockUtility.getOctantY(octant),
                    (rotateZ(centreX, centreZ, quarterTurns) + 1) / 2));
        }

        return rotated;
    }

    // Footprint \\

    // The footprint column a point lands in, relative to the anchor of a structure turned by quarter turns —
    // INDEX_NOT_FOUND outside its horizontal bounds
    public static int resolveFootprintColumn(
            StructureHandle structureHandle,
            int quarterTurns,
            int relativeX,
            int relativeZ) {

        int offsetX = rotateX(relativeX, relativeZ, -quarterTurns) - structureHandle.getMinOffsetX();
        int offsetZ = rotateZ(relativeX, relativeZ, -quarterTurns) - structureHandle.getMinOffsetZ();
        int width = structureHandle.getMaxOffsetX() - structureHandle.getMinOffsetX() + 1;
        int depth = structureHandle.getMaxOffsetZ() - structureHandle.getMinOffsetZ() + 1;

        if (offsetX < 0 || offsetX >= width || offsetZ < 0 || offsetZ >= depth)
            return EngineSetting.INDEX_NOT_FOUND;

        return offsetZ * width + offsetX;
    }

    // The span a structure turned by quarter turns reaches from its anchor along x, lowest first
    public static int resolveTurnedMinX(StructureHandle structureHandle, int quarterTurns) {
        return Math.min(
                rotateX(structureHandle.getMinOffsetX(), structureHandle.getMinOffsetZ(), quarterTurns),
                rotateX(structureHandle.getMaxOffsetX(), structureHandle.getMaxOffsetZ(), quarterTurns));
    }

    public static int resolveTurnedMaxX(StructureHandle structureHandle, int quarterTurns) {
        return Math.max(
                rotateX(structureHandle.getMinOffsetX(), structureHandle.getMinOffsetZ(), quarterTurns),
                rotateX(structureHandle.getMaxOffsetX(), structureHandle.getMaxOffsetZ(), quarterTurns));
    }

    public static int resolveTurnedMinZ(StructureHandle structureHandle, int quarterTurns) {
        return Math.min(
                rotateZ(structureHandle.getMinOffsetX(), structureHandle.getMinOffsetZ(), quarterTurns),
                rotateZ(structureHandle.getMaxOffsetX(), structureHandle.getMaxOffsetZ(), quarterTurns));
    }

    public static int resolveTurnedMaxZ(StructureHandle structureHandle, int quarterTurns) {
        return Math.max(
                rotateZ(structureHandle.getMinOffsetX(), structureHandle.getMinOffsetZ(), quarterTurns),
                rotateZ(structureHandle.getMaxOffsetX(), structureHandle.getMaxOffsetZ(), quarterTurns));
    }
}
