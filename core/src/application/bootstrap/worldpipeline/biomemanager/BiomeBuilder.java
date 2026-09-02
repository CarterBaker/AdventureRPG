package application.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.mathematics.extras.LinearSpline;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BiomeBuilder extends BuilderPackage {

    /*
     * Parses biome JSON into a BiomeData and wraps it in a BiomeHandle. Reads the
     * optional "weathers"
     * block, "map_color", "probable_biomes", surface/subsurface/underwater block
     * names, the boolean "ocean_water" flag that gates whether this biome's
     * below-sea-level terrain is flooded at all (see WorldGenerationManager),
     * and the terrain
     * shape controls — "continentalness_spline", "erosion_spline",
     * "peaks_valleys_spline",
     * "detail_amplitude_blocks", "detail_wavelength_blocks", and
     * "terrain_height_scale" — each falling
     * back to TerrainShapeUtility's global default when omitted, so an unmodified
     * biome file generates
     * exactly the terrain it always has while a fully-authored one can sculpt its
     * own distinct shape,
     * all validated at load time so a malformed biome file fails at boot rather
     * than mid-game.
     */

    // Build \\

    BiomeHandle build(File file, File root) {

        String biomeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short biomeID = RegistryUtility.toShortID(biomeName);

        JsonObject json = JsonUtility.loadJsonObject(file);

        ObjectArrayList<String> seasonNames = new ObjectArrayList<>();
        Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances = new Object2ObjectOpenHashMap<>();

        parseWeathers(json, seasonNames, seasonWeatherNames, seasonWeatherChances);

        int mapColor = parseMapColor(json, biomeName);

        ObjectArrayList<String> probableBiomeNames = new ObjectArrayList<>();
        FloatArrayList probableBiomeChances = new FloatArrayList();

        parseProbableBiomes(json, biomeName, probableBiomeNames, probableBiomeChances);

        String surfaceBlockName = parseBlockName(json, "surface_block", EngineSetting.DEFAULT_SURFACE_BLOCK_NAME);
        String subsurfaceBlockName = parseBlockName(
                json, "subsurface_block", EngineSetting.DEFAULT_SUBSURFACE_BLOCK_NAME);
        String underwaterBlockName = parseBlockName(
                json, "underwater_block", EngineSetting.DEFAULT_UNDERWATER_BLOCK_NAME);

        boolean oceanWater = JsonUtility.getBoolean(json, "ocean_water", false);

        LinearSpline continentalnessSpline = parseSpline(
                json, "continentalness_spline", "x", "height_blocks",
                TerrainShapeUtility.DEFAULT_CONTINENTALNESS_SPLINE, biomeName);

        LinearSpline erosionSpline = parseSpline(
                json, "erosion_spline", "x", "amplitude_blocks",
                TerrainShapeUtility.DEFAULT_EROSION_SPLINE, biomeName);

        LinearSpline peaksValleysSpline = parseSpline(
                json, "peaks_valleys_spline", "x", "contribution",
                TerrainShapeUtility.DEFAULT_PEAKS_VALLEYS_SPLINE, biomeName);

        float detailAmplitudeBlocks = json.has("detail_amplitude_blocks")
                ? json.get("detail_amplitude_blocks").getAsFloat()
                : EngineSetting.TERRAIN_DETAIL_AMPLITUDE_BLOCKS;

        float detailWavelengthBlocks = json.has("detail_wavelength_blocks")
                ? json.get("detail_wavelength_blocks").getAsFloat()
                : EngineSetting.TERRAIN_DETAIL_WAVELENGTH_BLOCKS;

        if (detailWavelengthBlocks <= 0f)
            throwException("Biome \"" + biomeName + "\" \"detail_wavelength_blocks\" must be greater than 0.");

        float terrainHeightScale = json.has("terrain_height_scale")
                ? json.get("terrain_height_scale").getAsFloat()
                : EngineSetting.DEFAULT_BIOME_TERRAIN_HEIGHT_SCALE;

        BiomeData biomeData = new BiomeData(
                biomeName, biomeID, Color.WHITE,
                seasonWeatherNames, seasonWeatherChances, seasonNames,
                mapColor, probableBiomeNames, probableBiomeChances,
                surfaceBlockName, subsurfaceBlockName, underwaterBlockName,
                continentalnessSpline, erosionSpline, peaksValleysSpline,
                detailAmplitudeBlocks, detailWavelengthBlocks, terrainHeightScale,
                oceanWater);

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }

    // Weather Parsing \\

    private void parseWeathers(
            JsonObject json,
            ObjectArrayList<String> outSeasonNames,
            Object2ObjectOpenHashMap<String, ObjectArrayList<String>> outSeasonWeatherNames,
            Object2ObjectOpenHashMap<String, FloatArrayList> outSeasonWeatherChances) {

        if (!json.has("weathers"))
            return;

        JsonObject weathersObject = json.getAsJsonObject("weathers");

        for (String seasonName : weathersObject.keySet()) {

            JsonArray weatherArray = weathersObject.getAsJsonArray(seasonName);

            ObjectArrayList<String> names = new ObjectArrayList<>(weatherArray.size());
            FloatArrayList chances = new FloatArrayList(weatherArray.size());

            for (JsonElement element : weatherArray)
                parseWeatherEntry(element, names, chances);

            outSeasonWeatherNames.put(seasonName, names);
            outSeasonWeatherChances.put(seasonName, chances);
            outSeasonNames.add(seasonName);
        }
    }

    private void parseWeatherEntry(JsonElement element, ObjectArrayList<String> names, FloatArrayList chances) {

        if (element.isJsonPrimitive()) {
            names.add(element.getAsString());
            chances.add(EngineSetting.DEFAULT_BIOME_WEATHER_CHANCE);
            return;
        }

        JsonObject entryObject = element.getAsJsonObject();
        String weatherName = JsonUtility.validateString(entryObject, "name");
        float chance = entryObject.has("chance")
                ? entryObject.get("chance").getAsFloat()
                : EngineSetting.DEFAULT_BIOME_WEATHER_CHANCE;

        names.add(weatherName);
        chances.add(chance);
    }

    // Map Color Parsing \\

    private int parseMapColor(JsonObject json, String biomeName) {

        if (!json.has("map_color"))
            return BiomeData.MAP_COLOR_UNDEFINED;

        String raw = json.get("map_color").getAsString();
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
            JsonObject json,
            String biomeName,
            ObjectArrayList<String> outNames,
            FloatArrayList outChances) {

        if (!json.has("probable_biomes"))
            return;

        JsonArray array = json.getAsJsonArray("probable_biomes");
        float runningTotal = 0f;

        for (JsonElement element : array) {

            JsonObject entryObject = element.getAsJsonObject();
            String variantName = JsonUtility.validateString(entryObject, "name");

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

    // Terrain Shape Parsing \\

    private LinearSpline parseSpline(
            JsonObject json,
            String field,
            String xKey,
            String yKey,
            LinearSpline defaultSpline,
            String biomeName) {

        if (!json.has(field) || json.get(field).isJsonNull())
            return defaultSpline;

        JsonObject splineJson = json.getAsJsonObject(field);

        if (!splineJson.has(xKey) || !splineJson.has(yKey))
            throwException("Biome \"" + biomeName + "\" \"" + field + "\" must declare both \""
                    + xKey + "\" and \"" + yKey + "\".");

        float[] x = parseFloatArray(splineJson.getAsJsonArray(xKey));
        float[] y = parseFloatArray(splineJson.getAsJsonArray(yKey));

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

    private float[] parseFloatArray(JsonArray array) {
        float[] result = new float[array.size()];
        for (int i = 0; i < array.size(); i++)
            result[i] = array.get(i).getAsFloat();
        return result;
    }

    // Terrain Block Parsing \\

    private String parseBlockName(JsonObject json, String field, String fallback) {
        return json.has(field) ? json.get(field).getAsString() : fallback;
    }
}