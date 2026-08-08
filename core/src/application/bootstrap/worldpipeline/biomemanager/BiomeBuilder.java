package application.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BiomeBuilder extends BuilderPackage {

    /*
     * Parses biome JSON into a BiomeData and wraps it in a BiomeHandle. Reads
     * the optional "weathers" block into a per-season pool of parallel
     * name/chance fastutil lists that WeatherManager resolves into live
     * WeatherHandles on demand. Season names are read directly from whatever
     * keys appear in the "weathers" object. Also reads the optional
     * "map_color" hex RGB value this biome matches against the world PNG,
     * and the optional "probable_biomes" list of alternate biomes that may
     * replace this one during generation — both validated fully at load
     * time so a malformed biome file fails at boot rather than mid-game.
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

        BiomeData biomeData = new BiomeData(
                biomeName, biomeID, Color.WHITE,
                seasonWeatherNames, seasonWeatherChances, seasonNames,
                mapColor, probableBiomeNames, probableBiomeChances);

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
}