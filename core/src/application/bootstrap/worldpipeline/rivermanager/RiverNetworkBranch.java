package application.bootstrap.worldpipeline.rivermanager;

import java.util.Arrays;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeRiverStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.river.RiverChangeStruct;
import application.bootstrap.worldpipeline.river.RiverNetworkStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import application.bootstrap.worldpipeline.util.TideUtility;
import application.bootstrap.worldpipeline.util.WorldElevationUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class RiverNetworkBranch extends BranchPackage {

    /*
     * Async — builds and keeps each world's drainage. Every drainage cell
     * reads the biomes painted on its map pixels and the height layer under
     * them, shaped by the smooth macro fields at its heart and a gentle swell
     * that keeps even slopes from sheeting their water, and sheds its
     * biomes' runoff. A priority flood rises inland from the coast, so each
     * cell drains to the neighbour that reached it first, hollows fill to
     * their brim and every cell finds the sea; cells level with one another
     * are reached in an order drawn from a hash, so water crosses a flat
     * along branching paths rather than straight rays. Flow then gathers from
     * the highest cells down. A river's level is its cell's ground less its
     * biome's cut, lowered to the level of every river flowing into it so
     * water never climbs, and held at the highest tide or above. The first
     * network of a world is built on whichever thread first asks; a rebuild
     * after an edit runs in the background while the old network keeps
     * serving, and leaves behind the span of cells it changed.
     */

    private static final byte UNVISITED = -2;

    // Internal
    private BiomeManager biomeManager;
    private ThreadHandle rebuildThread;

    // Networks — copy-on-write, read without a lock
    private volatile Object2ObjectOpenHashMap<WorldHandle, RiverNetworkStruct> worldHandle2Network;
    private volatile ObjectArrayList<RiverNetworkStruct> networks;

    // Rebuilds — guarded by this
    private ObjectOpenHashSet<WorldHandle> rebuildingWorlds;
    private ObjectOpenHashSet<WorldHandle> dirtyWorlds;
    private ObjectArrayList<RiverChangeStruct> pendingChanges;

    // Base \\

    @Override
    protected void create() {

        // Networks
        this.worldHandle2Network = new Object2ObjectOpenHashMap<>();
        this.networks = new ObjectArrayList<>();

        // Rebuilds
        this.rebuildingWorlds = new ObjectOpenHashSet<>();
        this.dirtyWorlds = new ObjectOpenHashSet<>();
        this.pendingChanges = new ObjectArrayList<>();
    }

    @Override
    protected void get() {

        // Internal
        this.biomeManager = get(BiomeManager.class);
        this.rebuildThread = getThreadHandleFromThreadName(EngineSetting.GENERAL_THREAD_NAME);
    }

    // Network \\

    // Any thread — the world's drainage, built here the first time it is asked for
    RiverNetworkStruct getNetwork(WorldHandle worldHandle) {

        RiverNetworkStruct network = worldHandle2Network.get(worldHandle);

        if (network != null)
            return network;

        synchronized (this) {

            network = worldHandle2Network.get(worldHandle);

            if (network == null) {
                network = build(worldHandle);
                publish(worldHandle, network);
            }

            return network;
        }
    }

    // A world never asked for yet is left alone; its network is built fresh when first needed
    synchronized void requestRebuild(WorldHandle worldHandle) {

        if (!worldHandle2Network.containsKey(worldHandle))
            return;

        if (rebuildingWorlds.contains(worldHandle)) {
            dirtyWorlds.add(worldHandle);
            return;
        }

        rebuildingWorlds.add(worldHandle);
        executeAsync(rebuildThread, () -> rebuild(worldHandle));
    }

    // Every network built under biomes that have since been rebuilt, unless it is already draining again
    synchronized void refreshStale() {

        int biomeRevision = biomeManager.getRevision();

        for (int i = 0; i < networks.size(); i++) {

            RiverNetworkStruct network = networks.get(i);

            if (network.getBiomeRevision() != biomeRevision && !rebuildingWorlds.contains(network.getWorldHandle()))
                requestRebuild(network.getWorldHandle());
        }
    }

    synchronized RiverChangeStruct pollChange() {
        return pendingChanges.isEmpty() ? null : pendingChanges.remove(pendingChanges.size() - 1);
    }

    private void rebuild(WorldHandle worldHandle) {

        RiverNetworkStruct network = build(worldHandle);

        synchronized (this) {

            RiverNetworkStruct previous = worldHandle2Network.get(worldHandle);
            RiverChangeStruct change = resolveChange(previous, network);

            publish(worldHandle, network);

            if (change != null)
                pendingChanges.add(change);

            rebuildingWorlds.remove(worldHandle);

            if (dirtyWorlds.remove(worldHandle)) {
                rebuildingWorlds.add(worldHandle);
                executeAsync(rebuildThread, () -> rebuild(worldHandle));
            }
        }
    }

    private void publish(WorldHandle worldHandle, RiverNetworkStruct network) {

        Object2ObjectOpenHashMap<WorldHandle, RiverNetworkStruct> next = new Object2ObjectOpenHashMap<>(
                worldHandle2Network);
        next.put(worldHandle, network);
        worldHandle2Network = next;
        networks = new ObjectArrayList<>(next.values());
    }

    // Build \\

    private RiverNetworkStruct build(WorldHandle worldHandle) {

        long startNanos = System.nanoTime();
        int biomeRevision = biomeManager.getRevision();

        Pixmap map = worldHandle.getWorld();
        int cellPixels = EngineSetting.RIVER_CELL_PIXELS;

        if (map.getWidth() % cellPixels != 0 || map.getHeight() % cellPixels != 0)
            throwException("World \"" + worldHandle.getWorldName() + "\" is " + map.getWidth() + " by "
                    + map.getHeight() + " pixels — rivers drain over cells of " + cellPixels
                    + " pixels a side, so both sides must divide by " + cellPixels + ".");

        int cellCountX = map.getWidth() / cellPixels;
        int cellCountZ = map.getHeight() / cellPixels;
        int cellCount = cellCountX * cellCountZ;
        double cellSizeBlocks = cellPixels * EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;

        int[] fillQuanta = new int[cellCount];
        float[] flow = new float[cellCount];
        short[] levelBlocks = new short[cellCount];
        byte[] downstream = new byte[cellCount];

        sampleCells(worldHandle, cellCountX, cellCountZ, cellSizeBlocks, fillQuanta, flow, levelBlocks, downstream);

        int[] order = new int[cellCount];
        int orderCount = flood(worldHandle.getSeed(), cellCountX, cellCountZ, fillQuanta, downstream, order);

        byte[] strength = gatherFlow(cellCountX, cellCountZ, downstream, order, orderCount, flow);

        resolveLevels(cellCountX, cellCountZ, fillQuanta, downstream, strength, order, orderCount, levelBlocks);

        RiverNetworkStruct network = new RiverNetworkStruct(
                worldHandle, biomeRevision, cellCountX, cellCountZ, cellSizeBlocks,
                downstream, strength, levelBlocks, resolveNear(cellCountX, cellCountZ, downstream, strength));

        log("Rivers drained \"" + worldHandle.getWorldName() + "\": " + cellCountX + " x " + cellCountZ
                + " cells, " + countRivers(network) + " river cells, in "
                + (System.nanoTime() - startNanos) / EngineSetting.NANOS_PER_MILLI + " ms");

        return network;
    }

    // Each cell's ground, runoff and cut, averaged over its land pixels; a cell mostly sea is ocean and drains
    // nowhere. Cells hold their ground minus their cut in levelBlocks until levels are resolved
    private void sampleCells(
            WorldHandle worldHandle,
            int cellCountX,
            int cellCountZ,
            double cellSizeBlocks,
            int[] fillQuanta,
            float[] flow,
            short[] levelBlocks,
            byte[] downstream) {

        long seed = worldHandle.getSeed();
        double worldWidthBlocks = worldHandle.getWorldScale().x;
        double worldHeightBlocks = worldHandle.getWorldScale().y;
        Pixmap heightLayer = worldHandle.getHeightLayer();
        int cellPixels = EngineSetting.RIVER_CELL_PIXELS;
        int pixelsPerCell = cellPixels * cellPixels;

        for (int cellZ = 0; cellZ < cellCountZ; cellZ++) {

            double worldZ = (cellZ + 0.5) * cellSizeBlocks;

            for (int cellX = 0; cellX < cellCountX; cellX++) {

                int index = cellZ * cellCountX + cellX;
                double spatialAngle = TerrainShapeUtility.toSpatialAngle((cellX + 0.5) * cellSizeBlocks,
                        worldWidthBlocks);
                double cosAngle = Math.cos(spatialAngle);
                double sinAngle = Math.sin(spatialAngle);
                float continentalness = TerrainShapeUtility.sampleContinentalness(
                        seed, cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks);
                float erosion = TerrainShapeUtility.sampleErosion(
                        seed, cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks);
                float roughness = TerrainShapeUtility.sampleDrainageRoughness(
                        seed, cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks);

                int oceanPixels = 0;
                float heightSum = 0f;
                float runoffSum = 0f;
                int incisionSum = 0;

                for (int offsetZ = 0; offsetZ < cellPixels; offsetZ++) {
                    for (int offsetX = 0; offsetX < cellPixels; offsetX++) {

                        int pixelX = cellX * cellPixels + offsetX;
                        int pixelZ = cellZ * cellPixels + offsetZ;
                        BiomeHandle biome = biomeManager.getPaintedBiome(worldHandle, pixelX, pixelZ);

                        if (biome.hasOceanWater()) {
                            oceanPixels++;
                            continue;
                        }

                        BiomeRiverStruct river = biome.getRiver();
                        float elevation = WorldElevationUtility.toElevation(
                                WorldElevationUtility.toLevel(heightLayer.getPixel(pixelX, pixelZ)));

                        heightSum += TerrainShapeUtility.computeDrainageHeightBlocks(
                                biome, continentalness, erosion, elevation);
                        runoffSum += river.getRunoff();
                        incisionSum += river.getIncisionBlocks();
                    }
                }

                if (oceanPixels * 2 > pixelsPerCell) {
                    downstream[index] = RiverNetworkStruct.DIRECTION_NONE;
                    fillQuanta[index] = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS
                            * EngineSetting.RIVER_FILL_QUANTA_PER_BLOCK;
                    levelBlocks[index] = (short) EngineSetting.RIVER_LEVEL_NONE;
                    continue;
                }

                int landPixels = pixelsPerCell - oceanPixels;
                float heightBlocks = heightSum / landPixels + roughness * EngineSetting.RIVER_ROUGHNESS_BLOCKS;

                downstream[index] = UNVISITED;
                fillQuanta[index] = Math.max(0, Math.round(heightBlocks * EngineSetting.RIVER_FILL_QUANTA_PER_BLOCK));
                flow[index] = runoffSum / landPixels;
                levelBlocks[index] = (short) ((int) Math.floor(heightBlocks) - incisionSum / landPixels);
            }
        }
    }

    // Rises inland from every coastal sea cell, lowest ground first; each land cell drains to the neighbour that
    // reached it, a filled hollow lying level with its brim. A world with no sea drains to its lowest cell. Returns
    // how many land cells were ordered, each after the cell it drains into
    private int flood(long seed, int cellCountX, int cellCountZ, int[] fillQuanta, byte[] downstream, int[] order) {

        LongHeapPriorityQueue queue = new LongHeapPriorityQueue();
        int directions = RiverNetworkStruct.getDirectionCount();
        int lowestLand = RiverNetworkStruct.INDEX_NONE;

        for (int index = 0; index < downstream.length; index++) {

            if (downstream[index] == UNVISITED) {

                if (lowestLand == RiverNetworkStruct.INDEX_NONE || fillQuanta[index] < fillQuanta[lowestLand])
                    lowestLand = index;

                continue;
            }

            if (bordersLand(cellCountX, cellCountZ, downstream, index))
                queue.enqueue(packEntry(seed, cellCountX, fillQuanta[index], index));
        }

        if (queue.isEmpty() && lowestLand != RiverNetworkStruct.INDEX_NONE) {
            downstream[lowestLand] = RiverNetworkStruct.DIRECTION_NONE;
            queue.enqueue(packEntry(seed, cellCountX, fillQuanta[lowestLand], lowestLand));
        }

        int orderCount = 0;

        while (!queue.isEmpty()) {

            long entry = queue.dequeueLong();
            int index = (int) entry;
            int quanta = fillQuanta[index];
            int cellX = index % cellCountX;
            int cellZ = index / cellCountX;

            if (!isSeaSeed(downstream, index, lowestLand))
                order[orderCount++] = index;

            for (int direction = 0; direction < directions; direction++) {

                int neighbour = wrapIndex(cellCountX, cellCountZ,
                        cellX + RiverNetworkStruct.getDirectionX(direction),
                        cellZ + RiverNetworkStruct.getDirectionZ(direction));

                if (downstream[neighbour] != UNVISITED)
                    continue;

                downstream[neighbour] = (byte) ((direction + directions / 2) % directions);
                fillQuanta[neighbour] = Math.max(fillQuanta[neighbour], quanta);
                queue.enqueue(packEntry(seed, cellCountX, fillQuanta[neighbour], neighbour));
            }
        }

        return orderCount;
    }

    // Each land cell's runoff passed down to the cell it drains into, highest first; a cell whose gathered flow
    // passes the threshold carries a river, its strength one step per doubling beyond it
    private byte[] gatherFlow(
            int cellCountX,
            int cellCountZ,
            byte[] downstream,
            int[] order,
            int orderCount,
            float[] flow) {

        byte[] strength = new byte[downstream.length];
        float threshold = EngineSetting.RIVER_DISCHARGE_THRESHOLD;
        int maxStrength = 0xFF;

        for (int i = orderCount - 1; i >= 0; i--) {

            int index = order[i];
            int target = resolveDownstream(cellCountX, cellCountZ, downstream, index);

            if (flow[index] >= threshold) {

                double doublings = Math.log(flow[index] / threshold) / Math.log(2.0);
                int steps = 1 + (int) Math.round(doublings * EngineSetting.RIVER_STRENGTH_STEPS_PER_DOUBLING);

                strength[index] = (byte) Math.min(maxStrength, steps);
            }

            if (target != RiverNetworkStruct.INDEX_NONE)
                flow[target] += flow[index];
        }

        return strength;
    }

    // Upstream first: a river keeps the lower of its own level and that of every river flowing into it, never below
    // the highest tide, so its surface only ever falls toward the sea
    private void resolveLevels(
            int cellCountX,
            int cellCountZ,
            int[] inflowLevels,
            byte[] downstream,
            byte[] strength,
            int[] order,
            int orderCount,
            short[] levelBlocks) {

        int capBlocks = TideUtility.BAND_MAX_Y;

        Arrays.fill(inflowLevels, Integer.MAX_VALUE);

        for (int i = orderCount - 1; i >= 0; i--) {

            int index = order[i];

            if (strength[index] == 0) {
                levelBlocks[index] = (short) EngineSetting.RIVER_LEVEL_NONE;
                continue;
            }

            int level = Math.max(capBlocks, Math.min(levelBlocks[index], inflowLevels[index]));
            int target = resolveDownstream(cellCountX, cellCountZ, downstream, index);

            levelBlocks[index] = (short) level;

            if (target != RiverNetworkStruct.INDEX_NONE)
                inflowLevels[target] = Math.min(inflowLevels[target], level);
        }
    }

    // Every cell within reach of a river cell, as one bit each
    private long[] resolveNear(int cellCountX, int cellCountZ, byte[] downstream, byte[] strength) {

        long[] nearWords = new long[(downstream.length + Long.SIZE - 1) / Long.SIZE];
        int radius = RiverNetworkStruct.NEAR_RADIUS_CELLS;

        for (int index = 0; index < downstream.length; index++) {

            if (strength[index] == 0 || downstream[index] == RiverNetworkStruct.DIRECTION_NONE)
                continue;

            int cellX = index % cellCountX;
            int cellZ = index / cellCountX;

            for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                for (int offsetX = -radius; offsetX <= radius; offsetX++) {

                    int near = wrapIndex(cellCountX, cellCountZ, cellX + offsetX, cellZ + offsetZ);
                    nearWords[near >>> 6] |= 1L << (near & 63);
                }
            }
        }

        return nearWords;
    }

    // Change \\

    // The cells whose drainage differs between two networks of one world, widened by a river's reach
    private RiverChangeStruct resolveChange(RiverNetworkStruct previous, RiverNetworkStruct next) {

        if (previous == null)
            return null;

        int minCellX = Integer.MAX_VALUE;
        int minCellZ = Integer.MAX_VALUE;
        int maxCellX = Integer.MIN_VALUE;
        int maxCellZ = Integer.MIN_VALUE;

        for (int index = 0; index < next.getCellCount(); index++) {

            if (previous.getDownstreamIndex(index) == next.getDownstreamIndex(index)
                    && previous.getStrength(index) == next.getStrength(index)
                    && previous.getLevelBlocks(index) == next.getLevelBlocks(index))
                continue;

            int cellX = next.getCellX(index);
            int cellZ = next.getCellZ(index);

            minCellX = Math.min(minCellX, cellX);
            minCellZ = Math.min(minCellZ, cellZ);
            maxCellX = Math.max(maxCellX, cellX);
            maxCellZ = Math.max(maxCellZ, cellZ);
        }

        if (minCellX == Integer.MAX_VALUE)
            return null;

        int radius = RiverNetworkStruct.NEAR_RADIUS_CELLS;

        return new RiverChangeStruct(
                next.getWorldHandle(), minCellX - radius, minCellZ - radius, maxCellX + radius, maxCellZ + radius);
    }

    // Utility \\

    private int countRivers(RiverNetworkStruct network) {

        int count = 0;

        for (int index = 0; index < network.getCellCount(); index++)
            if (network.isRiver(index))
                count++;

        return count;
    }

    private boolean bordersLand(int cellCountX, int cellCountZ, byte[] downstream, int index) {

        int cellX = index % cellCountX;
        int cellZ = index / cellCountX;

        for (int direction = 0; direction < RiverNetworkStruct.getDirectionCount(); direction++) {

            int neighbour = wrapIndex(cellCountX, cellCountZ,
                    cellX + RiverNetworkStruct.getDirectionX(direction),
                    cellZ + RiverNetworkStruct.getDirectionZ(direction));

            if (downstream[neighbour] == UNVISITED)
                return true;
        }

        return false;
    }

    // A sea cell seeds the flood but is not land, so it takes no place in the order; a sealess world's lowest cell
    // is land and does
    private boolean isSeaSeed(byte[] downstream, int index, int lowestLand) {
        return downstream[index] == RiverNetworkStruct.DIRECTION_NONE && index != lowestLand;
    }

    private int resolveDownstream(int cellCountX, int cellCountZ, byte[] downstream, int index) {

        int direction = downstream[index];

        if (direction == RiverNetworkStruct.DIRECTION_NONE)
            return RiverNetworkStruct.INDEX_NONE;

        return wrapIndex(cellCountX, cellCountZ,
                index % cellCountX + RiverNetworkStruct.getDirectionX(direction),
                index / cellCountX + RiverNetworkStruct.getDirectionZ(direction));
    }

    private int wrapIndex(int cellCountX, int cellCountZ, int cellX, int cellZ) {
        return Math.floorMod(cellZ, cellCountZ) * cellCountX + Math.floorMod(cellX, cellCountX);
    }

    // Lowest ground first, cells level with one another in the order their hash draws, the index in the low bits
    private long packEntry(long seed, int cellCountX, int quanta, int index) {

        long hash = BiomeFieldUtility.hashCell(seed ^ EngineSetting.RIVER_FLAT_SALT,
                index % cellCountX, index / cellCountX);
        long tie = (long) (BiomeFieldUtility.hash01(hash) * (1 << EngineSetting.RIVER_FLAT_TIE_BITS));

        return ((long) quanta << (Integer.SIZE + EngineSetting.RIVER_FLAT_TIE_BITS)) | (tie << Integer.SIZE) | index;
    }
}
