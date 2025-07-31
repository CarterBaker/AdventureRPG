package com.AdventureRPG.WorldSystem.Biomes;

import com.AdventureRPG.Util.Vector2Int;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.*;

public class BiomeDeserializer implements JsonDeserializer<Biome> {

    @Override
    public Biome deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        Biome.Builder builder = new Biome.Builder();

        // Base

        if (obj.has("name"))
            builder.name(obj.get("name").getAsString());
        if (obj.has("ID"))
            builder.ID(obj.get("ID").getAsInt());

        // Blending

        if (obj.has("similarBiomes")) {
            JsonArray similarArray = obj.getAsJsonArray("similarBiomes");
            Set<Integer> similarBiomes = new HashSet<>();
            for (JsonElement e : similarArray) {
                similarBiomes.add(e.getAsInt());
            }
            builder.similarBiomes(similarBiomes);
        }

        // Elevation

        if (obj.has("elevation"))
            builder.elevation(obj.get("elevation").getAsInt());
        if (obj.has("elevationBlending"))
            builder.elevationBlending(obj.get("elevationBlending").getAsInt());

        // Water Biomes

        if (obj.has("aquatic"))
            builder.aquatic(obj.get("aquatic").getAsBoolean());
        if (obj.has("ocean"))
            builder.ocean(obj.get("ocean").getAsBoolean());

        if (obj.has("waterNoiseScaleX"))
            builder.waterNoiseScaleX(obj.get("waterNoiseScaleX").getAsFloat());
        if (obj.has("waterNoiseScaleY"))
            builder.waterNoiseScaleY(obj.get("waterNoiseScaleY").getAsFloat());
        if (obj.has("waterThreshold"))
            builder.waterThreshold(obj.get("waterThreshold").getAsFloat());

        // SubTerrainian Biomes

        if (obj.has("allowCaves"))
            builder.allowCaves(obj.get("allowCaves").getAsBoolean());
        if (obj.has("subTerrainian"))
            builder.subTerrainian(obj.get("subTerrainian").getAsBoolean());
        if (obj.has("allowSurfaceBreak"))
            builder.allowSurfaceBreak(obj.get("allowSurfaceBreak").getAsBoolean());

        if (obj.has("caveNoiseScaleX"))
            builder.caveNoiseScaleX(obj.get("caveNoiseScaleX").getAsFloat());
        if (obj.has("caveNoiseScaleY"))
            builder.caveNoiseScaleY(obj.get("caveNoiseScaleY").getAsFloat());
        if (obj.has("caveNoiseScaleZ"))
            builder.caveNoiseScaleZ(obj.get("caveNoiseScaleZ").getAsFloat());
        if (obj.has("caveThreshold"))
            builder.caveThreshold(obj.get("caveThreshold").getAsFloat());

        // Block Composition

        if (obj.has("BiomeComposition")) {
            JsonObject compObj = obj.getAsJsonObject("BiomeComposition");
            Map<Integer, Vector2Int> composition = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : compObj.entrySet()) {
                int key = Integer.parseInt(entry.getKey());
                Vector2Int vec = context.deserialize(entry.getValue(), Vector2Int.class);
                composition.put(key, vec);
            }
            builder.BiomeComposition(composition);
        }

        return builder.build();
    }
}
