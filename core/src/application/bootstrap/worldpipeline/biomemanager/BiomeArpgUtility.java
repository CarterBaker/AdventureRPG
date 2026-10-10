package application.bootstrap.worldpipeline.biomemanager;

import application.bootstrap.worldpipeline.biome.BiomeCaveBiomeStruct;
import application.bootstrap.worldpipeline.biome.BiomeCaveStruct;
import application.bootstrap.worldpipeline.biome.BiomeCliffStruct;
import application.bootstrap.worldpipeline.biome.BiomeCoastStruct;
import application.bootstrap.worldpipeline.biome.BiomeCoveringStruct;
import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeRidgeStruct;
import application.bootstrap.worldpipeline.biome.BiomeTreeStruct;
import application.bootstrap.worldpipeline.biome.BiomeVeinStruct;
import application.bootstrap.worldpipeline.biome.ProbableBiomePlacement;
import application.bootstrap.worldpipeline.biome.ProbableBiomeStruct;
import application.bootstrap.worldpipeline.tree.TreeDistribution;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import engine.graphics.color.Color;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.LinearSpline;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeArpgUtility extends EngineUtility {

    /*
     * The single definition of the biome format: display name, weathers, map
     * color, probable biomes with their placement, patch sizes and shapes,
     * surface and rock blocks, the coverings laid over them and the tint
     * every covering takes from the biome, ocean, still water and beach
     * settings, the
     * optional terrain shape splines and detail controls, each falling back
     * to TerrainShapeUtility's defaults, and the optional cliffs, ridges,
     * coast, caves, the cave biomes it holds beneath it, veins, trees and the
     * architectures settlements on it are built in. Coverings and veins are
     * read the same way for cave biomes. A malformed field throws a
     * catchable InternalException naming the biome, so BiomeBuilder fails the
     * boot on it while a live rebuild from the editor reports it and keeps the
     * biome it already had.
     */

    // Parse \\

    static BiomeData parse(String biomeName, short biomeID, ArpgObjectStruct biomeArpg) {

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
        String rockBlockName = parseBlockName(
                biomeArpg, "rock_block", EngineSetting.DEFAULT_STONE_BLOCK_NAME);

        float rockSlope = ArpgUtility.getFloat(biomeArpg, "rock_slope", EngineSetting.DEFAULT_BIOME_ROCK_SLOPE);

        if (rockSlope <= 0f)
            throw fail(biomeName, "\"rock_slope\" must be greater than 0.");

        Color coveringTint = parseCoveringTint(biomeArpg, biomeName);
        BiomeCoveringStruct surfaceCovering = parseCovering(biomeArpg, "surface_covering", biomeName);
        BiomeCoveringStruct rockCovering = parseCovering(biomeArpg, "rock_covering", biomeName);
        BiomeCoveringStruct underwaterCovering = parseCovering(biomeArpg, "underwater_covering", biomeName);

        boolean oceanWater = ArpgUtility.getBoolean(biomeArpg, "ocean_water", false);
        int waterLevelBlocks = parseWaterLevel(biomeArpg, biomeName, oceanWater);
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

        BiomeCliffStruct cliffs = parseCliffs(biomeArpg, biomeName);
        BiomeRidgeStruct ridges = parseRidges(biomeArpg, biomeName);
        BiomeCoastStruct coast = parseCoast(biomeArpg, biomeName, oceanWater);
        BiomeCaveStruct caves = parseCaves(biomeArpg, biomeName);
        ObjectArrayList<BiomeCaveBiomeStruct> caveBiomes = parseCaveBiomes(biomeArpg, biomeName);
        ObjectArrayList<BiomeVeinStruct> veins = parseVeins(biomeArpg, biomeName, EngineSetting.BIOME_MAX_VEINS);
        ObjectArrayList<BiomeTreeStruct> trees = parseTrees(biomeArpg, biomeName);
        ObjectArrayList<String> architectureNames = parseArchitectures(biomeArpg);

        return new BiomeData(
                biomeName, displayName, biomeID, coveringTint,
                seasonWeatherNames, seasonWeatherChances, seasonNames,
                mapColor, probableBiomes,
                surfaceBlockName, subsurfaceBlockName, underwaterBlockName, rockBlockName, rockSlope,
                surfaceCovering, rockCovering, underwaterCovering,
                continentalnessSpline, erosionSpline, peaksValleysSpline,
                detailAmplitudeBlocks, detailWavelengthBlocks, terrainHeightScale,
                cliffs, ridges, coast, caves, caveBiomes, veins, trees, architectureNames,
                oceanWater, waterLevelBlocks, beachBiomeName);
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

        return parseHexColor(biomeArpg.get("map_color").getAsString(), "map_color", biomeName);
    }

    private static int parseHexColor(String raw, String field, String biomeName) {

        String hex = raw.startsWith("#") ? raw.substring(1) : raw;

        if (hex.length() != 6)
            throw fail(biomeName, "has invalid " + field + " \"" + raw
                    + "\" — expected a 6-digit hex RGB value, e.g. \"#5B8C3A\".");

        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new InternalException("Biome \"" + biomeName + "\" has invalid " + field + " \"" + raw
                    + "\" — not valid hex.", e);
        }
    }

    // Coverings \\

    // The tint every covering growing in the biome takes, carried to the surface shader in each terrain vertex's color
    private static Color parseCoveringTint(ArpgObjectStruct biomeArpg, String biomeName) {

        int tint = parseHexColor(
                ArpgUtility.getString(biomeArpg, "covering_tint", EngineSetting.DEFAULT_BIOME_COVERING_TINT),
                "covering_tint", biomeName);

        return new Color(
                PackedColorUtility.red(tint) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                PackedColorUtility.green(tint) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                PackedColorUtility.blue(tint) / EngineSetting.COLOR_CHANNEL_BYTE_MAX);
    }

    // A covering the biome lays over one kind of ground, null when it lays none there
    public static BiomeCoveringStruct parseCovering(ArpgObjectStruct biomeArpg, String field, String biomeName) {

        if (!ArpgUtility.hasObject(biomeArpg, field))
            return null;

        ArpgObjectStruct coveringArpg = biomeArpg.getAsObject(field);
        String coveringName = requireString(coveringArpg, "covering", biomeName, field);
        int level = ArpgUtility.getInt(coveringArpg, "level", CoverageUtility.LEVEL_MAX);
        int variance = ArpgUtility.getInt(coveringArpg, "variance", EngineSetting.DEFAULT_BIOME_COVERING_VARIANCE);

        if (level < 1 || level > CoverageUtility.LEVEL_MAX)
            throw fail(biomeName, "\"" + field + "\" has level " + level + " — it must run from 1 to "
                    + CoverageUtility.LEVEL_MAX + ".");

        if (variance < 0 || variance >= level)
            throw fail(biomeName, "\"" + field + "\" has variance " + variance
                    + " — it must run from 0 up to one below its level, so every column keeps some covering.");

        return new BiomeCoveringStruct(coveringName, level, variance);
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

    // Still Water \\

    private static int parseWaterLevel(ArpgObjectStruct biomeArpg, String biomeName, boolean oceanWater) {

        if (!biomeArpg.has("water_level_blocks"))
            return BiomeData.WATER_LEVEL_UNDEFINED;

        int waterLevelBlocks = biomeArpg.get("water_level_blocks").getAsInt();

        if (oceanWater)
            throw fail(biomeName, "is an ocean and declares \"water_level_blocks\" " + waterLevelBlocks
                    + " — the sea always stands at sea level and rides the tide; only still water declares its "
                    + "own level.");

        if (waterLevelBlocks <= EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS
                || waterLevelBlocks >= EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS)
            throw fail(biomeName, "has \"water_level_blocks\" " + waterLevelBlocks + " — it must lie between "
                    + EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS + " and " + EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS
                    + ", exclusive.");

        return waterLevelBlocks;
    }

    // Terrain Features \\

    private static BiomeCliffStruct parseCliffs(ArpgObjectStruct biomeArpg, String biomeName) {

        if (!biomeArpg.has("cliffs") || biomeArpg.get("cliffs").isNull())
            return BiomeCliffStruct.NONE;

        ArpgObjectStruct cliffsArpg = biomeArpg.getAsObject("cliffs");

        float stepBlocks = ArpgUtility.getFloat(
                cliffsArpg, "step_blocks", EngineSetting.DEFAULT_BIOME_CLIFF_STEP_BLOCKS);
        float strength = ArpgUtility.getFloat(cliffsArpg, "strength", EngineSetting.DEFAULT_BIOME_CLIFF_STRENGTH);
        float coverage = ArpgUtility.getFloat(cliffsArpg, "coverage", EngineSetting.DEFAULT_BIOME_CLIFF_COVERAGE);

        if (stepBlocks <= 0f)
            throw fail(biomeName, "\"cliffs\" \"step_blocks\" must be greater than 0.");

        requireUnit(strength, biomeName, "cliffs", "strength");
        requireUnit(coverage, biomeName, "cliffs", "coverage");

        return new BiomeCliffStruct(stepBlocks, strength, coverage);
    }

    private static BiomeRidgeStruct parseRidges(ArpgObjectStruct biomeArpg, String biomeName) {

        if (!biomeArpg.has("ridges") || biomeArpg.get("ridges").isNull())
            return BiomeRidgeStruct.NONE;

        ArpgObjectStruct ridgesArpg = biomeArpg.getAsObject("ridges");

        float amplitudeBlocks = ArpgUtility.getFloat(ridgesArpg, "amplitude_blocks", 0f);
        float wavelengthBlocks = ArpgUtility.getFloat(
                ridgesArpg, "wavelength_blocks", EngineSetting.DEFAULT_BIOME_RIDGE_WAVELENGTH_BLOCKS);

        if (amplitudeBlocks < 0f)
            throw fail(biomeName, "\"ridges\" \"amplitude_blocks\" must not be negative.");

        if (wavelengthBlocks <= 0f)
            throw fail(biomeName, "\"ridges\" \"wavelength_blocks\" must be greater than 0.");

        return new BiomeRidgeStruct(amplitudeBlocks, wavelengthBlocks);
    }

    private static BiomeCoastStruct parseCoast(ArpgObjectStruct biomeArpg, String biomeName, boolean oceanWater) {

        if (!biomeArpg.has("coast") || biomeArpg.get("coast").isNull())
            return BiomeCoastStruct.NONE;

        if (oceanWater)
            throw fail(biomeName, "is an ocean and declares \"coast\" — coasts are declared by the land biomes "
                    + "an ocean borders.");

        ArpgObjectStruct coastArpg = biomeArpg.getAsObject("coast");

        float cliffHeightBlocks = ArpgUtility.getFloat(coastArpg, "cliff_height_blocks", 0f);
        float coverage = ArpgUtility.getFloat(coastArpg, "coverage", EngineSetting.DEFAULT_BIOME_COAST_COVERAGE);
        float overhangBlocks = ArpgUtility.getFloat(
                coastArpg, "overhang_blocks", EngineSetting.DEFAULT_BIOME_COAST_OVERHANG_BLOCKS);
        float seaCaves = ArpgUtility.getFloat(coastArpg, "sea_caves", EngineSetting.DEFAULT_BIOME_COAST_SEA_CAVES);

        if (cliffHeightBlocks < 0f)
            throw fail(biomeName, "\"coast\" \"cliff_height_blocks\" must not be negative.");

        if (overhangBlocks < 0f)
            throw fail(biomeName, "\"coast\" \"overhang_blocks\" must not be negative.");

        requireUnit(coverage, biomeName, "coast", "coverage");
        requireUnit(seaCaves, biomeName, "coast", "sea_caves");

        return new BiomeCoastStruct(cliffHeightBlocks, coverage, overhangBlocks, seaCaves);
    }

    private static BiomeCaveStruct parseCaves(ArpgObjectStruct biomeArpg, String biomeName) {

        if (!biomeArpg.has("caves") || biomeArpg.get("caves").isNull())
            return BiomeCaveStruct.DEFAULT;

        ArpgObjectStruct cavesArpg = biomeArpg.getAsObject("caves");

        float tunnels = ArpgUtility.getFloat(cavesArpg, "tunnels", EngineSetting.DEFAULT_BIOME_CAVE_TUNNELS);
        float caverns = ArpgUtility.getFloat(cavesArpg, "caverns", EngineSetting.DEFAULT_BIOME_CAVE_CAVERNS);
        float noodles = ArpgUtility.getFloat(cavesArpg, "noodles", EngineSetting.DEFAULT_BIOME_CAVE_NOODLES);
        float lakes = ArpgUtility.getFloat(cavesArpg, "lakes", EngineSetting.DEFAULT_BIOME_CAVE_LAKES);
        int minHeightBlocks = ArpgUtility.getInt(
                cavesArpg, "min_height_blocks", EngineSetting.DEFAULT_BIOME_CAVE_MIN_HEIGHT_BLOCKS);
        int maxDepthBlocks = ArpgUtility.getInt(
                cavesArpg, "max_depth_blocks", EngineSetting.DEFAULT_BIOME_CAVE_MAX_DEPTH_BLOCKS);
        boolean entrances = ArpgUtility.getBoolean(
                cavesArpg, "entrances", EngineSetting.DEFAULT_BIOME_CAVE_ENTRANCES);

        requireUnit(tunnels, biomeName, "caves", "tunnels");
        requireUnit(caverns, biomeName, "caves", "caverns");
        requireUnit(noodles, biomeName, "caves", "noodles");
        requireUnit(lakes, biomeName, "caves", "lakes");

        if (minHeightBlocks < 0)
            throw fail(biomeName, "\"caves\" \"min_height_blocks\" must not be negative.");

        if (maxDepthBlocks <= 0)
            throw fail(biomeName, "\"caves\" \"max_depth_blocks\" must be greater than 0.");

        return new BiomeCaveStruct(tunnels, caverns, noodles, lakes, minHeightBlocks, maxDepthBlocks, entrances);
    }

    // Each cave biome claims its chance of the regions beneath the biome, together claiming no more than all of them
    private static ObjectArrayList<BiomeCaveBiomeStruct> parseCaveBiomes(ArpgObjectStruct biomeArpg, String biomeName) {

        ObjectArrayList<BiomeCaveBiomeStruct> caveBiomes = new ObjectArrayList<>();

        if (!ArpgUtility.hasArray(biomeArpg, "cave_biomes"))
            return caveBiomes;

        ArpgArrayStruct caveBiomeArray = biomeArpg.getAsArray("cave_biomes");
        float chanceTotal = 0f;

        if (caveBiomeArray.size() > EngineSetting.BIOME_MAX_CAVE_BIOMES)
            throw fail(biomeName, "declares " + caveBiomeArray.size() + " \"cave_biomes\" — no more than "
                    + EngineSetting.BIOME_MAX_CAVE_BIOMES + " are allowed.");

        for (ArpgElementStruct element : caveBiomeArray) {

            BiomeCaveBiomeStruct caveBiome = parseCaveBiome(element.getAsObject(), biomeName);

            chanceTotal += caveBiome.getChance();

            if (chanceTotal > 1f)
                throw fail(biomeName, "\"cave_biomes\" chances sum to " + chanceTotal
                        + ", which exceeds 1.0 — they share the regions beneath the biome.");

            caveBiomes.add(caveBiome);
        }

        return caveBiomes;
    }

    private static BiomeCaveBiomeStruct parseCaveBiome(ArpgObjectStruct entryArpg, String biomeName) {

        String caveBiomeName = requireString(entryArpg, "cave_biome", biomeName, "cave_biomes");

        float chance = ArpgUtility.getFloat(entryArpg, "chance", EngineSetting.DEFAULT_BIOME_CAVE_BIOME_CHANCE);
        int minHeightBlocks = ArpgUtility.getInt(
                entryArpg, "min_height_blocks", EngineSetting.DEFAULT_BIOME_CAVE_BIOME_MIN_HEIGHT_BLOCKS);
        int maxHeightBlocks = ArpgUtility.getInt(
                entryArpg, "max_height_blocks", EngineSetting.DEFAULT_BIOME_CAVE_BIOME_MAX_HEIGHT_BLOCKS);
        int maxDepthBlocks = ArpgUtility.getInt(
                entryArpg, "max_depth_blocks", EngineSetting.DEFAULT_BIOME_CAVE_BIOME_MAX_DEPTH_BLOCKS);

        if (chance <= 0f || chance > 1f)
            throw fail(biomeName, "\"cave_biomes\" entry \"" + caveBiomeName + "\" has chance " + chance
                    + " — chance must be greater than 0 and no more than 1.");

        if (maxHeightBlocks < minHeightBlocks)
            throw fail(biomeName, "\"cave_biomes\" entry \"" + caveBiomeName + "\" has \"max_height_blocks\" "
                    + maxHeightBlocks + ", below its \"min_height_blocks\" " + minHeightBlocks + ".");

        if (maxDepthBlocks <= 0)
            throw fail(biomeName, "\"cave_biomes\" entry \"" + caveBiomeName + "\" must have \"max_depth_blocks\" "
                    + "greater than 0.");

        return new BiomeCaveBiomeStruct(caveBiomeName, chance, minHeightBlocks, maxHeightBlocks, maxDepthBlocks);
    }

    public static ObjectArrayList<BiomeVeinStruct> parseVeins(
            ArpgObjectStruct biomeArpg,
            String biomeName,
            int maxVeins) {

        ObjectArrayList<BiomeVeinStruct> veins = new ObjectArrayList<>();

        if (!biomeArpg.has("veins"))
            return veins;

        ArpgArrayStruct veinArray = biomeArpg.getAsArray("veins");

        if (veinArray.size() > maxVeins)
            throw fail(biomeName, "declares " + veinArray.size() + " \"veins\" — no more than "
                    + maxVeins + " are allowed.");

        for (ArpgElementStruct element : veinArray)
            veins.add(parseVein(element.getAsObject(), biomeName));

        return veins;
    }

    // Architectures \\

    private static ObjectArrayList<String> parseArchitectures(ArpgObjectStruct biomeArpg) {

        ObjectArrayList<String> architectureNames = new ObjectArrayList<>();

        if (!ArpgUtility.hasArray(biomeArpg, "architectures"))
            return architectureNames;

        for (ArpgElementStruct element : biomeArpg.getAsArray("architectures"))
            architectureNames.add(element.getAsString());

        return architectureNames;
    }

    // Trees \\

    private static ObjectArrayList<BiomeTreeStruct> parseTrees(ArpgObjectStruct biomeArpg, String biomeName) {

        ObjectArrayList<BiomeTreeStruct> trees = new ObjectArrayList<>();

        if (!biomeArpg.has("trees"))
            return trees;

        ArpgArrayStruct treeArray = biomeArpg.getAsArray("trees");

        if (treeArray.size() > EngineSetting.BIOME_MAX_TREE_KINDS)
            throw fail(biomeName, "declares " + treeArray.size() + " \"trees\" — no more than "
                    + EngineSetting.BIOME_MAX_TREE_KINDS + " are allowed.");

        for (ArpgElementStruct element : treeArray)
            trees.add(parseTree(element.getAsObject(), biomeName));

        return trees;
    }

    private static BiomeTreeStruct parseTree(ArpgObjectStruct treeArpg, String biomeName) {

        String treeName = requireString(treeArpg, "tree", biomeName, "trees");
        String distributionName = ArpgUtility.getString(
                treeArpg, "distribution", EngineSetting.DEFAULT_BIOME_TREE_DISTRIBUTION);
        TreeDistribution distribution = null;

        for (TreeDistribution candidate : TreeDistribution.values())
            if (candidate.name().equalsIgnoreCase(distributionName))
                distribution = candidate;

        if (distribution == null)
            throw fail(biomeName, "\"trees\" entry \"" + treeName + "\" has unknown \"distribution\" \""
                    + distributionName + "\".");

        int spacingBlocks = ArpgUtility.getInt(
                treeArpg, "spacing_blocks", EngineSetting.DEFAULT_BIOME_TREE_SPACING_BLOCKS);
        float chance = ArpgUtility.getFloat(treeArpg, "chance", EngineSetting.DEFAULT_BIOME_TREE_CHANCE);
        float clusterRadiusBlocks = ArpgUtility.getFloat(
                treeArpg, "cluster_radius_blocks", EngineSetting.DEFAULT_BIOME_TREE_CLUSTER_RADIUS_BLOCKS);
        int minClusterTrees = EngineSetting.DEFAULT_BIOME_TREE_MIN_CLUSTER_TREES;
        int maxClusterTrees = EngineSetting.DEFAULT_BIOME_TREE_MAX_CLUSTER_TREES;
        float patchWavelengthBlocks = ArpgUtility.getFloat(
                treeArpg, "patch_wavelength_blocks", EngineSetting.DEFAULT_BIOME_TREE_PATCH_WAVELENGTH_BLOCKS);
        float patchCoverage = ArpgUtility.getFloat(
                treeArpg, "patch_coverage", EngineSetting.DEFAULT_BIOME_TREE_PATCH_COVERAGE);

        if (ArpgUtility.hasArray(treeArpg, "cluster_trees")) {

            ArpgArrayStruct clusterTrees = treeArpg.getAsArray("cluster_trees");

            if (clusterTrees.size() != 2)
                throw fail(biomeName, "\"trees\" entry \"" + treeName + "\" \"cluster_trees\" must be a "
                        + "[min, max] pair.");

            minClusterTrees = clusterTrees.get(0).getAsInt();
            maxClusterTrees = clusterTrees.get(1).getAsInt();
        }

        if (spacingBlocks < EngineSetting.BIOME_MIN_TREE_SPACING_BLOCKS)
            throw fail(biomeName, "\"trees\" entry \"" + treeName + "\" must have \"spacing_blocks\" of at least "
                    + EngineSetting.BIOME_MIN_TREE_SPACING_BLOCKS + ".");

        requireUnit(chance, biomeName, "trees", "chance");
        requireUnit(patchCoverage, biomeName, "trees", "patch_coverage");

        if (clusterRadiusBlocks < 0f || clusterRadiusBlocks > EngineSetting.BIOME_MAX_TREE_CLUSTER_RADIUS_BLOCKS)
            throw fail(biomeName, "\"trees\" entry \"" + treeName + "\" must have \"cluster_radius_blocks\" from 0 to "
                    + EngineSetting.BIOME_MAX_TREE_CLUSTER_RADIUS_BLOCKS + ".");

        if (minClusterTrees < 1 || maxClusterTrees < minClusterTrees
                || maxClusterTrees > EngineSetting.BIOME_MAX_CLUSTER_TREES)
            throw fail(biomeName, "\"trees\" entry \"" + treeName + "\" \"cluster_trees\" must run from 1 up to "
                    + EngineSetting.BIOME_MAX_CLUSTER_TREES + ", its maximum no lower than its minimum.");

        if (patchWavelengthBlocks <= 0f)
            throw fail(biomeName, "\"trees\" entry \"" + treeName + "\" must have \"patch_wavelength_blocks\" "
                    + "greater than 0.");

        return new BiomeTreeStruct(
                treeName, distribution, spacingBlocks, chance, clusterRadiusBlocks,
                minClusterTrees, maxClusterTrees, patchWavelengthBlocks, patchCoverage);
    }

    private static BiomeVeinStruct parseVein(ArpgObjectStruct veinArpg, String biomeName) {

        String blockName = requireString(veinArpg, "block", biomeName, "veins");

        float abundance = ArpgUtility.getFloat(veinArpg, "abundance", EngineSetting.DEFAULT_BIOME_VEIN_ABUNDANCE);
        float thicknessBlocks = ArpgUtility.getFloat(
                veinArpg, "thickness_blocks", EngineSetting.DEFAULT_BIOME_VEIN_THICKNESS_BLOCKS);
        int minHeightBlocks = ArpgUtility.getInt(
                veinArpg, "min_height_blocks", EngineSetting.DEFAULT_BIOME_VEIN_MIN_HEIGHT_BLOCKS);
        int maxHeightBlocks = ArpgUtility.getInt(
                veinArpg, "max_height_blocks", EngineSetting.DEFAULT_BIOME_VEIN_MAX_HEIGHT_BLOCKS);
        int maxDepthBlocks = ArpgUtility.getInt(
                veinArpg, "max_depth_blocks", EngineSetting.DEFAULT_BIOME_VEIN_MAX_DEPTH_BLOCKS);

        requireUnit(abundance, biomeName, "veins", "abundance");

        if (thicknessBlocks <= 0f)
            throw fail(biomeName, "\"veins\" entry \"" + blockName + "\" must have \"thickness_blocks\" "
                    + "greater than 0.");

        if (maxHeightBlocks < minHeightBlocks)
            throw fail(biomeName, "\"veins\" entry \"" + blockName + "\" has \"max_height_blocks\" "
                    + maxHeightBlocks + ", below its \"min_height_blocks\" " + minHeightBlocks + ".");

        if (maxDepthBlocks <= 0)
            throw fail(biomeName, "\"veins\" entry \"" + blockName + "\" must have \"max_depth_blocks\" "
                    + "greater than 0.");

        return new BiomeVeinStruct(
                blockName, abundance, thicknessBlocks, minHeightBlocks, maxHeightBlocks, maxDepthBlocks);
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

    private static void requireUnit(float value, String biomeName, String field, String key) {

        if (value < 0f || value > 1f)
            throw fail(biomeName, "\"" + field + "\" \"" + key + "\" is " + value
                    + " — it must run from 0 to 1.");
    }

    private static InternalException fail(String biomeName, String message) {
        return new InternalException("Biome \"" + biomeName + "\" " + message);
    }
}
