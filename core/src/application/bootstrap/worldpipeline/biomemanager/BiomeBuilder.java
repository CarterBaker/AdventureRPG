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
     * Parses biome JSON into a BiomeData and wraps it in a BiomeHandle.
     * Reads the optional "weathers" block into a per-season pool of
     * parallel name/chance fastutil lists that WeatherManager resolves
     * into live WeatherHandles on demand. Season names are read directly
     * from whatever keys appear in the "weathers" object — there's no
     * fixed set to validate against, since the active calendar is free to
     * define any named seasons it likes. Each season's array accepts
     * either a bare weather name string (given a default relative chance,
     * see EngineSetting.DEFAULT_BIOME_WEATHER_CHANCE) or an object with
     * explicit "name" and "chance" fields — both forms may be mixed
     * freely within one array.
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

        BiomeData biomeData = new BiomeData(
                biomeName, biomeID, Color.WHITE, seasonWeatherNames, seasonWeatherChances, seasonNames);

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }

    // Parsing \\

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
}