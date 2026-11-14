package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import application.bootstrap.worldpipeline.util.TideUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.kernelpipeline.kernel.KernelArgumentInstance;
import application.kernel.kernelpipeline.kernelmanager.KernelManager;
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
     * and below the tide where the sea is walled off; the CaveDensity kernel
     * computes it for a whole field of points per call, so every caller
     * queues its points, runs computeField() once and reads the results back
     * in its own order. carveVolume() resolves
     * a subchunk and a margin around it: lattice cells whose corners prove
     * them solid are skipped whole, a cell near a wall is cut at octant
     * precision wherever no water can stand against it, and lone crumbs of
     * rock left floating in the hollow are cleared. probeCaveFloor() finds a
     * cave floor down any single column exactly as generation lays it.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;
    private CaveLakeBranch caveLakeBranch;
    private KernelManager kernelManager;
    private CaveDensityAsyncContainer caveDensityContainer;

    // Kernels
    private int caveDensityKernelID;

    // Settings
    private int chunkSize;
    private int seaFeatureFloorY;
    private int seaFeatureCeilingY;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.caveDensityContainer = create(CaveDensityAsyncContainer.class);

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.seaFeatureFloorY = TerrainCarveUtility.getSeaFeatureFloorY();
        this.seaFeatureCeilingY = TerrainCarveUtility.getSeaFeatureCeilingY();
    }

    @Override
    protected void get() {

        // Internal
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.caveLakeBranch = get(CaveLakeBranch.class);
        this.kernelManager = get(KernelManager.class);

        // Kernels
        this.caveDensityKernelID = kernelManager.getKernelIDFromKernelName(EngineSetting.KERNEL_CAVE_DENSITY);
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

    // The lattice cell a frame point lies in, resolved once for every channel sampleFramePoint() reads there
    void resolveFramePoint(CaveFrameStruct frame, float frameX, float pointY, float frameZ) {
        TerrainCarveUtility.resolveLatticePoint(
                frame.latticePoint, frame.side, frame.rows, TerrainCarveUtility.CAVE_CHANNELS,
                frameX - frame.latticeOffsetX, pointY - frame.originY, frameZ - frame.latticeOffsetZ);
    }

    float sampleFramePoint(CaveFrameStruct frame, int channel) {
        return TerrainCarveUtility.sampleLatticePoint(frame.lattice, frame.latticePoint, channel);
    }

    // Density \\

    // The field this thread fills, bound to a frame and the chunk's columns and holding no points yet
    CaveDensityAsyncContainer bindField(CaveFrameStruct frame, TerrainColumnAsyncContainer column) {

        CaveDensityAsyncContainer field = caveDensityContainer.getInstance();
        field.bind(frame, column);

        return field;
    }

    // The block centres down one column from fromY to toY, each tagged with its height
    void queueColumn(CaveDensityAsyncContainer field, int columnIndex, float pointX, float pointZ, int fromY, int toY) {
        for (int worldY = fromY; worldY <= toY; worldY++)
            field.addPoint(pointX, worldY + 0.5f, pointZ, columnIndex, worldY);
    }

    // Every queued point's blocks from the nearest cave wall, positive inside the hollow, and whether it lies in lake
    // water, in one call of the CaveDensity kernel; the slots follow CaveDensityArgumentStruct exactly
    void computeField(CaveDensityAsyncContainer field) {

        if (field.getPointCount() == 0)
            return;

        KernelArgumentInstance arguments = field.arguments;

        arguments.reset();

        // Lattice
        arguments.putBuffer(field.lattice);
        arguments.putInt(field.latticeSide);
        arguments.putInt(field.latticeRows);
        arguments.putInt(TerrainCarveUtility.CAVE_CHANNELS);
        arguments.putInt(TerrainCarveUtility.LATTICE_STEP);
        arguments.putInt(field.latticeOriginY);
        arguments.putFloat(field.latticeOffsetX);
        arguments.putFloat(field.latticeOffsetZ);
        arguments.putInt(TerrainCarveUtility.CHANNEL_SPAGHETTI_A);
        arguments.putInt(TerrainCarveUtility.CHANNEL_SPAGHETTI_B);
        arguments.putInt(TerrainCarveUtility.CHANNEL_SPAGHETTI_RADIUS);
        arguments.putInt(TerrainCarveUtility.CHANNEL_CHEESE);
        arguments.putInt(TerrainCarveUtility.CHANNEL_NOODLE_A);
        arguments.putInt(TerrainCarveUtility.CHANNEL_NOODLE_B);
        arguments.putInt(TerrainCarveUtility.CHANNEL_NOODLE_GATE);

        // Lakes
        arguments.putBuffer(field.lakes);
        arguments.putInt(field.lakeCount);

        // Columns
        arguments.putBuffer(field.columnFloorY);
        arguments.putBuffer(field.columnCeilingY);
        arguments.putBuffer(field.columnGroundY);
        arguments.putBuffer(field.columnTunnels);
        arguments.putBuffer(field.columnNoodles);
        arguments.putBuffer(field.columnCaverns);
        arguments.putBuffer(field.columnBarrier);

        // Points
        arguments.putBuffer(field.pointX);
        arguments.putBuffer(field.pointY);
        arguments.putBuffer(field.pointZ);
        arguments.putBuffer(field.pointColumn);
        arguments.putInt(field.getPointCount());

        // Results
        arguments.putBuffer(field.densities);
        arguments.putBuffer(field.lakeWater);

        // Scratch
        arguments.putBuffer(field.radiusScratch);
        arguments.putBuffer(field.lakeScratch);

        // Settings
        arguments.putFloat(EngineSetting.CAVE_DENSITY_SOLID);
        arguments.putFloat(TerrainCarveUtility.SPAGHETTI_SCALE);
        arguments.putFloat(EngineSetting.CAVE_SPAGHETTI_MIN_RADIUS_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_SPAGHETTI_MAX_RADIUS_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_SPAGHETTI_CLOSE_SHARE);
        arguments.putFloat(EngineSetting.CAVE_SPAGHETTI_AMOUNT_FLOOR);
        arguments.putFloat(TerrainCarveUtility.NOODLE_SCALE);
        arguments.putFloat(EngineSetting.CAVE_NOODLE_MIN_RADIUS_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_NOODLE_MAX_RADIUS_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_NOODLE_GATE_THRESHOLD);
        arguments.putFloat(EngineSetting.CAVE_NOODLE_GATE_RANGE);
        arguments.putFloat(EngineSetting.CAVE_NOODLE_GATE_EDGE);
        arguments.putFloat(TerrainCarveUtility.CHEESE_SCALE);
        arguments.putInt(EngineSetting.CAVE_CHEESE_MIN_DEPTH_BLOCKS);
        arguments.putInt(EngineSetting.CAVE_CHEESE_DEPTH_RAMP_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_CHEESE_SHALLOW_SHARE);
        arguments.putFloat(EngineSetting.CAVE_CHEESE_THRESHOLD_MAX);
        arguments.putFloat(EngineSetting.CAVE_CHEESE_THRESHOLD_MIN);
        arguments.putInt(EngineSetting.CAVE_FLOOR_FADE_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_FLOOR_FADE_STRENGTH);
        arguments.putInt(EngineSetting.CAVE_ROOF_TAPER_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_ROOF_TAPER_STRENGTH);
        arguments.putInt(TideUtility.BAND_MAX_Y);
        arguments.putInt(EngineSetting.CAVE_SEA_BARRIER_RISE_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_SEA_BARRIER_PENALTY_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_LAKE_SHORE_WOBBLE);
        arguments.putFloat(EngineSetting.CAVE_LAKE_GUARD_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_LAKE_TAPER_BLOCKS);
        arguments.putFloat(EngineSetting.CAVE_LAKE_TAPER_PENALTY_BLOCKS);
        arguments.putFloat(EngineSetting.DIVISION_EPSILON);

        kernelManager.invoke(caveDensityKernelID, arguments);
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
                || isSeaCave(frame, column, columnIndex, frameX, worldY, frameZ);
    }

    private boolean isSeaCave(
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            int columnIndex,
            float frameX,
            int worldY,
            float frameZ) {

        resolveFramePoint(frame, frameX, worldY + 0.5f, frameZ);

        return TerrainCarveUtility.isSeaCave(
                sampleFramePoint(frame, TerrainCarveUtility.CHANNEL_SEA),
                column.columnSeaCaves[columnIndex], worldY,
                column.columnShoreDistanceBlocks[columnIndex]);
    }

    // Whether a whole block is hollow, judged at its centre from the density a field computed there
    boolean isCarved(
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            int columnIndex,
            float frameX,
            int worldY,
            float frameZ,
            float density) {
        return density > 0f || isSeaFeature(frame, column, columnIndex, frameX + 0.5f, worldY, frameZ + 0.5f);
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

        CaveDensityAsyncContainer field = bindField(frame, column);
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
                        queueCell(field, volume, cellX, cellZ, cellBottomY, cellTopY);
        }

        computeField(field);
        carveBlocks(field, frame, column, volume);
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

    // Every block of a lattice cell that is not already open, tagged with its cell in the volume
    private void queueCell(
            CaveDensityAsyncContainer field,
            CaveVolumeAsyncContainer volume,
            int cellX,
            int cellZ,
            int cellBottomY,
            int cellTopY) {

        int fromX = cellX * TerrainCarveUtility.LATTICE_STEP;
        int fromZ = cellZ * TerrainCarveUtility.LATTICE_STEP;

        for (int localZ = fromZ; localZ < fromZ + TerrainCarveUtility.LATTICE_STEP; localZ++)
            for (int localX = fromX; localX < fromX + TerrainCarveUtility.LATTICE_STEP; localX++)
                for (int worldY = cellBottomY; worldY <= cellTopY; worldY++) {

                    int cell = volume.index(localX, worldY, localZ);

                    if (volume.states[cell] != CaveVolumeAsyncContainer.STATE_OPEN)
                        field.addPoint(
                                localX + 0.5f, worldY + 0.5f, localZ + 0.5f, localZ * chunkSize + localX, cell);
                }
    }

    // Each queued block's state from its density, then the blocks beside a wall cut at octant precision
    private void carveBlocks(
            CaveDensityAsyncContainer field,
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume) {

        int cutCount = 0;

        for (int point = 0; point < field.getPointCount(); point++) {

            int cell = field.getTag(point);

            if (carveBlock(frame, column, volume, cell, field.getDensity(point), field.isLakeWater(point)))
                volume.cutCells[cutCount++] = cell;
        }

        cutBlocks(field, volume, cutCount);
    }

    // True when the block lies close enough to a smooth wall to be cut at octant precision
    private boolean carveBlock(
            CaveFrameStruct frame,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            int cell,
            float density,
            boolean lakeWater) {

        int columnIndex = volume.toColumnIndex(cell);
        int localX = columnIndex % chunkSize;
        int localZ = columnIndex / chunkSize;
        int worldY = volume.toWorldY(cell);
        boolean seaFlood = column.columnSeaZone[columnIndex] == TerrainColumnAsyncContainer.SEA_ZONE_FLOOD;

        volume.densities[cell] = density;

        if (lakeWater) {
            volume.states[cell] = CaveVolumeAsyncContainer.STATE_LAKE;
            return false;
        }

        if (density <= 0f && isSeaFeature(frame, column, columnIndex, localX + 0.5f, worldY, localZ + 0.5f)) {
            volume.states[cell] = CaveVolumeAsyncContainer.STATE_SEA;
            return false;
        }

        boolean seaWet = seaFlood && worldY <= TideUtility.BAND_MAX_Y;
        boolean smooth = !(seaFlood && worldY <= TideUtility.BAND_MAX_Y + 1)
                && !isNearLakeWater(frame, localX + 0.5f, worldY, localZ + 0.5f);

        if (!smooth || Math.abs(density) >= EngineSetting.CAVE_SMOOTHING_MARGIN_BLOCKS) {
            volume.states[cell] = density > 0f
                    ? (seaWet ? CaveVolumeAsyncContainer.STATE_SEA : CaveVolumeAsyncContainer.STATE_HOLLOW)
                    : CaveVolumeAsyncContainer.STATE_SOLID;
            return false;
        }

        return true;
    }

    // The octants each cut block leaves solid, each judged at its own centre, as many blocks a field as it holds
    private void cutBlocks(CaveDensityAsyncContainer field, CaveVolumeAsyncContainer volume, int cutCount) {

        int blocksPerField = CaveDensityAsyncContainer.POINT_CAPACITY / SubBlockUtility.OCTANT_COUNT;

        for (int first = 0; first < cutCount; first += blocksPerField) {

            int last = Math.min(cutCount, first + blocksPerField);

            field.clearPoints();

            for (int i = first; i < last; i++)
                queueOctants(field, volume, volume.cutCells[i]);

            computeField(field);

            for (int i = first; i < last; i++)
                cutBlock(field, volume, volume.cutCells[i], (i - first) * SubBlockUtility.OCTANT_COUNT);
        }
    }

    private void queueOctants(CaveDensityAsyncContainer field, CaveVolumeAsyncContainer volume, int cell) {

        int columnIndex = volume.toColumnIndex(cell);
        int localX = columnIndex % chunkSize;
        int localZ = columnIndex / chunkSize;
        int worldY = volume.toWorldY(cell);
        float half = SubBlockUtility.SIZE * 0.5f;

        for (int octant = 0; octant < SubBlockUtility.OCTANT_COUNT; octant++) {

            float pointX = localX + half + SubBlockUtility.getOctantX(octant) * SubBlockUtility.SIZE;
            float pointY = worldY + half + SubBlockUtility.getOctantY(octant) * SubBlockUtility.SIZE;
            float pointZ = localZ + half + SubBlockUtility.getOctantZ(octant) * SubBlockUtility.SIZE;

            field.addPoint(pointX, pointY, pointZ, columnIndex, cell);
        }
    }

    private void cutBlock(CaveDensityAsyncContainer field, CaveVolumeAsyncContainer volume, int cell, int firstPoint) {

        int solidMask = SubBlockUtility.MASK_EMPTY;

        for (int octant = 0; octant < SubBlockUtility.OCTANT_COUNT; octant++)
            if (field.getDensity(firstPoint + octant) <= 0f)
                solidMask |= SubBlockUtility.getOctantBit(octant);

        if (solidMask == SubBlockUtility.MASK_FULL)
            volume.states[cell] = CaveVolumeAsyncContainer.STATE_SOLID;
        else if (solidMask == SubBlockUtility.MASK_EMPTY)
            volume.states[cell] = CaveVolumeAsyncContainer.STATE_HOLLOW;
        else {
            volume.states[cell] = CaveVolumeAsyncContainer.STATE_PARTIAL;
            volume.solidMasks[cell] = (byte) solidMask;
        }
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
        CaveDensityAsyncContainer field = bindField(frame, probeColumn);
        int clear = 0;

        queueColumn(field, columnIndex, frameX + 0.5f, frameZ + 0.5f, fromY, toY - 1);
        computeField(field);

        for (int worldY = toY - 1; worldY >= fromY; worldY--) {

            int point = worldY - fromY;
            boolean carved = isCarved(
                    frame, probeColumn, columnIndex, frameX, worldY, frameZ, field.getDensity(point));
            boolean dry = !field.isLakeWater(point) && !(seaFlood && worldY <= TideUtility.BAND_MAX_Y);

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
