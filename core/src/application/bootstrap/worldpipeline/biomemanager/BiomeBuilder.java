package application.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.extras.LinearSpline;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BiomeBuilder extends BuilderPackage {

    /*
     * Parses biome ARPG into BiomeData wrapped in a BiomeHandle: display name,
     * weathers, map color, probable variants, surface blocks, ocean and beach
     * settings, and the optional terrain shape splines and detail controls,
     * each falling back to TerrainShapeUtility's defaults. Everything is
     * validated at load, so a malformed biome fails at boot.
     */

    // Build \\

    BiomeHandle build(File file, File root) {

        String biomeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short biomeID = RegistryUtility.toShortID(biomeName);

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        ObjectArrayList<String> seasonNames = new ObjectArrayList<>();
        Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances = new Object2ObjectOpenHashMap<>();

        parseWeathers(arpg, seasonNames, seasonWeatherNames, seasonWeatherChances);

        int mapColor = parseMapColor(arpg, biomeName);
        String displayName = parseDisplayName(arpg, biomeName, mapColor);

        ObjectArrayList<String> probableBiomeNames = new ObjectArrayList<>();
        FloatArrayList probableBiomeChances = new FloatArrayList();

        parseProbableBiomes(arpg, biomeName, probableBiomeNames, probableBiomeChances);

        String surfaceBlockName = parseBlockName(arpg, "surface_block", EngineSetting.DEFAULT_SURFACE_BLOCK_NAME);
        String subsurfaceBlockName = parseBlockName(
                arpg, "subsurface_block", EngineSetting.DEFAULT_SUBSURFACE_BLOCK_NAME);
        String underwaterBlockName = parseBlockName(
                arpg, "underwater_block", EngineSetting.DEFAULT_UNDERWATER_BLOCK_NAME);

        boolean oceanWater = ArpgUtility.getBoolean(arpg, "ocean_water", false);
        String beachBiomeName = parseBeachBiomeName(arpg, biomeName, oceanWater);

        LinearSpline continentalnessSpline = parseSpline(
                arpg, "continentalness_spline", "x", "height_blocks",
                TerrainShapeUtility.DEFAULT_CONTINENTALNESS_SPLINE, biomeName);

        LinearSpline erosionSpline = parseSpline(
                arpg, "erosion_spline", "x", "amplitude_blocks",
                TerrainShapeUtility.DEFAULT_EROSION_SPLINE, biomeName);

        LinearSpline peaksValleysSpline = parseSpline(
                arpg, "peaks_valleys_spline", "x", "contribution",
                TerrainShapeUtility.DEFAULT_PEAKS_VALLEYS_SPLINE, biomeName);

        float detailAmplitudeBlocks = arpg.has("detail_amplitude_blocks")
                ? arpg.get("detail_amplitude_blocks").getAsFloat()
                : EngineSetting.TERRAIN_DETAIL_AMPLITUDE_BLOCKS;

        float detailWavelengthBlocks = arpg.has("detail_wavelength_blocks")
                ? arpg.get("detail_wavelength_blocks").getAsFloat()
                : EngineSetting.TERRAIN_DETAIL_WAVELENGTH_BLOCKS;

        if (detailWavelengthBlocks <= 0f)
            throwException("Biome \"" + biomeName + "\" \"detail_wavelength_blocks\" must be greater than 0.");

        float terrainHeightScale = arpg.has("terrain_height_scale")
                ? arpg.get("terrain_height_scale").getAsFloat()
                : EngineSetting.DEFAULT_BIOME_TERRAIN_HEIGHT_SCALE;

        BiomeData biomeData = new BiomeData(
                biomeName, displayName, biomeID, Color.WHITE,
                seasonWeatherNames, seasonWeatherChances, seasonNames,
                mapColor, probableBiomeNames, probableBiomeChances,
                surfaceBlockName, subsurfaceBlockName, underwaterBlockName,
                continentalnessSpline, erosionSpline, peaksValleysSpline,
                detailAmplitudeBlocks, detailWavelengthBlocks, terrainHeightScale,
                oceanWater, beachBiomeName);

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }

    // Display Name Parsing \\

    private String parseDisplayName(ArpgObjectStruct arpg, String biomeName, int mapColor) {

        if (!arpg.has("display_name")) {

            if (mapColor != BiomeData.MAP_COLOR_UNDEFINED)
                throwException("Biome \"" + biomeName + "\" is painted on the world map but declares no "
                        + "\"display_name\" — only variants linked through \"probable_biomes\" may be unnamed.");

            return null;
        }

        String displayName = arpg.get("display_name").getAsString().trim();

        if (displayName.isEmpty())
            throwException("Biome \"" + biomeName + "\" declares an empty \"display_name\".");

        return displayName;
    }

    // Weather Parsing \\

    private void parseWeathers(
            ArpgObjectStruct arpg,
            ObjectArrayList<String> outSeasonNames,
            Object2ObjectOpenHashMap<String, ObjectArrayList<String>> outSeasonWeatherNames,
            Object2ObjectOpenHashMap<String, FloatArrayList> outSeasonWeatherChances) {

        if (!arpg.has("weathers"))
            return;

        ArpgObjectStruct weathersObject = arpg.getAsObject("weathers");

        for (String seasonName : weathersObject.keySet()) {

            ArpgArrayStruct weatherArray = weathersObject.getAsArray(seasonName);

            ObjectArrayList<String> names = new ObjectArrayList<>(weatherArray.size());
            FloatArrayList chances = new FloatArrayList(weatherArray.size());

            for (ArpgElementStruct element : weatherArray)
                parseWeatherEntry(element, names, chances);

            outSeasonWeatherNames.put(seasonName, names);
            outSeasonWeatherChances.put(seasonName, chances);
            outSeasonNames.add(seasonName);
        }
    }

    private void parseWeatherEntry(ArpgElementStruct element, ObjectArrayList<String> names, FloatArrayList chances) {

        if (element.isValue()) {
            names.add(element.getAsString());
            chances.add(EngineSetting.DEFAULT_BIOME_WEATHER_CHANCE);
            return;
        }

        ArpgObjectStruct entryObject = element.getAsObject();
        String weatherName = ArpgUtility.validateString(entryObject, "name");
        float chance = entryObject.has("chance")
                ? entryObject.get("chance").getAsFloat()
                : EngineSetting.DEFAULT_BIOME_WEATHER_CHANCE;

        names.add(weatherName);
        chances.add(chance);
    }

    // Map Color Parsing \\

    private int parseMapColor(ArpgObjectStruct arpg, String biomeName) {

        if (!arpg.has("map_color"))
            return BiomeData.MAP_COLOR_UNDEFINED;

        String raw = arpg.get("map_color").getAsString();
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;

        if (hex.length() != 6)
            throwException("Biome \"" + biomeName + "\" has invalid map_color \"" + raw
                    + "\" — expected a 6-digit hex RGB value, e.g. \"#5B8C3A\".");

        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throwException("Biome \"" + biomeName + "\" has invalid map_color \"" + raw + "\" — not valid hex.", e);
            return BiomeData.MAP_COLOR_UNDEFINED;
        }
    }

    // Probable Biome Parsing \\

    private void parseProbableBiomes(
            ArpgObjectStruct arpg,
            String biomeName,
            ObjectArrayList<String> outNames,
            FloatArrayList outChances) {

        if (!arpg.has("probable_biomes"))
            return;

        ArpgArrayStruct array = arpg.getAsArray("probable_biomes");
        float runningTotal = 0f;

        for (ArpgElementStruct element : array) {

            ArpgObjectStruct entryObject = element.getAsObject();
            String variantName = ArpgUtility.validateString(entryObject, "name");

            if (!entryObject.has("chance"))
                throwException("Biome \"" + biomeName + "\" probable_biomes entry \"" + variantName
                        + "\" is missing required \"chance\" field.");

            float chance = entryObject.get("chance").getAsFloat();

            if (chance <= 0f || chance > 1f)
                throwException("Biome \"" + biomeName + "\" probable_biomes entry \"" + variantName
                        + "\" has chance " + chance + " — chance must be greater than 0 and no more than 1.");

            runningTotal += chance;

            if (runningTotal > 1f)
                throwException("Biome \"" + biomeName + "\" probable_biomes chances sum to " + runningTotal
                        + ", which exceeds 1.0 — reduce the chances so the base biome retains some probability.");

            outNames.add(variantName);
            outChances.add(chance);
        }
    }

    // Beach Biome Parsing \\

    private String parseBeachBiomeName(ArpgObjectStruct arpg, String biomeName, boolean oceanWater) {

        if (!arpg.has("beach_biome"))
            return null;

        String beachBiomeName = arpg.get("beach_biome").getAsString();

        if (oceanWater)
            throwException("Biome \"" + biomeName + "\" is an ocean and declares \"beach_biome\" \""
                    + beachBiomeName + "\" — beaches are declared by the land biomes an ocean borders.");

        if (beachBiomeName.equals(biomeName))
            throwException("Biome \"" + biomeName + "\" declares itself as its own \"beach_biome\".");

        return beachBiomeName;
    }

    // Terrain Shape Parsing \\

    private LinearSpline parseSpline(
            ArpgObjectStruct arpg,
            String field,
            String xKey,
            String yKey,
            LinearSpline defaultSpline,
            String biomeName) {

        if (!arpg.has(field) || arpg.get(field).isNull())
            return defaultSpline;

        ArpgObjectStruct splineArpg = arpg.getAsObject(field);

        if (!splineArpg.has(xKey) || !splineArpg.has(yKey))
            throwException("Biome \"" + biomeName + "\" \"" + field + "\" must declare both \""
                    + xKey + "\" and \"" + yKey + "\".");

        float[] x = parseFloatArray(splineArpg.getAsArray(xKey));
        float[] y = parseFloatArray(splineArpg.getAsArray(yKey));

        if (x.length != y.length)
            throwException("Biome \"" + biomeName + "\" \"" + field + "\" has " + x.length + " \"" + xKey
                    + "\" entries but " + y.length + " \"" + yKey + "\" entries — they must match.");

        if (x.length < 2)
            throwException("Biome \"" + biomeName + "\" \"" + field + "\" needs at least 2 control points.");

        for (int i = 1; i < x.length; i++)
            if (x[i] <= x[i - 1])
                throwException("Biome \"" + biomeName + "\" \"" + field + "\" \"" + xKey
                        + "\" values must be strictly increasing.");

        return new LinearSpline(x, y);
    }

    private float[] parseFloatArray(ArpgArrayStruct array) {
        float[] result = new float[array.size()];
        for (int i = 0; i < array.size(); i++)
            result[i] = array.get(i).getAsFloat();
        return result;
    }

    // Terrain Block Parsing \\

    private String parseBlockName(ArpgObjectStruct arpg, String field, String fallback) {
        return arpg.has(field) ? arpg.get(field).getAsString() : fallback;
    }
}