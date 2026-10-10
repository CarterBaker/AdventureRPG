package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class CaveDressingBranch extends BranchPackage {

    /*
     * Async — dresses a carved subchunk with the cave biome lining each
     * stretch of it. Rock within a cave biome's lining becomes its rock,
     * floors, ceilings and the beds under still water take their own
     * blocks, and floors, walls and ceilings take its coverings. Dry ceilings
     * and floors grow small stalactites and stalagmites cut from octants,
     * thin spikes or a thick base tapering to a spike. Tall dry chambers
     * found down a few columns per chunk raise giant cones and columns
     * built of blocks, each read down its own column so every subchunk it
     * crosses lays the same formation. Water never meets a speleothem.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;
    private CaveCarveBranch caveCarveBranch;
    private CaveRegionBranch caveRegionBranch;

    // Settings
    private int chunkSize;
    private int maxLengthBlocks;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.maxLengthBlocks = EngineSetting.SPELEOTHEM_MAX_LENGTH_BLOCKS;
    }

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.caveCarveBranch = get(CaveCarveBranch.class);
        this.caveRegionBranch = get(CaveRegionBranch.class);
    }

    // Formations \\

    // Every giant formation standing in a tall dry chamber down one of the chunk's formation columns that reaches
    // the subchunk
    void prepareFormations(
            WorldHandle worldHandle,
            long chunkCoordinate,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            int offsetY) {

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);
        long seed = worldHandle.getSeed() ^ EngineSetting.SPELEOTHEM_GIANT_SITE_SALT;

        volume.formationCount = 0;

        for (int site = 0; site < EngineSetting.SPELEOTHEM_GIANT_SITES_PER_CHUNK; site++) {

            long siteHash = TerrainCarveUtility.hashCell(seed, chunkX, site, chunkZ);
            int localX = Math.min((int) (roll(siteHash, 0) * chunkSize), chunkSize - 1);
            int localZ = Math.min((int) (roll(siteHash, 1) * chunkSize), chunkSize - 1);
            float roomBlocks = Math.min(
                    Math.min(localX, chunkSize - 1 - localX),
                    Math.min(localZ, chunkSize - 1 - localZ)) + 0.5f;

            if (roomBlocks < EngineSetting.SPELEOTHEM_GIANT_MIN_RADIUS_BLOCKS)
                continue;

            prepareSite(worldHandle, chunkCoordinate, column, volume, offsetY, localX, localZ, roomBlocks, siteHash);
        }
    }

    private void prepareSite(
            WorldHandle worldHandle,
            long chunkCoordinate,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            int offsetY,
            int localX,
            int localZ,
            float roomBlocks,
            long siteHash) {

        int columnIndex = localZ * chunkSize + localX;

        if (column.columnSeaZone[columnIndex] == TerrainColumnAsyncContainer.SEA_ZONE_FLOOD)
            return;

        int groundHeight = column.groundHeightBlocks[columnIndex];
        int scanFromY = Math.max(
                offsetY - CaveVolumeAsyncContainer.SCAN_MARGIN, column.columnCaveFloorY[columnIndex] - 1);
        int scanToY = Math.min(
                offsetY + chunkSize - 1 + CaveVolumeAsyncContainer.SCAN_MARGIN,
                Math.min(column.columnCaveCeilingY[columnIndex], groundHeight) + 1);

        if (scanToY - scanFromY < EngineSetting.SPELEOTHEM_GIANT_MIN_SPAN_BLOCKS + 2)
            return;

        long chunkOriginX = (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize;
        long chunkOriginZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize;
        long latticeOriginX = TerrainCarveUtility.alignDown(chunkOriginX + localX);
        long latticeOriginZ = TerrainCarveUtility.alignDown(chunkOriginZ + localZ);
        CaveFrameStruct frame = volume.columnFrame;

        caveCarveBranch.fillColumnFrame(
                worldHandle, latticeOriginX, latticeOriginZ,
                latticeOriginX - chunkOriginX, latticeOriginZ - chunkOriginZ, false,
                volume.columnLattice, frame, scanFromY, scanToY + 1);
        copyLakeSites(volume.chunkFrame, frame);

        float frameX = localX + 0.5f;
        float frameZ = localZ + 0.5f;

        for (int worldY = scanFromY; worldY <= scanToY; worldY++) {

            float density = caveCarveBranch.computeDensity(
                    frame, column, columnIndex, frameX, worldY + 0.5f, frameZ);

            volume.scanHollow[worldY - scanFromY] = density > 0f && !frame.lakeWater;
        }

        int floorY = Integer.MIN_VALUE;

        for (int worldY = scanFromY + 1; worldY <= scanToY; worldY++) {

            boolean hollow = volume.scanHollow[worldY - scanFromY];
            boolean belowHollow = volume.scanHollow[worldY - 1 - scanFromY];

            if (hollow && !belowHollow)
                floorY = worldY - 1;
            else if (!hollow && belowHollow && floorY != Integer.MIN_VALUE) {
                prepareRun(
                        worldHandle, column, volume, frame, chunkOriginX, chunkOriginZ, offsetY,
                        localX, localZ, columnIndex, floorY, worldY, roomBlocks, siteHash);
                floorY = Integer.MIN_VALUE;
            }
        }
    }

    // One chamber found down a formation column, between the solid floor below it and the solid ceiling above
    private void prepareRun(
            WorldHandle worldHandle,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            CaveFrameStruct frame,
            long chunkOriginX,
            long chunkOriginZ,
            int offsetY,
            int localX,
            int localZ,
            int columnIndex,
            int floorY,
            int ceilingY,
            float roomBlocks,
            long siteHash) {

        int span = ceilingY - floorY - 1;

        if (span < EngineSetting.SPELEOTHEM_GIANT_MIN_SPAN_BLOCKS
                || span > EngineSetting.SPELEOTHEM_GIANT_MAX_SPAN_BLOCKS)
            return;

        if (floorY + 1 - EngineSetting.SPELEOTHEM_GIANT_FLARE_BLOCKS > offsetY + chunkSize - 1
                || ceilingY - 1 + EngineSetting.SPELEOTHEM_GIANT_FLARE_BLOCKS < offsetY)
            return;

        TerrainCaveProfileStruct profile = caveRegionBranch.resolveProfile(
                worldHandle, frame, chunkOriginX, chunkOriginZ,
                localX + 0.5f, floorY + 1 + span / 2, localZ + 0.5f,
                column.groundHeightBlocks[columnIndex]);

        if (profile == null || !profile.speleothems || profile.giants <= 0f)
            return;

        long formHash = TerrainCarveUtility.hashCell(
                siteHash ^ EngineSetting.SPELEOTHEM_GIANT_FORM_SALT, localX, floorY, localZ);

        if (roll(formHash, 0) >= profile.giants)
            return;

        float widest = Math.min(profile.maxGiantRadiusBlocks, roomBlocks);
        float radius = Math.max(EngineSetting.SPELEOTHEM_GIANT_MIN_RADIUS_BLOCKS,
                EngineSetting.SPELEOTHEM_GIANT_MIN_RADIUS_BLOCKS
                        + (widest - EngineSetting.SPELEOTHEM_GIANT_MIN_RADIUS_BLOCKS) * roll(formHash, 1));
        float stalagmiteReach = 0f;
        float stalactiteReach = 0f;
        float waist = 0f;

        if (roll(formHash, 2) < EngineSetting.SPELEOTHEM_GIANT_COLUMN_SHARE) {
            stalagmiteReach = span * lerp(EngineSetting.SPELEOTHEM_GIANT_COLUMN_MIN_REACH, 1f, roll(formHash, 3));
            stalactiteReach = span * lerp(EngineSetting.SPELEOTHEM_GIANT_COLUMN_MIN_REACH, 1f, roll(formHash, 4));
            waist = radius * EngineSetting.SPELEOTHEM_GIANT_WAIST_RATIO;
        } else {
            boolean stalactite = roll(formHash, 5) < EngineSetting.SPELEOTHEM_GIANT_STALACTITE_SHARE;
            boolean stalagmite = !stalactite || roll(formHash, 6) < EngineSetting.SPELEOTHEM_GIANT_STALAGMITE_SHARE;

            if (stalagmite)
                stalagmiteReach = span * lerp(
                        EngineSetting.SPELEOTHEM_GIANT_MIN_REACH, EngineSetting.SPELEOTHEM_GIANT_MAX_REACH,
                        roll(formHash, 3));

            if (stalactite)
                stalactiteReach = span * lerp(
                        EngineSetting.SPELEOTHEM_GIANT_MIN_REACH, EngineSetting.SPELEOTHEM_GIANT_MAX_REACH,
                        roll(formHash, 4));
        }

        CaveFormationStruct formation = volume.addFormation();

        if (formation == null)
            return;

        formation.set(
                localX + 0.5f, localZ + 0.5f, floorY, ceilingY,
                stalagmiteReach, stalactiteReach, radius, waist, profile.speleothemBlockID);
    }

    private void copyLakeSites(CaveFrameStruct source, CaveFrameStruct target) {

        target.clearLakeSites();

        for (int i = 0; i < source.lakeSiteCount; i++) {

            CaveLakeSiteStruct from = source.lakeSites[i];

            target.addLakeSite().set(
                    from.centerX, from.centerZ, from.waterY,
                    from.radiusX, from.radiusZ, from.depth, from.headroom);
        }
    }

    // Speleothems \\

    // The octants of a dry cell taken by speleothems, reporting their block through the volume; 0 when none
    int resolveSpeleothemMask(
            WorldHandle worldHandle,
            long chunkCoordinate,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            int localX,
            int worldY,
            int localZ) {

        volume.speleothemBlockID = EngineSetting.REGISTRY_RESERVED_ID;

        int mask = resolveFormationMask(volume, localX, worldY, localZ);

        if (!volume.isHollow(localX, worldY, localZ))
            return mask;

        mask |= resolveHangingMask(worldHandle, chunkCoordinate, column, volume, localX, worldY, localZ);
        mask |= resolveRisingMask(worldHandle, chunkCoordinate, column, volume, localX, worldY, localZ);

        return mask;
    }

    private int resolveFormationMask(CaveVolumeAsyncContainer volume, int localX, int worldY, int localZ) {

        int mask = SubBlockUtility.MASK_EMPTY;
        float half = SubBlockUtility.SIZE * 0.5f;

        for (int i = 0; i < volume.formationCount; i++) {

            CaveFormationStruct formation = volume.formations[i];

            if (worldY < formation.getLowestY() || worldY > formation.getHighestY())
                continue;

            int formationMask = SubBlockUtility.MASK_EMPTY;

            for (int octant = 0; octant < SubBlockUtility.OCTANT_COUNT; octant++) {

                float pointX = localX + half + SubBlockUtility.getOctantX(octant) * SubBlockUtility.SIZE;
                float pointY = worldY + half + SubBlockUtility.getOctantY(octant) * SubBlockUtility.SIZE;
                float pointZ = localZ + half + SubBlockUtility.getOctantZ(octant) * SubBlockUtility.SIZE;
                float dx = pointX - formation.centerX;
                float dz = pointZ - formation.centerZ;
                float spread = formation.computeRadius(pointY);

                if (spread > 0f && dx * dx + dz * dz <= spread * spread)
                    formationMask |= SubBlockUtility.getOctantBit(octant);
            }

            if (formationMask != SubBlockUtility.MASK_EMPTY) {
                mask |= formationMask;
                volume.speleothemBlockID = formation.blockID;
            }
        }

        return mask;
    }

    // A stalactite hanging from the first solid ceiling straight above, if one hangs there and reaches down this far
    private int resolveHangingMask(
            WorldHandle worldHandle,
            long chunkCoordinate,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            int localX,
            int worldY,
            int localZ) {

        for (int segment = 0; segment < maxLengthBlocks; segment++) {

            int anchorY = worldY + segment;

            if (!volume.isHollow(localX, anchorY, localZ))
                return SubBlockUtility.MASK_EMPTY;

            if (!volume.isSolid(localX, anchorY + 1, localZ))
                continue;

            return resolveSpikeMask(
                    worldHandle, chunkCoordinate, column, volume, localX, anchorY, localZ, segment, true);
        }

        return SubBlockUtility.MASK_EMPTY;
    }

    // A stalagmite rising from the first solid floor straight below, if one rises there and reaches up this far
    private int resolveRisingMask(
            WorldHandle worldHandle,
            long chunkCoordinate,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            int localX,
            int worldY,
            int localZ) {

        for (int segment = 0; segment < maxLengthBlocks; segment++) {

            int anchorY = worldY - segment;

            if (!volume.isHollow(localX, anchorY, localZ))
                return SubBlockUtility.MASK_EMPTY;

            if (!volume.isSolid(localX, anchorY - 1, localZ))
                continue;

            return resolveSpikeMask(
                    worldHandle, chunkCoordinate, column, volume, localX, anchorY, localZ, segment, false);
        }

        return SubBlockUtility.MASK_EMPTY;
    }

    // The octants of one segment of a small spike anchored at anchorY, counted in half blocks from its base: a thick
    // spike keeps its full width for the first half of its length, then every spike runs on as a single octant
    private int resolveSpikeMask(
            WorldHandle worldHandle,
            long chunkCoordinate,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            int localX,
            int anchorY,
            int localZ,
            int segment,
            boolean hanging) {

        int columnIndex = localZ * chunkSize + localX;
        long chunkOriginX = (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize;
        long chunkOriginZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize;

        TerrainCaveProfileStruct profile = caveRegionBranch.resolveProfile(
                worldHandle, volume.chunkFrame, chunkOriginX, chunkOriginZ,
                localX + 0.5f, anchorY, localZ + 0.5f, column.groundHeightBlocks[columnIndex]);

        if (profile == null || !profile.speleothems)
            return SubBlockUtility.MASK_EMPTY;

        long spikeHash = TerrainCarveUtility.hashCell(
                worldHandle.getSeed() ^ (hanging
                        ? EngineSetting.SPELEOTHEM_STALACTITE_SEED_SALT
                        : EngineSetting.SPELEOTHEM_STALAGMITE_SEED_SALT),
                (int) (chunkOriginX + localX), anchorY, (int) (chunkOriginZ + localZ));

        if (roll(spikeHash, 0) >= (hanging ? profile.stalactites : profile.stalagmites))
            return SubBlockUtility.MASK_EMPTY;

        int lengthBlocks = Math.min(1 + (int) (roll(spikeHash, 1) * profile.maxLengthBlocks), profile.maxLengthBlocks);

        if (segment >= lengthBlocks)
            return SubBlockUtility.MASK_EMPTY;

        int lengthHalves = lengthBlocks * SubBlockUtility.DIVISIONS
                - (roll(spikeHash, 2) < EngineSetting.SPELEOTHEM_BLUNT_SHARE ? 0 : 1);
        boolean thick = roll(spikeHash, 3) < (hanging
                ? EngineSetting.SPELEOTHEM_STALACTITE_THICK_SHARE
                : EngineSetting.SPELEOTHEM_STALAGMITE_THICK_SHARE);
        int quadrants = SubBlockUtility.DIVISIONS * SubBlockUtility.DIVISIONS;
        int quadrant = Math.min((int) (roll(spikeHash, 4) * quadrants), quadrants - 1);
        int mask = SubBlockUtility.MASK_EMPTY;

        for (int layer = 0; layer < SubBlockUtility.DIVISIONS; layer++) {

            int half = segment * SubBlockUtility.DIVISIONS + layer;

            if (half >= lengthHalves)
                break;

            int octantY = hanging ? SubBlockUtility.DIVISIONS - 1 - layer : layer;

            mask |= thick && half * 2 < lengthHalves
                    ? SubBlockUtility.layerMask(octantY)
                    : SubBlockUtility.getOctantBit(SubBlockUtility.getOctant(
                            quadrant % SubBlockUtility.DIVISIONS, octantY, quadrant / SubBlockUtility.DIVISIONS));
        }

        volume.speleothemBlockID = profile.speleothemBlockID;

        return mask;
    }

    // Rock \\

    // The block rock within a cave biome's lining becomes — its rock, a vein through it, or the block of the floor,
    // ceiling or bed it forms — reporting the coverage laid over it through the volume
    short resolveRock(
            WorldHandle worldHandle,
            long chunkCoordinate,
            TerrainColumnAsyncContainer column,
            CaveVolumeAsyncContainer volume,
            int localX,
            int worldY,
            int localZ,
            short blockID,
            int mask,
            short stoneBlockID) {

        int columnIndex = localZ * chunkSize + localX;

        volume.cellCoverage = CoverageUtility.NONE;

        if (blockID != stoneBlockID && blockID != column.columnRockBlockID[columnIndex])
            return blockID;

        float density = volume.getDensity(localX, worldY, localZ);

        if (density <= -EngineSetting.CAVE_BIOME_MAX_SHELL_BLOCKS)
            return blockID;

        long chunkOriginX = (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize;
        long chunkOriginZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize;
        int groundHeight = column.groundHeightBlocks[columnIndex];

        TerrainCaveProfileStruct profile = caveRegionBranch.resolveProfile(
                worldHandle, volume.chunkFrame, chunkOriginX, chunkOriginZ,
                localX + 0.5f, worldY, localZ + 0.5f, groundHeight);

        if (profile == null || density <= -profile.shellBlocks)
            return blockID;

        short resultBlockID = worldGenerationManager.resolveVeins(
                column, profile.veins, profile.veinBlockIDs, localX, localZ, worldY,
                volume.originY + CaveVolumeAsyncContainer.MARGIN, groundHeight, profile.rockBlockID);

        if (resultBlockID != profile.rockBlockID)
            return resultBlockID;

        short coverage;
        int variance;

        if (volume.isCavity(localX, worldY + 1, localZ)) {

            if (!volume.isHollow(localX, worldY + 1, localZ))
                return profile.bedBlockID;

            resultBlockID = profile.floorBlockID;
            coverage = profile.floorCoverage;
            variance = profile.floorCoverageVariance;
        } else if (volume.isHollow(localX, worldY - 1, localZ)) {
            resultBlockID = profile.ceilingBlockID;
            coverage = profile.ceilingCoverage;
            variance = profile.ceilingCoverageVariance;
        } else if (isBesideHollow(volume, localX, worldY, localZ)) {
            coverage = profile.wallCoverage;
            variance = profile.wallCoverageVariance;
        } else
            return resultBlockID;

        if (mask == SubBlockUtility.MASK_FULL && CoverageUtility.isCovered(coverage))
            volume.cellCoverage = resolveCellCoverage(
                    worldHandle, coverage, variance, chunkOriginX + localX, worldY, chunkOriginZ + localZ);

        return resultBlockID;
    }

    private boolean isBesideHollow(CaveVolumeAsyncContainer volume, int localX, int worldY, int localZ) {
        return (localX > 0 && volume.isHollow(localX - 1, worldY, localZ))
                || (localX < chunkSize - 1 && volume.isHollow(localX + 1, worldY, localZ))
                || (localZ > 0 && volume.isHollow(localX, worldY, localZ - 1))
                || (localZ < chunkSize - 1 && volume.isHollow(localX, worldY, localZ + 1));
    }

    // A covering fallen short of its full level by up to its variance, drawn per patch of cells so neighbouring
    // faces share a level and the mesher still merges them
    private short resolveCellCoverage(
            WorldHandle worldHandle,
            short coverage,
            int variance,
            long worldX,
            int worldY,
            long worldZ) {

        if (variance == 0)
            return coverage;

        int patch = EngineSetting.BIOME_COVERING_VARIANCE_PATCH_BLOCKS;
        float shortfall = TerrainCarveUtility.rollCell(
                worldHandle.getSeed(), EngineSetting.BIOME_COVERING_VARIANCE_SALT,
                (int) Math.floorDiv(worldX, patch), Math.floorDiv(worldY, patch), (int) Math.floorDiv(worldZ, patch));

        return CoverageUtility.addLevels(coverage, -(int) (shortfall * (variance + 1)));
    }

    // Math \\

    // An independent roll in 0 to 1 per index from one hash
    private static float roll(long hash, int index) {
        return BiomeFieldUtility.hash01(hash + index * EngineSetting.CAVE_ROLL_STRIDE);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
