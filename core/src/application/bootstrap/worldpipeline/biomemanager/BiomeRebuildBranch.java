package application.bootstrap.worldpipeline.biomemanager;

import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BiomeRebuildBranch extends BranchPackage {

    /*
     * Builds the handle for a live biome edit and proves it can go live
     * before anything changes. Once the boot loaders are released nothing can
     * be loaded on demand, so every biome, block, weather and season the tree
     * names must already be registered; its registry ID must neither be the
     * reserved sentinel nor collide with another biome; its variants must not
     * belong to another parent; and the last biome painted on the world map
     * must keep its color. Every refusal is a catchable InternalException.
     */

    // Internal
    private BiomeManager biomeManager;
    private BlockManager blockManager;
    private WeatherManager weatherManager;
    private SeasonManager seasonManager;

    // Base \\

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
        this.blockManager = get(BlockManager.class);
        this.weatherManager = get(WeatherManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    // Build \\

    BiomeHandle build(String biomeName, ArpgObjectStruct biomeArpg) {

        BiomeData biomeData = BiomeArpgUtility.parse(biomeName, biomeArpg);

        validateIdentity(biomeData);
        validateVariants(biomeData);
        validateBlocks(biomeData);
        validateWeathers(biomeData);
        validateMapColor(biomeData);

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }

    // Validation \\

    private void validateIdentity(BiomeData biomeData) {

        String biomeName = biomeData.getBiomeName();
        short biomeID = biomeData.getBiomeID();

        if (biomeID == EngineSetting.REGISTRY_RESERVED_ID)
            throw fail(biomeName, "hashes to the reserved registry ID " + EngineSetting.REGISTRY_RESERVED_ID
                    + " — rename it.");

        BiomeHandle existing = biomeManager.getRegisteredBiome(biomeID);

        if (existing != null && RegistryUtility.isCollision(biomeName, existing.getBiomeName(), biomeID))
            throw fail(biomeName, "collides with \"" + existing.getBiomeName() + "\" (ID " + biomeID
                    + ") — rename one of them.");
    }

    private void validateVariants(BiomeData biomeData) {

        String biomeName = biomeData.getBiomeName();
        ObjectArrayList<String> variantNames = biomeData.getProbableBiomeNames();

        for (int i = 0; i < variantNames.size(); i++) {

            String variantName = variantNames.get(i);
            String parentName = biomeManager.getVariantParentName(variantName);

            requireBiome(biomeName, variantName, "probable_biomes");

            if (parentName != null && !parentName.equals(biomeName))
                throw fail(biomeName, "lists \"" + variantName + "\" in \"probable_biomes\", which already belongs "
                        + "to \"" + parentName + "\".");
        }

        if (biomeData.getBeachBiomeName() != null)
            requireBiome(biomeName, biomeData.getBeachBiomeName(), "beach_biome");
    }

    private void validateBlocks(BiomeData biomeData) {
        requireBlock(biomeData.getBiomeName(), biomeData.getSurfaceBlockName(), "surface_block");
        requireBlock(biomeData.getBiomeName(), biomeData.getSubsurfaceBlockName(), "subsurface_block");
        requireBlock(biomeData.getBiomeName(), biomeData.getUnderwaterBlockName(), "underwater_block");
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
