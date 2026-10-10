package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import application.bootstrap.worldpipeline.util.TideUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class CaveCarveBranch extends BranchPackage {

    /*
     * Async — hollows the ground. Cave density is read through a frame: the
     * cave lattice filled over a chunk or down a single column, with the
     * underground lakes reaching it. A point's density is the widest of its
     * tunnels, passages and caverns, held shut within any lake's guard and
     * eased shut through its taper, merged with the lake chambers
     * themselves, then closed toward the floor and roof of its column's band
     * and below the tide where the sea is walled off. carveVolume() resolves
     * a subchunk and a margin around it: lattice cells whose corners prove
     * them solid are skipped whole, a cell near a wall is cut at octant
     * precision wherever no water can stand against it, and lone crumbs of
     * rock left floating in the hollow are cleared. probeCaveFloor() finds a
     * cave floor down any single column exactly as generation lays it.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;
    private CaveLakeBranch caveLakeBranch;

    // Settings
    private int chunkSize;
    private int seaFeatureFloorY;
    private int seaFeatureCeilingY;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.seaFeatureFloorY = TerrainCarveUtility.getSeaFeatureFloorY();
        this.seaFeatureCeilingY = TerrainCarveUtility.getSeaFeatureCeilingY();
    }

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.caveLakeBranch = get(CaveLakeBranch.class);
    }

    // Frames \\

    // The chunk's own cave lattice, covering every point from fromY to toY
    void fillChunkFrame(
            WorldHandle worldHandle,
            long chunkCoordinate,
            TerrainColumnAsyncContainer column,
            CaveFrameStruct frame,
            int fromY,
            int toY) {

        int originY = TerrainCarveUtility.alignDown(fromY);
        int rows = TerrainCarveUtility.computeLatticeRows(fromY, toY);

        TerrainCarveUtility.fillCaveLattice(
                worldHandle.getSeed(), worldHandle.getWorldScale().x, worldHandle.getWorldScale().y,
                (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize, originY,
                (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize,
                TerrainCarveUtility.LATTICE_SIDE, rows, column.hasSeaFeatures, column.caveLattice);

        frame.setLattice(column.caveLattice, TerrainCarveUtility.LATTICE_SIDE, rows, originY, 0f, 0f);
    }

    // The lattice around one column, whose first point lies offsetX and offsetZ into the frame's coordinates
    void fillColumnFrame(
            WorldHandle worldHandle,
            long latticeOriginX,
            long latticeOriginZ,
            float offsetX,
            float offsetZ,
            boolean seaChannel,
            float[] lattice,
            CaveFrameStruct frame,
            int fromY,
            int toY) {

        int originY = TerrainCarveUtility.alignDown(fromY);
        int rows = TerrainCarveUtility.computeLatticeRows(fromY, toY);

        TerrainCarveUtility.fillCaveLattice(
                worldHandle.getSeed(), worldHandle.getWorldScale().x, worldHandle.getWorldScale().y,
                latticeOriginX, originY, latticeOriginZ,
                TerrainCarveUtility.COLUMN_LATTICE_SIDE, rows, seaChannel, lattice);

        frame.setLattice(lattice, TerrainCarveUtility.COLUMN_LATTICE_SIDE, rows, originY, offsetX, offsetZ);
    }

    float sampleChannel(CaveFrameStruct frame, int channel, float frameX, float pointY, float frameZ) {
        return TerrainCarveUtility.sampleLattice(
                frame.lattice, frame.side, frame.rows, TerrainCarveUtility.CAVE_CHANNELS, channel,
                frameX - frame.latticeOffsetX, pointY - frame.originY, frameZ - frame.latticeOffsetZ);
    }

    // Density \\

    // Blocks from the nearest cave wall at a point, positive inside the hollow; reports lake water through the frame
    float computeDensity(
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            int columnIndex,
            float frameX,
            float pointY,
            float frameZ) {

        frame.lakeWater = false;

        if (pointY < column.columnCaveFloorY[columnIndex] || pointY >= column.columnCaveCeilingY[columnIndex] + 1f)
            return EngineSetting.CAVE_DENSITY_SOLID;

        float radiusField = sampleChannel(frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_RADIUS, frameX, pointY, frameZ);

        float caves = Math.max(
                TerrainCarveUtility.computeSpaghetti(
                        sampleChannel(frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_A, frameX, pointY, frameZ),
                        sampleChannel(frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_B, frameX, pointY, frameZ),
                        radiusField, column.columnCaveTunnels[columnIndex]),
                TerrainCarveUtility.computeNoodle(
                        sampleChannel(frame, TerrainCarveUtility.CHANNEL_NOODLE_A, frameX, pointY, frameZ),
                        sampleChannel(frame, TerrainCarveUtility.CHANNEL_NOODLE_B, frameX, pointY, frameZ),
                        sampleChannel(frame, TerrainCarveUtility.CHANNEL_NOODLE_GATE, frameX, pointY, frameZ),
                        radiusField, column.columnCaveNoodles[columnIndex]));

        caves = Math.max(caves, TerrainCarveUtility.computeCheese(
                sampleChannel(frame, TerrainCarveUtility.CHANNEL_CHEESE, frameX, pointY, frameZ),
                column.columnCaveCaverns[columnIndex],
                column.groundHeightBlocks[columnIndex] - pointY));

        float lake = EngineSetting.CAVE_DENSITY_SOLID;
        boolean lakeBelowWaterline = false;

        for (int i = 0; i < frame.lakeSiteCount; i++) {

            CaveLakeSiteStruct site = frame.lakeSites[i];
            float chamber = site.computeChamber(frameX, pointY, frameZ, radiusField);

            caves -= site.computeGuardPenalty(frameX, pointY, frameZ);

            if (chamber > lake) {
                lake = chamber;
                lakeBelowWaterline = site.isBelowWaterline(pointY);
            }
        }

        float penalty = TerrainCarveUtility.computeFloorPenalty(pointY, column.columnCaveFloorY[columnIndex])
                + TerrainCarveUtility.computeRoofPenalty(pointY, column.columnCaveCeilingY[columnIndex])
                + TerrainCarveUtility.computeBarrierPenalty(pointY, column.columnCaveBarrier[columnIndex]);

        frame.lakeWater = lakeBelowWaterline && lake - penalty > 0f;

        return Math.max(caves, lake) - penalty;
    }

    // Notches and sea caves, hollowed through the tide band only where the sea floods caves
    boolean isSeaFeature(
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            int columnIndex,
            float frameX,
            int worldY,
            float frameZ) {

        if (worldY < seaFeatureFloorY || worldY > column.columnSeaCeilingY[columnIndex])
            return false;

        return TerrainCarveUtility.isNotch(
                worldY,
                column.columnFaceDistanceBlocks[columnIndex],
                column.columnOverhangBlocks[columnIndex])
                || TerrainCarveUtility.isSeaCave(
                        sampleChannel(frame, TerrainCarveUtility.CHANNEL_SEA, frameX, worldY + 0.5f, frameZ),
                        column.columnSeaCaves[columnIndex], worldY,
                        column.columnShoreDistanceBlocks[columnIndex]);
    }

    // Whether a whole block is hollow, judged at its centre
    boolean isCarved(
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            int columnIndex,
            float frameX,
            int worldY,
            float frameZ) {
        return computeDensity(frame, column, columnIndex, frameX + 0.5f, worldY + 0.5f, frameZ + 0.5f) > 0f
                || isSeaFeature(frame, column, columnIndex, frameX + 0.5f, worldY, frameZ + 0.5f);
    }

    // Volume \\

    CaveVolumeAsyncContainer carveVolume(
            WorldHandle worldHandle,
            long chunkCoordinate,
            TerrainColumnAsyncContainer column,
            int offsetY) {

        CaveVolumeAsyncContainer volume = worldGenerationManager.getCaveVolumeContainer().getInstance();
        CaveFrameStruct frame = volume.chunkFrame;

        volume.originY = offsetY - CaveVolumeAsyncContainer.MARGIN;
        volume.formationCount = 0;

        fillChunkFrame(
                worldHandle, chunkCoordinate, column, frame,
                volume.originY, volume.originY + CaveVolumeAsyncContainer.HEIGHT);

        caveLakeBranch.collectLakes(
                worldHandle, frame,
                (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize,
                (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize,
                chunkSize,
                offsetY - CaveVolumeAsyncContainer.SCAN_MARGIN,
                offsetY + chunkSize - 1 + CaveVolumeAsyncContainer.SCAN_MARGIN);

        resetVolume(column, volume);

        int topY = volume.originY + CaveVolumeAsyncContainer.HEIGHT - 1;
        int cellsPerAxis = TerrainCarveUtility.LATTICE_SIDE - 1;

        for (int cellRow = 0; cellRow < frame.rows - 1; cellRow++) {

            int cellBottomY = Math.max(frame.originY + cellRow * TerrainCarveUtility.LATTICE_STEP, volume.originY);
            int cellTopY = Math.min(
                    frame.originY + (cellRow + 1) * TerrainCarveUtility.LATTICE_STEP - 1, topY);

            if (cellBottomY > cellTopY)
                continue;

            for (int cellZ = 0; cellZ < cellsPerAxis; cellZ++)
                for (int cellX = 0; cellX < cellsPerAxis; cellX++)
                    if (isCellOpen(frame, column, cellX, cellRow, cellZ, cellBottomY, cellTopY))
                        carveCell(frame, column, volume, cellX, cellZ, cellBottomY, cellTopY);
        }

        clearCrumbs(volume);

        return volume;
    }

    // Cells above the ground stand open; everything else starts as rock
    private void resetVolume(TerrainColumnAsyncContainer column, CaveVolumeAsyncContainer volume) {

        for (int localZ = 0; localZ < chunkSize; localZ++) {
            for (int localX = 0; localX < chunkSize; localX++) {

                int groundHeight = column.groundHeightBlocks[localZ * chunkSize + localX];

                for (int step = 0; step < CaveVolumeAsyncContainer.HEIGHT; step++) {

                    int worldY = volume.originY + step;
                    int cell = volume.index(localX, worldY, localZ);

                    volume.states[cell] = worldY > groundHeight
                            ? CaveVolumeAsyncContainer.STATE_OPEN
                            : CaveVolumeAsyncContainer.STATE_SOLID;
                    volume.densities[cell] = EngineSetting.CAVE_DENSITY_SOLID;
                }
            }
        }
    }

    // Whether anything inside a lattice cell can come within a lining's depth of a cave wall
    private boolean isCellOpen(
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            int cellX,
            int cellRow,
            int cellZ,
            int cellBottomY,
            int cellTopY) {

        if (column.hasSeaFeatures && cellBottomY <= seaFeatureCeilingY && cellTopY >= seaFeatureFloorY)
            return true;

        if (isLakeNearCell(frame, cellX, cellZ, cellBottomY, cellTopY))
            return true;

        float bound = Math.max(
                TerrainCarveUtility.boundSpaghetti(
                        readCorners(frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_A, cellX, cellRow, cellZ, false),
                        readCorners(frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_A, cellX, cellRow, cellZ, true),
                        readCorners(frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_B, cellX, cellRow, cellZ, false),
                        readCorners(frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_B, cellX, cellRow, cellZ, true),
                        column.maxCaveTunnels),
                TerrainCarveUtility.boundNoodle(
                        readCorners(frame, TerrainCarveUtility.CHANNEL_NOODLE_A, cellX, cellRow, cellZ, false),
                        readCorners(frame, TerrainCarveUtility.CHANNEL_NOODLE_A, cellX, cellRow, cellZ, true),
                        readCorners(frame, TerrainCarveUtility.CHANNEL_NOODLE_B, cellX, cellRow, cellZ, false),
                        readCorners(frame, TerrainCarveUtility.CHANNEL_NOODLE_B, cellX, cellRow, cellZ, true),
                        readCorners(frame, TerrainCarveUtility.CHANNEL_NOODLE_GATE, cellX, cellRow, cellZ, true),
                        column.maxCaveNoodles));

        bound = Math.max(bound, TerrainCarveUtility.boundCheese(
                readCorners(frame, TerrainCarveUtility.CHANNEL_CHEESE, cellX, cellRow, cellZ, true),
                column.maxCaveCaverns));

        return bound > -EngineSetting.CAVE_BIOME_MAX_SHELL_BLOCKS;
    }

    // The lowest or highest value a channel holds at a lattice cell's eight corners
    private float readCorners(CaveFrameStruct frame, int channel, int cellX, int cellRow, int cellZ, boolean highest) {

        float extreme = highest ? -Float.MAX_VALUE : Float.MAX_VALUE;

        for (int corner = 0; corner < SubBlockUtility.OCTANT_COUNT; corner++) {

            float value = TerrainCarveUtility.readLattice(
                    frame.lattice, frame.side, TerrainCarveUtility.CAVE_CHANNELS, channel,
                    cellX + SubBlockUtility.getOctantX(corner),
                    cellRow + SubBlockUtility.getOctantY(corner),
                    cellZ + SubBlockUtility.getOctantZ(corner));

            extreme = highest ? Math.max(extreme, value) : Math.min(extreme, value);
        }

        return extreme;
    }

    private boolean isLakeNearCell(CaveFrameStruct frame, int cellX, int cellZ, int cellBottomY, int cellTopY) {

        float minX = cellX * TerrainCarveUtility.LATTICE_STEP;
        float minZ = cellZ * TerrainCarveUtility.LATTICE_STEP;
        float maxX = minX + TerrainCarveUtility.LATTICE_STEP;
        float maxZ = minZ + TerrainCarveUtility.LATTICE_STEP;

        for (int i = 0; i < frame.lakeSiteCount; i++) {

            CaveLakeSiteStruct site = frame.lakeSites[i];
            float reach = site.getReachBlocks();

            if (site.centerX + reach >= minX && site.centerX - reach <= maxX
                    && site.centerZ + reach >= minZ && site.centerZ - reach <= maxZ
                    && site.getHighestY() >= cellBottomY && site.getLowestY() <= cellTopY + 1)
                return true;
        }

        return false;
    }

    private void carveCell(
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            int cellX,
            int cellZ,
            int cellBottomY,
            int cellTopY) {

        int fromX = cellX * TerrainCarveUtility.LATTICE_STEP;
        int fromZ = cellZ * TerrainCarveUtility.LATTICE_STEP;

        for (int localZ = fromZ; localZ < fromZ + TerrainCarveUtility.LATTICE_STEP; localZ++)
            for (int localX = fromX; localX < fromX + TerrainCarveUtility.LATTICE_STEP; localX++)
                for (int worldY = cellBottomY; worldY <= cellTopY; worldY++)
                    carveBlock(frame, column, volume, localX, worldY, localZ);
    }

    private void carveBlock(
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            int localX,
            int worldY,
            int localZ) {

        int cell = volume.index(localX, worldY, localZ);

        if (volume.states[cell] == CaveVolumeAsyncContainer.STATE_OPEN)
            return;

        int columnIndex = localZ * chunkSize + localX;
        float density = computeDensity(frame, column, columnIndex, localX + 0.5f, worldY + 0.5f, localZ + 0.5f);
        boolean lakeWater = frame.lakeWater;
        boolean seaFlood = column.columnSeaZone[columnIndex] == TerrainColumnAsyncContainer.SEA_ZONE_FLOOD;

        volume.densities[cell] = density;

        if (lakeWater) {
            volume.states[cell] = CaveVolumeAsyncContainer.STATE_LAKE;
            return;
        }

        if (density <= 0f && isSeaFeature(frame, column, columnIndex, localX + 0.5f, worldY, localZ + 0.5f)) {
            volume.states[cell] = CaveVolumeAsyncContainer.STATE_SEA;
            return;
        }

        boolean seaWet = seaFlood && worldY <= TideUtility.BAND_MAX_Y;
        boolean smooth = !(seaFlood && worldY <= TideUtility.BAND_MAX_Y + 1)
                && !isNearLakeWater(frame, localX + 0.5f, worldY, localZ + 0.5f);

        if (!smooth || Math.abs(density) >= EngineSetting.CAVE_SMOOTHING_MARGIN_BLOCKS) {
            volume.states[cell] = density > 0f
                    ? (seaWet ? CaveVolumeAsyncContainer.STATE_SEA : CaveVolumeAsyncContainer.STATE_HOLLOW)
                    : CaveVolumeAsyncContainer.STATE_SOLID;
            return;
        }

        int solidMask = resolveSolidOctants(frame, column, columnIndex, localX, worldY, localZ);

        if (solidMask == SubBlockUtility.MASK_FULL)
            volume.states[cell] = CaveVolumeAsyncContainer.STATE_SOLID;
        else if (solidMask == SubBlockUtility.MASK_EMPTY)
            volume.states[cell] = CaveVolumeAsyncContainer.STATE_HOLLOW;
        else {
            volume.states[cell] = CaveVolumeAsyncContainer.STATE_PARTIAL;
            volume.solidMasks[cell] = (byte) solidMask;
        }
    }

    // The octants of a block the cave leaves solid, each judged at its own centre
    private int resolveSolidOctants(
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            int columnIndex,
            int localX,
            int worldY,
            int localZ) {

        int mask = SubBlockUtility.MASK_EMPTY;
        float half = SubBlockUtility.SIZE * 0.5f;

        for (int octant = 0; octant < SubBlockUtility.OCTANT_COUNT; octant++) {

            float pointX = localX + half + SubBlockUtility.getOctantX(octant) * SubBlockUtility.SIZE;
            float pointY = worldY + half + SubBlockUtility.getOctantY(octant) * SubBlockUtility.SIZE;
            float pointZ = localZ + half + SubBlockUtility.getOctantZ(octant) * SubBlockUtility.SIZE;

            if (computeDensity(frame, column, columnIndex, pointX, pointY, pointZ) <= 0f)
                mask |= SubBlockUtility.getOctantBit(octant);
        }

        return mask;
    }

    // Within a block of where any lake's water stands, where walls stay whole so water never meets a cut block
    private boolean isNearLakeWater(CaveFrameStruct frame, float frameX, int worldY, float frameZ) {

        for (int i = 0; i < frame.lakeSiteCount; i++) {

            CaveLakeSiteStruct site = frame.lakeSites[i];
            float reachX = site.radiusX + EngineSetting.CAVE_LAKE_GUARD_BLOCKS;
            float reachZ = site.radiusZ + EngineSetting.CAVE_LAKE_GUARD_BLOCKS;

            if (Math.abs(frameX - site.centerX) <= reachX && Math.abs(frameZ - site.centerZ) <= reachZ
                    && worldY <= site.waterY + 1 && worldY >= site.getLowestY())
                return true;
        }

        return false;
    }

    // Slivers of a few octants and whole blocks with dry hollow on every side are cleared, so no rock floats in the
    // air; a block whose neighbours lie outside the chunk is always kept, since another chunk holds the truth there
    private void clearCrumbs(CaveVolumeAsyncContainer volume) {

        for (int worldY = volume.originY + 1; worldY < volume.originY + CaveVolumeAsyncContainer.HEIGHT - 1; worldY++) {
            for (int localZ = 0; localZ < chunkSize; localZ++) {
                for (int localX = 0; localX < chunkSize; localX++) {

                    int cell = volume.index(localX, worldY, localZ);
                    byte state = volume.states[cell];

                    if (state == CaveVolumeAsyncContainer.STATE_PARTIAL
                            && Integer.bitCount(volume.solidMasks[cell] & SubBlockUtility.MASK_FULL)
                                    < EngineSetting.CAVE_CRUMB_MIN_OCTANTS)
                        volume.states[cell] = CaveVolumeAsyncContainer.STATE_HOLLOW;
                    else if (state == CaveVolumeAsyncContainer.STATE_SOLID
                            && volume.densities[cell] > -EngineSetting.CAVE_CRUMB_REACH_BLOCKS
                            && isEnclosedByHollow(volume, localX, worldY, localZ))
                        volume.states[cell] = CaveVolumeAsyncContainer.STATE_HOLLOW;
                }
            }
        }
    }

    private boolean isEnclosedByHollow(CaveVolumeAsyncContainer volume, int localX, int worldY, int localZ) {

        if (localX == 0 || localX == chunkSize - 1 || localZ == 0 || localZ == chunkSize - 1)
            return false;

        return volume.isHollow(localX - 1, worldY, localZ)
                && volume.isHollow(localX + 1, worldY, localZ)
                && volume.isHollow(localX, worldY - 1, localZ)
                && volume.isHollow(localX, worldY + 1, localZ)
                && volume.isHollow(localX, worldY, localZ - 1)
                && volume.isHollow(localX, worldY, localZ + 1);
    }

    // Probe \\

    // The highest cave floor between minY and maxY down one column with clearance blocks of dry cave air above it,
    // NO_FLOOR when there is none
    int probeCaveFloor(
            WorldHandle worldHandle,
            long worldX,
            long worldZ,
            int minY,
            int maxY,
            int clearanceBlocks) {

        long wrappedX = WorldWrapUtility.wrapBlockX(worldHandle, worldX);
        long wrappedZ = WorldWrapUtility.wrapBlockZ(worldHandle, worldZ);

        TerrainColumnAsyncContainer probeColumn = worldGenerationManager.resolveProbeCaveColumn(
                worldHandle, wrappedX, wrappedZ);
        CaveProbeAsyncContainer probe = worldGenerationManager.getCaveProbeContainer().getInstance();
        CaveFrameStruct frame = probe.frame;

        int localX = (int) (wrappedX % chunkSize);
        int localZ = (int) (wrappedZ % chunkSize);
        int columnIndex = localZ * chunkSize + localX;
        int groundHeight = probeColumn.groundHeightBlocks[columnIndex];
        long latticeOriginX = TerrainCarveUtility.alignDown(wrappedX);
        long latticeOriginZ = TerrainCarveUtility.alignDown(wrappedZ);
        int fromY = Math.max(minY, 0);
        int toY = Math.min(maxY + clearanceBlocks, groundHeight) + 1;

        if (fromY >= toY)
            return TerrainColumnAsyncContainer.NO_FLOOR;

        fillColumnFrame(
                worldHandle, latticeOriginX, latticeOriginZ, 0f, 0f, probeColumn.hasSeaFeatures,
                probe.lattice, frame, fromY, toY);
        caveLakeBranch.collectLakes(worldHandle, frame, latticeOriginX, latticeOriginZ, 1, fromY, toY);

        float frameX = wrappedX - latticeOriginX;
        float frameZ = wrappedZ - latticeOriginZ;
        boolean seaFlood = probeColumn.columnSeaZone[columnIndex] == TerrainColumnAsyncContainer.SEA_ZONE_FLOOD;
        int clear = 0;

        for (int worldY = toY - 1; worldY >= fromY; worldY--) {

            boolean carved = isCarved(frame, probeColumn, columnIndex, frameX, worldY, frameZ);
            boolean dry = !frame.lakeWater && !(seaFlood && worldY <= TideUtility.BAND_MAX_Y);

            if (carved && dry && worldY <= groundHeight) {
                clear++;
                continue;
            }

            if (!carved && clear >= clearanceBlocks && worldY <= maxY)
                return worldY;

            clear = 0;
        }

        return TerrainColumnAsyncContainer.NO_FLOOR;
    }
}
