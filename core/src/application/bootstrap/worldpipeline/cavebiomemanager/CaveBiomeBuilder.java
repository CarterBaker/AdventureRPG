package application.bootstrap.worldpipeline.cavebiomemanager;

import java.io.File;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.biome.BiomeCoveringStruct;
import application.bootstrap.worldpipeline.biome.BiomeVeinStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeArpgUtility;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.cavebiome.CaveBiomeData;
import application.bootstrap.worldpipeline.cavebiome.CaveBiomeHandle;
import application.bootstrap.worldpipeline.cavebiome.CaveSpeleothemStruct;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CaveBiomeBuilder extends BuilderPackage {

    /*
     * Parses cave biome ARPG into CaveBiomeData wrapped in a CaveBiomeHandle,
     * under the ID CaveBiomeManager assigns its name. Floors, ceilings and
     * still water beds fall back to the rock, every block it lays must be a
     * whole FULL-geometry block, every covering must host the block it is
     * laid over, and coverings and veins are read exactly as a biome reads
     * them. Everything is validated at load, so a malformed cave biome fails
     * the boot.
     */

    // Internal
    private CaveBiomeManager caveBiomeManager;
    private BlockManager blockManager;
    private CoveringManager coveringManager;

    // Base \\

    @Override
    protected void get() {
        this.caveBiomeManager = get(CaveBiomeManager.class);
        this.blockManager = get(BlockManager.class);
        this.coveringManager = get(CoveringManager.class);
    }

    // Build \\

    CaveBiomeHandle build(File file, String caveBiomeName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short caveBiomeID = caveBiomeManager.registerCaveBiomeName(caveBiomeName);

        String displayName = ArpgUtility.getString(arpg, "display_name", caveBiomeName);

        // Blocks
        String rockBlockName = ArpgUtility.getString(arpg, "rock_block", EngineSetting.DEFAULT_STONE_BLOCK_NAME);
        String floorBlockName = ArpgUtility.getString(arpg, "floor_block", rockBlockName);
        String ceilingBlockName = ArpgUtility.getString(arpg, "ceiling_block", rockBlockName);
        String bedBlockName = ArpgUtility.getString(arpg, "bed_block", floorBlockName);

        requireWholeBlock(rockBlockName, "rock_block", caveBiomeName);
        requireWholeBlock(floorBlockName, "floor_block", caveBiomeName);
        requireWholeBlock(ceilingBlockName, "ceiling_block", caveBiomeName);
        requireWholeBlock(bedBlockName, "bed_block", caveBiomeName);

        int shellBlocks = parseShellBlocks(arpg, caveBiomeName);

        // Coverings
        BiomeCoveringStruct floorCovering;
        BiomeCoveringStruct wallCovering;
        BiomeCoveringStruct ceilingCovering;
        ObjectArrayList<BiomeVeinStruct> veins;

        try {
            floorCovering = BiomeArpgUtility.parseCovering(arpg, "floor_covering", caveBiomeName);
            wallCovering = BiomeArpgUtility.parseCovering(arpg, "wall_covering", caveBiomeName);
            ceilingCovering = BiomeArpgUtility.parseCovering(arpg, "ceiling_covering", caveBiomeName);
            veins = BiomeArpgUtility.parseVeins(arpg, caveBiomeName, EngineSetting.CAVE_BIOME_MAX_VEINS);
        } catch (InternalException e) {
            return throwException(e.getMessage(), e.getCause());
        }

        requireHost(floorCovering, floorBlockName, "floor_covering", caveBiomeName);
        requireHost(wallCovering, rockBlockName, "wall_covering", caveBiomeName);
        requireHost(ceilingCovering, ceilingBlockName, "ceiling_covering", caveBiomeName);

        for (int i = 0; i < veins.size(); i++)
            requireWholeBlock(veins.get(i).getBlockName(), "veins", caveBiomeName);

        // Speleothems
        CaveSpeleothemStruct speleothems = parseSpeleothems(arpg, caveBiomeName);

        CaveBiomeData caveBiomeData = new CaveBiomeData(
                caveBiomeName,
                displayName,
                caveBiomeID,
                rockBlockName,
                floorBlockName,
                ceilingBlockName,
                bedBlockName,
                shellBlocks,
                floorCovering,
                wallCovering,
                ceilingCovering,
                speleothems,
                veins);

        CaveBiomeHandle caveBiomeHandle = create(CaveBiomeHandle.class);
        caveBiomeHandle.constructor(caveBiomeData);

        return caveBiomeHandle;
    }

    // Blocks \\

    private void requireWholeBlock(String blockName, String field, String caveBiomeName) {

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockName(blockName);

        if (blockHandle.getGeometry() != DynamicGeometryType.FULL)
            throwException("Cave biome \"" + caveBiomeName + "\" lays \"" + blockName + "\" as its \"" + field
                    + "\", which is not a FULL-geometry block — caves are lined only with whole blocks.");
    }

    private int parseShellBlocks(ArpgObjectStruct arpg, String caveBiomeName) {

        int shellBlocks = ArpgUtility.getInt(arpg, "shell_blocks", EngineSetting.DEFAULT_CAVE_BIOME_SHELL_BLOCKS);

        if (shellBlocks < 1 || shellBlocks > EngineSetting.CAVE_BIOME_MAX_SHELL_BLOCKS)
            throwException("Cave biome \"" + caveBiomeName + "\" has \"shell_blocks\" " + shellBlocks
                    + " — it must run from 1 to " + EngineSetting.CAVE_BIOME_MAX_SHELL_BLOCKS + ".");

        return shellBlocks;
    }

    // Coverings \\

    private void requireHost(BiomeCoveringStruct covering, String hostBlockName, String field, String caveBiomeName) {

        if (covering == null)
            return;

        short hostBlockID = (short) blockManager.getBlockIDFromBlockName(hostBlockName);

        if (!coveringManager.getCoveringHandleFromCoveringName(covering.getCoveringName()).canHost(hostBlockID))
            throwException("Cave biome \"" + caveBiomeName + "\" lays \"" + field + "\" \""
                    + covering.getCoveringName() + "\" over \"" + hostBlockName
                    + "\", which the covering does not name among its \"hosts\".");
    }

    // Speleothems \\

    private CaveSpeleothemStruct parseSpeleothems(ArpgObjectStruct arpg, String caveBiomeName) {

        if (!ArpgUtility.hasObject(arpg, "speleothems"))
            return CaveSpeleothemStruct.NONE;

        ArpgObjectStruct speleothemsArpg = arpg.getAsObject("speleothems");
        String blockName = ArpgUtility.validateString(speleothemsArpg, "block");

        requireWholeBlock(blockName, "speleothems", caveBiomeName);

        float stalactites = parseShare(speleothemsArpg, "stalactites",
                EngineSetting.DEFAULT_SPELEOTHEM_STALACTITES, caveBiomeName);
        float stalagmites = parseShare(speleothemsArpg, "stalagmites",
                EngineSetting.DEFAULT_SPELEOTHEM_STALAGMITES, caveBiomeName);
        float giants = parseShare(speleothemsArpg, "giants", EngineSetting.DEFAULT_SPELEOTHEM_GIANTS, caveBiomeName);

        int maxLengthBlocks = ArpgUtility.getInt(
                speleothemsArpg, "max_length_blocks", EngineSetting.DEFAULT_SPELEOTHEM_MAX_LENGTH_BLOCKS);
        int maxGiantRadiusBlocks = ArpgUtility.getInt(
                speleothemsArpg, "max_giant_radius_blocks", EngineSetting.DEFAULT_SPELEOTHEM_MAX_GIANT_RADIUS_BLOCKS);

        if (maxLengthBlocks < 1 || maxLengthBlocks > EngineSetting.SPELEOTHEM_MAX_LENGTH_BLOCKS)
            throwException("Cave biome \"" + caveBiomeName + "\" has speleothem \"max_length_blocks\" "
                    + maxLengthBlocks + " — it must run from 1 to "
                    + EngineSetting.SPELEOTHEM_MAX_LENGTH_BLOCKS + ".");

        if (maxGiantRadiusBlocks < 1 || maxGiantRadiusBlocks > EngineSetting.SPELEOTHEM_GIANT_MAX_RADIUS_BLOCKS)
            throwException("Cave biome \"" + caveBiomeName + "\" has speleothem \"max_giant_radius_blocks\" "
                    + maxGiantRadiusBlocks + " — it must run from 1 to "
                    + EngineSetting.SPELEOTHEM_GIANT_MAX_RADIUS_BLOCKS + ".");

        return new CaveSpeleothemStruct(
                blockName, stalactites, stalagmites, maxLengthBlocks, giants, maxGiantRadiusBlocks);
    }

    private float parseShare(ArpgObjectStruct arpg, String key, float fallback, String caveBiomeName) {

        float share = ArpgUtility.getFloat(arpg, key, fallback);

        if (share < 0f || share > 1f)
            throwException("Cave biome \"" + caveBiomeName + "\" has speleothem \"" + key + "\" " + share
                    + " — it must lie between 0 and 1.");

        return share;
    }
}
