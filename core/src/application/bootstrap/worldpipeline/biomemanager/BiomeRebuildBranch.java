package application.bootstrap.worldpipeline.biomemanager;

import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.architecturemanager.ArchitectureManager;
import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeTreeStruct;
import application.bootstrap.worldpipeline.biome.BiomeVeinStruct;
import application.bootstrap.worldpipeline.biome.ProbableBiomeStruct;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BiomeRebuildBranch extends BranchPackage {

    /*
     * Builds the handle for a live biome edit and proves it can go live
     * before anything changes. Once the boot loaders are released nothing can
     * be loaded on demand, so every biome, block, weather, season, tree and
     * architecture it names must already be registered; its variants must not belong to
     * another parent or chain back into it; and the last biome painted on the
     * world map must keep its color. A biome keeps the ID its name was first
     * registered under. Every refusal is a catchable InternalException.
     */

    // Internal
    private BiomeManager biomeManager;
    private BlockManager blockManager;
    private WeatherManager weatherManager;
    private SeasonManager seasonManager;
    private TreeManager treeManager;
    private ArchitectureManager architectureManager;

    // Base \\

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
        this.blockManager = get(BlockManager.class);
        this.weatherManager = get(WeatherManager.class);
        this.seasonManager = get(SeasonManager.class);
        this.treeManager = get(TreeManager.class);
        this.architectureManager = get(ArchitectureManager.class);
    }

    // Build \\

    BiomeHandle build(String biomeName, ArpgObjectStruct biomeArpg) {

        BiomeData biomeData = BiomeArpgUtility.parse(
                biomeName, biomeManager.registerBiomeName(biomeName), biomeArpg);

        validateVariants(biomeData);
        validateBlocks(biomeData);
        validateWeathers(biomeData);
        validateTrees(biomeData);
        validateArchitectures(biomeData);
        validateMapColor(biomeData);

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }

    // Validation \\

    private void validateVariants(BiomeData biomeData) {

        String biomeName = biomeData.getBiomeName();
        ObjectArrayList<ProbableBiomeStruct> probableBiomes = biomeData.getProbableBiomes();

        for (int i = 0; i < probableBiomes.size(); i++) {

            String variantName = probableBiomes.get(i).getBiomeName();
            String parentName = biomeManager.getVariantParentName(variantName);

            requireBiome(biomeName, variantName, "probable_biomes");

            if (parentName != null && !parentName.equals(biomeName))
                throw fail(biomeName, "lists \"" + variantName + "\" in \"probable_biomes\", which already belongs "
                        + "to \"" + parentName + "\".");

            if (biomeManager.isChainedBeneath(biomeName, variantName))
                throw fail(biomeName, "lists \"" + variantName + "\" in \"probable_biomes\", but is already chained "
                        + "beneath it — a probable biome chain cannot loop back on itself.");
        }

        if (biomeData.getBeachBiomeName() != null)
            requireBiome(biomeName, biomeData.getBeachBiomeName(), "beach_biome");
    }

    private void validateBlocks(BiomeData biomeData) {
        requireBlock(biomeData.getBiomeName(), biomeData.getSurfaceBlockName(), "surface_block");
        requireBlock(biomeData.getBiomeName(), biomeData.getSubsurfaceBlockName(), "subsurface_block");
        requireBlock(biomeData.getBiomeName(), biomeData.getUnderwaterBlockName(), "underwater_block");
        requireBlock(biomeData.getBiomeName(), biomeData.getRockBlockName(), "rock_block");

        ObjectArrayList<BiomeVeinStruct> veins = biomeData.getVeins();

        for (int i = 0; i < veins.size(); i++)
            requireBlock(biomeData.getBiomeName(), veins.get(i).getBlockName(), "veins");
    }

    private void validateWeathers(BiomeData biomeData) {

        String biomeName = biomeData.getBiomeName();
        ObjectArrayList<String> seasonNames = biomeData.getSeasonNames();

        for (int i = 0; i < seasonNames.size(); i++) {

            String seasonName = seasonNames.get(i);
            ObjectArrayList<String> weatherNames = biomeData.getWeatherNamesForSeason(seasonName);

            if (!seasonManager.hasSeason(seasonName))
                throw fail(biomeName, "\"weathers\" names unknown season \"" + seasonName + "\".");

            for (int j = 0; j < weatherNames.size(); j++)
                if (!weatherManager.hasWeather(weatherNames.get(j)))
                    throw fail(biomeName, "\"weathers\" names unknown weather \"" + weatherNames.get(j) + "\".");
        }
    }

    private void validateTrees(BiomeData biomeData) {

        ObjectArrayList<BiomeTreeStruct> trees = biomeData.getTrees();

        for (int i = 0; i < trees.size(); i++)
            if (!treeManager.hasTree(trees.get(i).getTreeName()))
                throw fail(biomeData.getBiomeName(), "\"trees\" names unknown tree \""
                        + trees.get(i).getTreeName() + "\".");
    }

    private void validateArchitectures(BiomeData biomeData) {

        ObjectArrayList<String> architectureNames = biomeData.getArchitectureNames();

        for (int i = 0; i < architectureNames.size(); i++)
            if (!architectureManager.hasArchitecture(architectureNames.get(i)))
                throw fail(biomeData.getBiomeName(), "\"architectures\" names unknown architecture \""
                        + architectureNames.get(i) + "\".");
    }

    private void validateMapColor(BiomeData biomeData) {

        if (biomeData.getMapColor() == BiomeData.MAP_COLOR_UNDEFINED
                && biomeManager.isLastPaintedBiome(biomeData.getBiomeName()))
            throw fail(biomeData.getBiomeName(), "is the last biome painted on the world map and must keep its "
                    + "\"map_color\".");
    }

    // Utility \\

    private void requireBiome(String biomeName, String referencedName, String field) {

        if (!biomeManager.hasBiome(referencedName))
            throw fail(biomeName, "\"" + field + "\" names unknown biome \"" + referencedName + "\".");
    }

    private void requireBlock(String biomeName, String blockName, String field) {

        if (!blockManager.hasBlock(blockName))
            throw fail(biomeName, "\"" + field + "\" names unknown block \"" + blockName + "\".");
    }

    private InternalException fail(String biomeName, String message) {
        return new InternalException("Biome \"" + biomeName + "\" " + message);
    }
}
