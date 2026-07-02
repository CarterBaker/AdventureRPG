package application.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.calendarpipeline.clock.Season;
import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses biome JSON into a BiomeData and wraps it in a BiomeHandle.
     * Reads the optional "weathers" block into a per-season name pool that
     * WeatherManager resolves into live WeatherHandles on demand.
     */

    // Build \\

    BiomeHandle build(File file, File root) {

        String biomeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short biomeID = RegistryUtility.toShortID(biomeName);

        JsonObject json = JsonUtility.loadJsonObject(file);
        Object2ObjectOpenHashMap<Season, ObjectArrayList<String>> seasonWeatherNames = parseWeathers(json);

        BiomeData biomeData = new BiomeData(biomeName, biomeID, Color.WHITE, seasonWeatherNames);

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }

    // Parsing \\

    private Object2ObjectOpenHashMap<Season, ObjectArrayList<String>> parseWeathers(JsonObject json) {

        Object2ObjectOpenHashMap<Season, ObjectArrayList<String>> seasonWeatherNames = new Object2ObjectOpenHashMap<>();

        if (!json.has("weathers"))
            return seasonWeatherNames;

        JsonObject weathersObject = json.getAsJsonObject("weathers");

        for (Season season : Season.values()) {

            if (!weathersObject.has(season.name()))
                continue;

            JsonArray weatherArray = weathersObject.getAsJsonArray(season.name());
            ObjectArrayList<String> weatherNames = new ObjectArrayList<>(weatherArray.size());

            for (JsonElement element : weatherArray)
                weatherNames.add(element.getAsString());

            seasonWeatherNames.put(season, weatherNames);
        }

        return seasonWeatherNames;
    }
}