package application.bootstrap.worldpipeline.biomemanager;

import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.ProbableBiomePlacement;
import application.bootstrap.worldpipeline.biome.ProbableBiomeStruct;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.LinearSpline;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BiomeArpgUtility extends EngineUtility {

    /*
     * The single definition of the biome format: display name, weathers, map
     * color, probable biomes with their placement, patch sizes and shapes,
     * surface blocks, ocean and beach settings, and
     * the optional terrain shape splines and detail controls, each falling
     * back to TerrainShapeUtility's defaults. A malformed field throws a
     * catchable InternalException naming the biome, so BiomeBuilder fails the
     * boot on it while a live rebuild from the editor reports it and keeps the
     * biome it already had.
     */

    // Parse \\

    static BiomeData parse(String biomeName, ArpgObjectStruct biomeArpg) {

        short biomeID = RegistryUtility.toShortID(biomeName);

        ObjectArrayList<String> seasonNames = new ObjectArrayList<>();
        Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances = new Object2ObjectOpenHashMap<>();

        parseWeathers(biomeArpg, biomeName, seasonNames, seasonWeatherNames, seasonWeatherChances);

        int mapColor = parseMapColor(biomeArpg, biomeName);
        String displayName = parseDisplayName(biomeArpg, biomeName, mapColor);

        ObjectArrayList<ProbableBiomeStruct> probableBiomes = parseProbableBiomes(biomeArpg, biomeName);

        String surfaceBlockName = parseBlockName(
                biomeArpg, "surface_block", EngineSetting.DEFAULT_SURFACE_BLOCK_NAME);
        String subsurfaceBlockName = parseBlockName(
                biomeArpg, "subsurface_block", EngineSetting.DEFAULT_SUBSURFACE_BLOCK_NAME);
        String underwaterBlockName = parseBlockName(
                biomeArpg, "underwater_block", EngineSetting.DEFAULT_UNDERWATER_BLOCK_NAME);

        boolean oceanWater = ArpgUtility.getBoolean(biomeArpg, "ocean_water", false);
        String beachBiomeName = parseBeachBiomeName(biomeArpg, biomeName, oceanWater);

        LinearSpline continentalnessSpline = parseSpline(
                biomeArpg, "continentalness_spline", "x", "height_blocks",
                TerrainShapeUtility.DEFAULT_CONTINENTALNESS_SPLINE, biomeName);

        LinearSpline erosionSpline = parseSpline(
                biomeArpg, "erosion_spline", "x", "amplitude_blocks",
                TerrainShapeUtility.DEFAULT_EROSION_SPLINE, biomeName);

        LinearSpline peaksValleysSpline = parseSpline(
                biomeArpg, "peaks_valleys_spline", "x", "contribution",
                TerrainShapeUtility.DEFAULT_PEAKS_VALLEYS_SPLINE, biomeName);

        float detailAmplitudeBlocks = ArpgUtility.getFloat(
                biomeArpg, "detail_amplitude_blocks", EngineSetting.TERRAIN_DETAIL_AMPLITUDE_BLOCKS);

        float detailWavelengthBlocks = ArpgUtility.getFloat(
                biomeArpg, "detail_wavelength_blocks", EngineSetting.TERRAIN_DETAIL_WAVELENGTH_BLOCKS);

        if (detailWavelengthBlocks <= 0f)
            throw fail(biomeName, "\"detail_wavelength_blocks\" must be greater than 0.");

        float terrainHeightScale = ArpgUtility.getFloat(
                biomeArpg, "terrain_height_scale", EngineSetting.DEFAULT_BIOME_TERRAIN_HEIGHT_SCALE);

        return new BiomeData(
                biomeName, displayName, biomeID, Color.WHITE,
                seasonWeatherNames, seasonWeatherChances, seasonNames,
                mapColor, probableBiomes,
                surfaceBlockName, subsurfaceBlockName, underwaterBlockName,
                continentalnessSpline, erosionSpline, peaksValleysSpline,
                detailAmplitudeBlocks, detailWavelengthBlocks, terrainHeightScale,
                oceanWater, beachBiomeName);
    }

    // Display Name \\

    private static String parseDisplayName(ArpgObjectStruct biomeArpg, String biomeName, int mapColor) {

        if (!biomeArpg.has("display_name")) {

            if (mapColor != BiomeData.MAP_COLOR_UNDEFINED)
                throw fail(biomeName, "is painted on the world map but declares no \"display_name\" — only "
                        + "variants linked through \"probable_biomes\" may be unnamed.");

            return null;
        }

        String displayName = biomeArpg.get("display_name").getAsString().trim();

        if (displayName.isEmpty())
            throw fail(biomeName, "declares an empty \"display_name\".");

        return displayName;
    }

    // Weathers \\

    private static void parseWeathers(
            ArpgObjectStruct biomeArpg,
            String biomeName,
            ObjectArrayList<String> outSeasonNames,
            Object2ObjectOpenHashMap<String, ObjectArrayList<String>> outSeasonWeatherNames,
            Object2ObjectOpenHashMap<String, FloatArrayList> outSeasonWeatherChances) {

        if (!biomeArpg.has("weathers"))
            return;

        ArpgObjectStruct weathersArpg = biomeArpg.getAsObject("weathers");

        for (String seasonName : weathersArpg.keySet()) {

            ArpgArrayStruct weatherArray = weathersArpg.getAsArray(seasonName);

            ObjectArrayList<String> names = new ObjectArrayList<>(weatherArray.size());
            FloatArrayList chances = new FloatArrayList(weatherArray.size());

            for (ArpgElementStruct element : weatherArray)
                parseWeatherEntry(element, biomeName, names, chances);

            outSeasonWeatherNames.put(seasonName, names);
            outSeasonWeatherChances.put(seasonName, chances);
            outSeasonNames.add(seasonName);
        }
    }

    private static void parseWeatherEntry(
            ArpgElementStruct element,
            String biomeName,
            ObjectArrayList<String> names,
            FloatArrayList chances) {

        if (element.isValue()) {
            names.add(element.getAsString());
            chances.add(EngineSetting.DEFAULT_BIOME_WEATHER_CHANCE);
            return;
        }

        ArpgObjectStruct entryArpg = element.getAsObject();

        names.add(requireString(entryArpg, "name", biomeName, "weathers"));
        chances.add(ArpgUtility.getFloat(entryArpg, "chance", EngineSetting.DEFAULT_BIOME_WEATHER_CHANCE));
    }

    // Map Color \\

    static int parseMapColor(ArpgObjectStruct biomeArpg, String biomeName) {

        if (!biomeArpg.has("map_color"))
            return BiomeData.MAP_COLOR_UNDEFINED;

        String raw = biomeArpg.get("map_color").getAsString();
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;

        if (hex.length() != 6)
            throw fail(biomeName, "has invalid map_color \"" + raw
                    + "\" — expected a 6-digit hex RGB value, e.g. \"#5B8C3A\".");

        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new InternalException("Biome \"" + biomeName + "\" has invalid map_color \"" + raw
                    + "\" — not valid hex.", e);
        }
    }

    // Probable Biomes \\

    private static ObjectArrayList<ProbableBiomeStruct> parseProbableBiomes(
            ArpgObjectStruct biomeArpg,
            String biomeName) {

        ObjectArrayList<ProbableBiomeStruct> probableBiomes = new ObjectArrayList<>();

        if (!biomeArpg.has("probable_biomes"))
            return probableBiomes;

        ArpgArrayStruct probableArray = biomeArpg.getAsArray("probable_biomes");
        float scatteredTotal = 0f;

        for (ArpgElementStruct element : probableArray) {

            ProbableBiomeStruct probableBiome = parseProbableBiome(element.getAsObject(), biomeName, scatteredTotal);

            if (!probableBiome.isCentered())
                scatteredTotal += probableBiome.getChance();

            if (scatteredTotal > 1f)
                throw fail(biomeName, "probable_biomes scattered chances sum to " + scatteredTotal
                        + ", which exceeds 1.0 — reduce the chances so they share no more than the whole biome.");

            probableBiomes.add(probableBiome);
        }

        return probableBiomes;
    }

    // A scattered entry covers the part of what earlier entries leave that makes its chance a share of the whole
    private static ProbableBiomeStruct parseProbableBiome(
            ArpgObjectStruct entryArpg,
            String biomeName,
            float scatteredTotal) {

        String variantName = requireString(entryArpg, "name", biomeName, "probable_biomes");

        if (variantName.equals(biomeName))
            throw fail(biomeName, "lists itself in \"probable_biomes\".");

        if (!entryArpg.has("chance"))
            throw fail(biomeName, "probable_biomes entry \"" + variantName
                    + "\" is missing required \"chance\" field.");

        float chance = entryArpg.get("chance").getAsFloat();

        if (chance <= 0f || chance > 1f)
            throw fail(biomeName, "probable_biomes entry \"" + variantName + "\" has chance " + chance
                    + " — chance must be greater than 0 and no more than 1.");

        ProbableBiomePlacement placement = parsePlacement(entryArpg, biomeName, variantName);

        float minSizeBlocks = ArpgUtility.getFloat(
                entryArpg, "min_size_blocks", EngineSetting.BIOME_PROBABLE_DEFAULT_MIN_SIZE_BLOCKS);

        float maxSizeBlocks = ArpgUtility.getFloat(
                entryArpg, "max_size_blocks",
                Math.max(minSizeBlocks, EngineSetting.BIOME_PROBABLE_DEFAULT_MAX_SIZE_BLOCKS));

        if (minSizeBlocks <= 0f)
            throw fail(biomeName, "probable_biomes entry \"" + variantName + "\" has min_size_blocks "
                    + minSizeBlocks + " — it must be greater than 0.");

        if (maxSizeBlocks < minSizeBlocks)
            throw fail(biomeName, "probable_biomes entry \"" + variantName + "\" has max_size_blocks "
                    + maxSizeBlocks + ", smaller than its min_size_blocks " + minSizeBlocks + ".");

        int minArms = ArpgUtility.getInt(entryArpg, "min_arms", EngineSetting.BIOME_PROBABLE_DEFAULT_MIN_ARMS);
        int maxArms = ArpgUtility.getInt(
                entryArpg, "max_arms", Math.max(minArms, EngineSetting.BIOME_PROBABLE_DEFAULT_MAX_ARMS));

        if (minArms < 0 || maxArms < minArms || maxArms > EngineSetting.BIOME_PROBABLE_SHAPE_MAX_ARMS)
            throw fail(biomeName, "probable_biomes entry \"" + variantName + "\" has arms " + minArms + " to "
                    + maxArms + " — they must run from 0 up to " + EngineSetting.BIOME_PROBABLE_SHAPE_MAX_ARMS
                    + ", with min_arms no more than max_arms.");

        float minCoreScale = ArpgUtility.getFloat(
                entryArpg, "min_core_scale", EngineSetting.BIOME_PROBABLE_DEFAULT_MIN_CORE_SCALE);

        float maxCoreScale = ArpgUtility.getFloat(
                entryArpg, "max_core_scale",
                Math.max(minCoreScale, EngineSetting.BIOME_PROBABLE_DEFAULT_MAX_CORE_SCALE));

        if (minCoreScale <= 0f || maxCoreScale < minCoreScale || maxCoreScale > 1f)
            throw fail(biomeName, "probable_biomes entry \"" + variantName + "\" has core scales " + minCoreScale
                    + " to " + maxCoreScale + " — they must be greater than 0 and no more than 1, "
                    + "with min_core_scale no more than max_core_scale.");

        float coverage = placement == ProbableBiomePlacement.CENTER
                ? chance
                : computeScatterCoverage(chance, scatteredTotal);

        return new ProbableBiomeStruct(
                variantName, placement, chance, coverage,
                minSizeBlocks, maxSizeBlocks, minArms, maxArms, minCoreScale, maxCoreScale);
    }

    private static float computeScatterCoverage(float chance, float scatteredTotal) {

        float remainingShare = 1f - scatteredTotal;

        return remainingShare > chance ? chance / remainingShare : 1f;
    }

    private static ProbableBiomePlacement parsePlacement(
            ArpgObjectStruct entryArpg,
            String biomeName,
            String variantName) {

        if (!entryArpg.has("placement"))
            return ProbableBiomePlacement.SCATTER;

        String raw = entryArpg.get("placement").getAsString();

        for (ProbableBiomePlacement placement : ProbableBiomePlacement.values())
            if (placement.name().equalsIgnoreCase(raw))
                return placement;

        throw fail(biomeName, "probable_biomes entry \"" + variantName + "\" has invalid placement \"" + raw
                + "\" — expected \"scatter\" or \"center\".");
    }

    // Beach Biome \\

    private static String parseBeachBiomeName(ArpgObjectStruct biomeArpg, String biomeName, boolean oceanWater) {

        if (!biomeArpg.has("beach_biome"))
            return null;

        String beachBiomeName = biomeArpg.get("beach_biome").getAsString();

        if (oceanWater)
            throw fail(biomeName, "is an ocean and declares \"beach_biome\" \"" + beachBiomeName
                    + "\" — beaches are declared by the land biomes an ocean borders.");

        if (beachBiomeName.equals(biomeName))
            throw fail(biomeName, "declares itself as its own \"beach_biome\".");

        return beachBiomeName;
    }

    // Terrain Shape \\

    private static LinearSpline parseSpline(
            ArpgObjectStruct biomeArpg,
            String field,
            String xKey,
            String yKey,
            LinearSpline defaultSpline,
            String biomeName) {

        if (!biomeArpg.has(field) || biomeArpg.get(field).isNull())
            return defaultSpline;

        ArpgObjectStruct splineArpg = biomeArpg.getAsObject(field);

        if (!splineArpg.has(xKey) || !splineArpg.has(yKey))
            throw fail(biomeName, "\"" + field + "\" must declare both \"" + xKey + "\" and \"" + yKey + "\".");

        float[] x = parseFloatArray(splineArpg.getAsArray(xKey));
        float[] y = parseFloatArray(splineArpg.getAsArray(yKey));

        if (x.length != y.length)
            throw fail(biomeName, "\"" + field + "\" has " + x.length + " \"" + xKey + "\" entries but "
                    + y.length + " \"" + yKey + "\" entries — they must match.");

        if (x.length < 2)
            throw fail(biomeName, "\"" + field + "\" needs at least 2 control points.");

        for (int i = 1; i < x.length; i++)
            if (x[i] <= x[i - 1])
                throw fail(biomeName, "\"" + field + "\" \"" + xKey + "\" values must be strictly increasing.");

        return new LinearSpline(x, y);
    }

    private static float[] parseFloatArray(ArpgArrayStruct array) {

        float[] result = new float[array.size()];

        for (int i = 0; i < array.size(); i++)
            result[i] = array.get(i).getAsFloat();

        return result;
    }

    // Terrain Blocks \\

    private static String parseBlockName(ArpgObjectStruct biomeArpg, String field, String fallback) {
        return biomeArpg.has(field) ? biomeArpg.get(field).getAsString() : fallback;
    }

    // Utility \\

    private static String requireString(ArpgObjectStruct entryArpg, String key, String biomeName, String field) {

        if (!entryArpg.has(key))
            throw fail(biomeName, "\"" + field + "\" entry is missing required \"" + key + "\" field.");

        return entryArpg.get(key).getAsString();
    }

    private static InternalException fail(String biomeName, String message) {
        return new InternalException("Biome \"" + biomeName + "\" " + message);
    }
}
