package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import application.bootstrap.worldpipeline.architecturemanager.ArchitectureManager;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.settlement.SettlementHandle;
import application.bootstrap.worldpipeline.settlement.SettlementSiteStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

class SettlementSiteBranch extends BranchPackage {

    /*
     * Async — settles whether a settlement stands in one settlement cell, and
     * what it is. A settlement placed by hand in the cell always stands, of
     * the type it names, in the architecture it names or else the one its
     * biome builds it in, ignoring every rule. Otherwise, where the world
     * grows settlements on its own, the cell rolls once whether one stands,
     * its centre falls a margin inside the cell, its biome picks one of the
     * architectures it allows, the architecture picks one of the settlement
     * types it builds by their weights, and the ground must be dry, within the
     * type's heights and level enough across its streets. Every roll is a
     * pure function of the world's seed and the cell.
     */

    // Internal
    private WorldManager worldManager;
    private BiomeManager biomeManager;
    private ArchitectureManager architectureManager;
    private LayoutManager layoutManager;
    private SettlementManager settlementManager;
    private SettlementSiteAsyncContainer siteContainer;

    // Base \\

    @Override
    protected void create() {
        this.siteContainer = create(SettlementSiteAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.architectureManager = get(ArchitectureManager.class);
        this.layoutManager = get(LayoutManager.class);
        this.settlementManager = get(SettlementManager.class);
    }

    // Site \\

    // The settlement standing in a cell, null when none does
    SettlementSiteStruct resolveSite(WorldHandle worldHandle, int cellX, int cellZ) {

        WorldPlacementStruct[] placements = worldManager.getPlacements(worldHandle);

        for (int i = 0; i < placements.length; i++)
            if (placements[i].getKind() == WorldPlacementKind.SETTLEMENT
                    && isInCell(worldHandle, placements[i], cellX, cellZ))
                return resolvePlacedSite(worldHandle, placements[i], cellX, cellZ);

        if (!worldHandle.hasNaturalSettlements())
            return null;

        return resolveNaturalSite(worldHandle, cellX, cellZ);
    }

    private boolean isInCell(WorldHandle worldHandle, WorldPlacementStruct placement, int cellX, int cellZ) {

        int cellSize = EngineSetting.SETTLEMENT_CELL_SIZE_BLOCKS;

        return WorldWrapUtility.wrapBlockX(worldHandle, placement.getWorldX()) / cellSize == cellX
                && WorldWrapUtility.wrapBlockZ(worldHandle, placement.getWorldZ()) / cellSize == cellZ;
    }

    // Placed \\

    private SettlementSiteStruct resolvePlacedSite(
            WorldHandle worldHandle,
            WorldPlacementStruct placement,
            int cellX,
            int cellZ) {

        if (!settlementManager.hasSettlement(placement.getName()))
            throwException("World \"" + worldHandle.getWorldName() + "\" places settlement \"" + placement.getName()
                    + "\", which no settlement file defines.");

        SettlementHandle settlementHandle = settlementManager.getSettlementHandleFromSettlementName(
                placement.getName());
        long centerX = WorldWrapUtility.wrapBlockX(worldHandle, placement.getWorldX());
        long centerZ = WorldWrapUtility.wrapBlockZ(worldHandle, placement.getWorldZ());

        ArchitectureHandle architectureHandle = placement.hasArchitecture()
                ? resolveArchitecture(placement.getArchitectureName(), worldHandle.getWorldName())
                : resolvePlacedArchitecture(worldHandle, settlementHandle, centerX, centerZ);

        return new SettlementSiteStruct(
                settlementHandle, architectureHandle, cellX, cellZ, centerX, centerZ,
                resolveSeed(worldHandle, cellX, cellZ), true);
    }

    // The first architecture the biome allows that builds the type, else the first of all that does
    private ArchitectureHandle resolvePlacedArchitecture(
            WorldHandle worldHandle,
            SettlementHandle settlementHandle,
            long centerX,
            long centerZ) {

        ObjectList<String> allowed = sampleArchitectureNames(worldHandle, centerX, centerZ);

        for (int i = 0; i < allowed.size(); i++) {

            ArchitectureHandle candidate = resolveArchitecture(allowed.get(i), worldHandle.getWorldName());

            if (candidate.buildsSettlement(settlementHandle.getSettlementID()))
                return candidate;
        }

        ObjectArrayList<ArchitectureHandle> architectures = architectureManager.getArchitectureHandles();

        for (int i = 0; i < architectures.size(); i++)
            if (architectures.get(i).buildsSettlement(settlementHandle.getSettlementID()))
                return architectures.get(i);

        return throwException("Settlement \"" + settlementHandle.getSettlementName()
                + "\" was placed by hand, but no architecture builds it.");
    }

    // Natural \\

    private SettlementSiteStruct resolveNaturalSite(WorldHandle worldHandle, int cellX, int cellZ) {

        long seed = worldHandle.getSeed() ^ EngineSetting.SETTLEMENT_SITE_SALT;

        if (roll(seed, cellX, cellZ, EngineSetting.SETTLEMENT_CHANCE_SALT) >= EngineSetting.SETTLEMENT_CELL_CHANCE)
            return null;

        int cellSize = EngineSetting.SETTLEMENT_CELL_SIZE_BLOCKS;
        int margin = EngineSetting.SETTLEMENT_CELL_MARGIN_BLOCKS;
        int usable = cellSize - 2 * margin;
        long centerX = (long) cellX * cellSize + margin + Math.min(usable - 1,
                (int) (roll(seed, cellX, cellZ, EngineSetting.SETTLEMENT_OFFSET_X_SALT) * usable));
        long centerZ = (long) cellZ * cellSize + margin + Math.min(usable - 1,
                (int) (roll(seed, cellX, cellZ, EngineSetting.SETTLEMENT_OFFSET_Z_SALT) * usable));

        ObjectList<String> allowed = sampleArchitectureNames(worldHandle, centerX, centerZ);

        if (allowed.isEmpty())
            return null;

        int pick = Math.min(allowed.size() - 1,
                (int) (roll(seed, cellX, cellZ, EngineSetting.SETTLEMENT_ARCHITECTURE_SALT) * allowed.size()));
        ArchitectureHandle architectureHandle = resolveArchitecture(allowed.get(pick), worldHandle.getWorldName());
        SettlementHandle settlementHandle = settlementManager.drawSettlement(
                architectureHandle, roll(seed, cellX, cellZ, EngineSetting.SETTLEMENT_TYPE_SALT));

        if (settlementHandle == null || !standsOnGround(worldHandle, settlementHandle, centerX, centerZ))
            return null;

        return new SettlementSiteStruct(
                settlementHandle, architectureHandle, cellX, cellZ, centerX, centerZ,
                resolveSeed(worldHandle, cellX, cellZ), false);
    }

    // True when the centre is dry within the type's heights and the ground across its radius level enough
    private boolean standsOnGround(
            WorldHandle worldHandle,
            SettlementHandle settlementHandle,
            long centerX,
            long centerZ) {

        int center = layoutManager.probeGround(worldHandle, centerX, centerZ);

        if (center == EngineSetting.LAYOUT_GROUND_FLOODED
                || center < settlementHandle.getMinGroundHeightBlocks()
                || center > settlementHandle.getMaxGroundHeightBlocks())
            return false;

        double reach = settlementHandle.getRadiusBlocks() * EngineSetting.SETTLEMENT_SLOPE_PROBE_FRACTION;
        int lowest = center;
        int highest = center;

        for (int i = 0; i < EngineSetting.SETTLEMENT_SLOPE_PROBES; i++) {

            double angle = Math.PI * 2.0 * i / EngineSetting.SETTLEMENT_SLOPE_PROBES;
            int ground = layoutManager.probeGround(
                    worldHandle, centerX + Math.cos(angle) * reach, centerZ + Math.sin(angle) * reach);

            if (ground == EngineSetting.LAYOUT_GROUND_FLOODED)
                return false;

            lowest = Math.min(lowest, ground);
            highest = Math.max(highest, ground);
        }

        return highest - lowest <= settlementHandle.getMaxSlopeBlocks();
    }

    // Utility \\

    // An architecture a biome or placement names, refused clearly when no architecture file defines it
    private ArchitectureHandle resolveArchitecture(String architectureName, String worldName) {

        if (!architectureManager.hasArchitecture(architectureName))
            throwException("World \"" + worldName + "\" asks for architecture \"" + architectureName
                    + "\" through a biome or placement, but no architecture file defines it.");

        return architectureManager.getArchitectureHandleFromArchitectureName(architectureName);
    }

    // The architectures the biome dominating a column allows
    private ObjectList<String> sampleArchitectureNames(WorldHandle worldHandle, long worldX, long worldZ) {

        SettlementSiteAsyncContainer scratch = siteContainer.getInstance();

        biomeManager.sampleBiomeField(worldHandle, worldX, worldZ, scratch.blend);

        BiomeHandle biome = scratch.blend.getDominantBiome();

        scratch.reset();

        return biome != null ? biome.getArchitectureNames() : ObjectLists.emptyList();
    }

    private long resolveSeed(WorldHandle worldHandle, int cellX, int cellZ) {
        return BiomeFieldUtility.hashCell(worldHandle.getSeed() ^ EngineSetting.SETTLEMENT_PLAN_SALT, cellX, cellZ);
    }

    private float roll(long seed, int cellX, int cellZ, long salt) {
        return BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(seed ^ salt, cellX, cellZ));
    }
}
