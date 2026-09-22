package application.bootstrap.worldpipeline.structuremanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.structure.StructureCacheStruct;
import application.bootstrap.worldpipeline.structure.StructureElevationType;
import application.bootstrap.worldpipeline.structure.StructureGeometryUtility;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structure.StructureLocationStruct;
import application.bootstrap.worldpipeline.structure.StructurePlacementStruct;
import application.bootstrap.worldpipeline.structure.StructureSpawnData;
import application.bootstrap.worldpipeline.structure.StructureTemplateData;
import application.bootstrap.worldpipeline.structure.StructureTemplatePieceStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StructurePlacementBranch extends BranchPackage {

    /*
     * Decides where every structure is, as a pure function of the world
     * seed, so any worker resolving any chunk agrees without coordination.
     *
     * Each self-placing definition tiles the world with its own grid of
     * spawn cells, sized as close to its spacing as divides the world
     * evenly so the grid wraps exactly at the seam. A cell rolls its
     * frequency once, jitters a position inside itself, and keeps a
     * candidate only if the local rules hold there — allowed biome, height
     * range, flat enough ground, dry unless water is allowed. Hand-authored
     * locations become candidates unconditionally. A candidate is then
     * valid only if no valid candidate that outranks it overlaps it;
     * because a candidate only ever defers to strictly higher-ranked ones,
     * that recursion always terminates, and its result is memoized.
     *
     * The same machinery answers the road network's question: every
     * connectable placement links to its nearest connectable neighbours,
     * and a link exists when either end picked the other.
     */

    // Internal
    private StructureManager structureManager;
    private WorldGenerationManager worldGenerationManager;

    // Caches — every value is a pure function of its key for the current world
    private StructureCacheStruct<StructurePlacementStruct> candidateCache;
    private StructureCacheStruct<StructurePlacementStruct> placementCache;
    private StructureCacheStruct<ObjectArrayList<StructurePlacementStruct>> neighbourCache;

    private volatile int cachedWorldID = Integer.MIN_VALUE;

    private static final long HANDCRAFTED_FLAG = 1L << 63;

    // Base \\

    @Override
    protected void create() {
        this.candidateCache = new StructureCacheStruct<>(EngineSetting.STRUCTURE_PLAN_CACHE_LIMIT * 4);
        this.placementCache = new StructureCacheStruct<>(EngineSetting.STRUCTURE_PLAN_CACHE_LIMIT * 4);
        this.neighbourCache = new StructureCacheStruct<>(EngineSetting.STRUCTURE_PLAN_CACHE_LIMIT);
    }

    @Override
    protected void get() {
        this.structureManager = get(StructureManager.class);
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // World Binding \\

    /*
     * Caches key by placement identity, not by world. A different world
     * loaded into the same session invalidates everything, including the
     * layout and road caches that hang off these placements.
     */
    boolean bindWorld(WorldHandle worldHandle) {

        if (cachedWorldID == worldHandle.getWorldID())
            return false;

        synchronized (this) {

            if (cachedWorldID == worldHandle.getWorldID())
                return false;

            candidateCache.clear();
            placementCache.clear();
            neighbourCache.clear();
            cachedWorldID = worldHandle.getWorldID();

            return true;
        }
    }

    // Queries \\

    /*
     * Every valid placement whose bounding circle comes within reach of
     * (centerX, centerZ), in a fixed order: catalogue order, then cell
     * order, then hand-authored locations. Callers rely on that order being
     * identical on every thread.
     */
    void collectPlacements(
            WorldHandle worldHandle,
            double centerX, double centerZ, double reach,
            ObjectArrayList<StructurePlacementStruct> out) {

        StructureHandle[] handles = structureManager.getPlaceableHandles();

        for (int i = 0; i < handles.length; i++)
            collectForStructure(worldHandle, handles[i], centerX, centerZ, reach, true, out);
    }

    /*
     * The placements this one links to by its own choice — its nearest
     * connectable neighbours inside its link distance. Not symmetric; the
     * region builder unions both directions.
     */
    ObjectArrayList<StructurePlacementStruct> getRoadNeighbours(
            WorldHandle worldHandle,
            StructurePlacementStruct placement) {

        return neighbourCache.get(placement.getPlacementID(), key -> resolveRoadNeighbours(worldHandle, placement));
    }

    int getMaxRoadLinkDistance() {

        int maxDistance = 0;

        for (StructureHandle handle : structureManager.getPlaceableHandles())
            if (handle.connectsToRoads())
                maxDistance = Math.max(maxDistance, handle.getRoadLinkData().getMaxDistanceBlocks());

        return maxDistance;
    }

    // Enumeration \\

    private void collectForStructure(
            WorldHandle worldHandle,
            StructureHandle handle,
            double centerX, double centerZ, double reach,
            boolean validOnly,
            ObjectArrayList<StructurePlacementStruct> out) {

        double worldWidth = worldHandle.getWorldScale().x;
        double worldHeight = worldHandle.getWorldScale().y;
        double searchReach = reach + handle.getBoundingRadiusBlocks();

        StructureSpawnData spawn = handle.getSpawnData();

        if (spawn != null) {

            int cellsX = cellCount(worldWidth, spawn.getSpacingBlocks());
            int cellsZ = cellCount(worldHeight, spawn.getSpacingBlocks());
            double cellSizeX = worldWidth / cellsX;
            double cellSizeZ = worldHeight / cellsZ;

            int lowCellX = (int) Math.floor((centerX - searchReach) / cellSizeX);
            int highCellX = (int) Math.floor((centerX + searchReach) / cellSizeX);
            int lowCellZ = (int) Math.floor((centerZ - searchReach) / cellSizeZ);
            int highCellZ = (int) Math.floor((centerZ + searchReach) / cellSizeZ);

            // A search wider than the world would visit a cell twice.
            if (highCellX - lowCellX >= cellsX) {
                lowCellX = 0;
                highCellX = cellsX - 1;
            }

            if (highCellZ - lowCellZ >= cellsZ) {
                lowCellZ = 0;
                highCellZ = cellsZ - 1;
            }

            for (int cellZ = lowCellZ; cellZ <= highCellZ; cellZ++) {
                for (int cellX = lowCellX; cellX <= highCellX; cellX++) {

                    int wrappedCellX = Math.floorMod(cellX, cellsX);
                    int wrappedCellZ = Math.floorMod(cellZ, cellsZ);
                    long key = cellKey(handle, wrappedCellX, wrappedCellZ);

                    StructurePlacementStruct placement = validOnly
                            ? getValidPlacement(worldHandle, handle, key, wrappedCellX, wrappedCellZ,
                                    cellSizeX, cellSizeZ, -1)
                            : getCandidate(worldHandle, handle, key, wrappedCellX, wrappedCellZ,
                                    cellSizeX, cellSizeZ, -1);

                    addIfWithin(worldHandle, placement, centerX, centerZ, reach, out);
                }
            }
        }

        ObjectArrayList<StructureLocationStruct> locations = handle.getLocations();

        for (int i = 0; i < locations.size(); i++) {

            long key = HANDCRAFTED_FLAG | ((long) handle.getStructureID() << 32) | i;

            StructurePlacementStruct placement = validOnly
                    ? getValidPlacement(worldHandle, handle, key, 0, 0, 0, 0, i)
                    : getCandidate(worldHandle, handle, key, 0, 0, 0, 0, i);

            addIfWithin(worldHandle, placement, centerX, centerZ, reach, out);
        }
    }

    private void addIfWithin(
            WorldHandle worldHandle,
            StructurePlacementStruct placement,
            double centerX, double centerZ, double reach,
            ObjectArrayList<StructurePlacementStruct> out) {

        if (placement == null)
            return;

        double distance = wrappedDistance(worldHandle, placement.getX(), placement.getZ(), centerX, centerZ);

        if (distance <= reach + placement.getRadiusBlocks())
            out.add(placement);
    }

    // Validity \\

    private StructurePlacementStruct getValidPlacement(
            WorldHandle worldHandle,
            StructureHandle handle,
            long key,
            int cellX, int cellZ,
            double cellSizeX, double cellSizeZ,
            int locationIndex) {

        return placementCache.get(key, k -> {

            StructurePlacementStruct candidate = getCandidate(
                    worldHandle, handle, key, cellX, cellZ, cellSizeX, cellSizeZ, locationIndex);

            if (candidate == null || isOutranked(worldHandle, candidate))
                return null;

            return candidate;
        });
    }

    /*
     * Whether any valid candidate that outranks this one overlaps it.
     * Only higher-ranked candidates are ever consulted, and those in turn
     * only consult ones ranked higher still, so the recursion is finite and
     * acquires cache entries in rank order — which is also what keeps two
     * workers resolving overlapping candidates from deadlocking.
     */
    private boolean isOutranked(WorldHandle worldHandle, StructurePlacementStruct candidate) {

        ObjectArrayList<StructurePlacementStruct> rivals = new ObjectArrayList<>();
        StructureHandle[] handles = structureManager.getPlaceableHandles();
        double reach = candidate.getRadiusBlocks() + EngineSetting.STRUCTURE_OVERLAP_PADDING_BLOCKS;

        for (int i = 0; i < handles.length; i++)
            collectForStructure(worldHandle, handles[i], candidate.getX(), candidate.getZ(), reach, false, rivals);

        for (int i = 0; i < rivals.size(); i++) {

            StructurePlacementStruct rival = rivals.get(i);

            if (rival.getPlacementID() == candidate.getPlacementID() || !rival.outranks(candidate))
                continue;

            if (isValid(worldHandle, rival))
                return true;
        }

        return false;
    }

    private boolean isValid(WorldHandle worldHandle, StructurePlacementStruct candidate) {

        long key = candidate.getPlacementID();

        return placementCache.get(key, k -> isOutranked(worldHandle, candidate) ? null : candidate) != null;
    }

    // Candidates \\

    private StructurePlacementStruct getCandidate(
            WorldHandle worldHandle,
            StructureHandle handle,
            long key,
            int cellX, int cellZ,
            double cellSizeX, double cellSizeZ,
            int locationIndex) {

        return candidateCache.get(key, k -> locationIndex >= 0
                ? resolveHandcrafted(worldHandle, handle, key, handle.getLocations().get(locationIndex))
                : resolveProcedural(worldHandle, handle, key, cellX, cellZ, cellSizeX, cellSizeZ));
    }

    private StructurePlacementStruct resolveProcedural(
            WorldHandle worldHandle,
            StructureHandle handle,
            long key,
            int cellX, int cellZ,
            double cellSizeX, double cellSizeZ) {

        StructureSpawnData spawn = handle.getSpawnData();
        long cellHash = StructureGeometryUtility.hash(
                worldHandle.getSeed() ^ EngineSetting.STRUCTURE_PLACEMENT_SEED, handle.getStructureID(), cellX, cellZ);

        if (StructureGeometryUtility.hash01(cellHash) >= spawn.getFrequency())
            return null;

        double x = Math.floor((cellX + StructureGeometryUtility.hash01(cellHash ^ 0x1L)) * cellSizeX) + 0.5;
        double z = Math.floor((cellZ + StructureGeometryUtility.hash01(cellHash ^ 0x2L)) * cellSizeZ) + 0.5;
        int rotation = StructureGeometryUtility.hashRange(cellHash ^ 0x3L, 0, 3);

        TerrainSampleStruct sample = new TerrainSampleStruct();

        BiomeHandle biome = worldGenerationManager.sampleDominantBiome(worldHandle, x, z, sample);

        if (!spawn.allowsBiome(biome.getBiomeName()))
            return null;

        long groundX = (long) Math.floor(x);
        long groundZ = (long) Math.floor(z);

        if (!spawn.allowsWater()
                && worldGenerationManager.sampleGroundHeightBlocks(worldHandle, groundX, groundZ, sample)
                        < EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS
                && worldGenerationManager.sampleOceanWater(worldHandle, groundX, groundZ, sample))
            return null;

        int[] footprintHeights = sampleFootprint(worldHandle, handle, x, z, rotation, sample);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long sum = 0;

        for (int height : footprintHeights) {
            min = Math.min(min, height);
            max = Math.max(max, height);
            sum += height;
        }

        if (max - min > spawn.getMaxSlopeBlocks())
            return null;

        int surface = handle.getStructureType().isLayout()
                ? footprintHeights[0]
                : Math.round(sum / (float) footprintHeights.length);

        if (surface < spawn.getMinHeightBlocks() || surface > spawn.getMaxHeightBlocks())
            return null;

        int depth = 0;
        int y = surface;

        if (handle.getElevationType() == StructureElevationType.UNDERGROUND) {
            depth = StructureGeometryUtility.hashRange(
                    cellHash ^ 0x4L, spawn.getMinDepthBlocks(), spawn.getMaxDepthBlocks());
            y = surface - depth;
        }

        return new StructurePlacementStruct(key, handle, x, z, y, rotation, false, depth);
    }

    private StructurePlacementStruct resolveHandcrafted(
            WorldHandle worldHandle,
            StructureHandle handle,
            long key,
            StructureLocationStruct location) {

        double worldWidth = worldHandle.getWorldScale().x;
        double worldHeight = worldHandle.getWorldScale().y;

        double x = Math.floor(Math.floorMod((long) Math.floor(location.getX()), (long) worldWidth)) + 0.5;
        double z = Math.floor(Math.floorMod((long) Math.floor(location.getZ()), (long) worldHeight)) + 0.5;

        long locationHash = StructureGeometryUtility.hash(
                worldHandle.getSeed() ^ EngineSetting.STRUCTURE_PLACEMENT_SEED, key, 0, 0);

        int rotation = location.getRotation() == StructureLocationStruct.ROTATION_RANDOM
                ? StructureGeometryUtility.hashRange(locationHash, 0, 3)
                : location.getRotation();

        TerrainSampleStruct sample = new TerrainSampleStruct();
        int surface = worldGenerationManager.sampleSurfaceHeightBlocks(
                worldHandle, (long) Math.floor(x), (long) Math.floor(z), sample);

        int depth = 0;
        int y = location.hasY() ? location.getY() : surface;

        if (!location.hasY() && handle.getElevationType() == StructureElevationType.UNDERGROUND) {

            StructureSpawnData spawn = handle.getSpawnData();
            int minDepth = spawn != null ? spawn.getMinDepthBlocks() : EngineSetting.STRUCTURE_DEFAULT_MIN_DEPTH_BLOCKS;
            int maxDepth = spawn != null ? spawn.getMaxDepthBlocks() : EngineSetting.STRUCTURE_DEFAULT_MAX_DEPTH_BLOCKS;

            depth = StructureGeometryUtility.hashRange(locationHash ^ 0x4L, minDepth, maxDepth);
            y = surface - depth;
        } else if (location.hasY()) {
            depth = Math.max(0, surface - y);
        }

        return new StructurePlacementStruct(key, handle, x, z, y, rotation, true, depth);
    }

    /*
     * Surface heights under a candidate. Element 0 is always the placement
     * point itself. A template samples the four corners of its rotated
     * footprint; a layout, too big to be judged by its corners, samples a
     * ring at half its radius.
     */
    private int[] sampleFootprint(
            WorldHandle worldHandle,
            StructureHandle handle,
            double x, double z,
            int rotation,
            TerrainSampleStruct sample) {

        long centerX = (long) Math.floor(x);
        long centerZ = (long) Math.floor(z);

        int[] heights = new int[5];
        heights[0] = worldGenerationManager.sampleSurfaceHeightBlocks(worldHandle, centerX, centerZ, sample);

        long[] offsetsX = new long[4];
        long[] offsetsZ = new long[4];

        StructureTemplateData template = handle.getTemplateData();

        if (template != null) {

            int footprintX = StructureTemplatePieceStruct.footprintX(template, rotation);
            int footprintZ = StructureTemplatePieceStruct.footprintZ(template, rotation);
            int anchorX = StructureTemplatePieceStruct.footprintXOf(
                    template, rotation, template.getAnchorX(), template.getAnchorZ());
            int anchorZ = StructureTemplatePieceStruct.footprintZOf(
                    template, rotation, template.getAnchorX(), template.getAnchorZ());

            offsetsX[0] = -anchorX;
            offsetsZ[0] = -anchorZ;
            offsetsX[1] = footprintX - 1 - anchorX;
            offsetsZ[1] = -anchorZ;
            offsetsX[2] = -anchorX;
            offsetsZ[2] = footprintZ - 1 - anchorZ;
            offsetsX[3] = footprintX - 1 - anchorX;
            offsetsZ[3] = footprintZ - 1 - anchorZ;
        } else {

            long ring = Math.max(1, handle.getLayoutData().getRadiusBlocks() / 2);

            offsetsX[0] = ring;
            offsetsX[1] = -ring;
            offsetsZ[2] = ring;
            offsetsZ[3] = -ring;
        }

        for (int i = 0; i < 4; i++)
            heights[i + 1] = worldGenerationManager.sampleSurfaceHeightBlocks(
                    worldHandle, centerX + offsetsX[i], centerZ + offsetsZ[i], sample);

        return heights;
    }

    // Road Neighbours \\

    private ObjectArrayList<StructurePlacementStruct> resolveRoadNeighbours(
            WorldHandle worldHandle,
            StructurePlacementStruct placement) {

        ObjectArrayList<StructurePlacementStruct> neighbours = new ObjectArrayList<>();

        if (!placement.getStructureHandle().connectsToRoads())
            return neighbours;

        int maxDistance = placement.getStructureHandle().getRoadLinkData().getMaxDistanceBlocks();
        int maxConnections = placement.getStructureHandle().getRoadLinkData().getMaxConnections();

        ObjectArrayList<StructurePlacementStruct> nearby = new ObjectArrayList<>();
        collectPlacements(worldHandle, placement.getX(), placement.getZ(), maxDistance, nearby);

        ObjectArrayList<StructurePlacementStruct> connectable = new ObjectArrayList<>();
        it.unimi.dsi.fastutil.doubles.DoubleArrayList distances = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();

        for (int i = 0; i < nearby.size(); i++) {

            StructurePlacementStruct other = nearby.get(i);

            if (other.getPlacementID() == placement.getPlacementID() || !other.getStructureHandle().connectsToRoads())
                continue;

            double distance = wrappedDistance(worldHandle, placement.getX(), placement.getZ(), other.getX(), other.getZ());

            if (distance > maxDistance)
                continue;

            connectable.add(other);
            distances.add(distance);
        }

        // Selection by distance, ties broken by ID so the choice never depends on enumeration order.
        for (int picked = 0; picked < maxConnections && !connectable.isEmpty(); picked++) {

            int best = 0;

            for (int i = 1; i < connectable.size(); i++) {

                double distance = distances.getDouble(i);
                double bestDistance = distances.getDouble(best);

                if (distance < bestDistance || (distance == bestDistance
                        && connectable.get(i).getPlacementID() < connectable.get(best).getPlacementID()))
                    best = i;
            }

            neighbours.add(connectable.remove(best));
            distances.removeDouble(best);
        }

        return neighbours;
    }

    // Math \\

    private static int cellCount(double worldSize, int spacingBlocks) {
        return Math.max(1, (int) Math.round(worldSize / spacingBlocks));
    }

    private static long cellKey(StructureHandle handle, int cellX, int cellZ) {
        return ((long) handle.getStructureID() << 48) | ((long) (cellX & 0xFFFFFF) << 24) | (cellZ & 0xFFFFFF);
    }

    double wrappedDistance(WorldHandle worldHandle, double ax, double az, double bx, double bz) {

        double dx = StructureGeometryUtility.wrapDelta(bx - ax, worldHandle.getWorldScale().x);
        double dz = StructureGeometryUtility.wrapDelta(bz - az, worldHandle.getWorldScale().y);

        return Math.sqrt(dx * dx + dz * dz);
    }
}
