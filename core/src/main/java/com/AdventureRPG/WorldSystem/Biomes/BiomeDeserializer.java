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

        String name = obj.get("name").getAsString();
        int ID = obj.get("ID").getAsInt();

        Set<Integer> similarBiomes = new HashSet<>();
        JsonArray similarArray = obj.getAsJsonArray("similarBiomes");
        if (similarArray != null) {
            for (JsonElement e : similarArray) {
                similarBiomes.add(e.getAsInt());
            }
        }

        int elevation = obj.get("elevation").getAsInt();
        int elevationBlending = obj.get("elevationBlending").getAsInt();

        boolean aquatic = obj.get("aquatic").getAsBoolean();
        boolean ocean = obj.get("ocean").getAsBoolean();

        boolean allowCaves = obj.get("allowCaves").getAsBoolean();
        boolean subTerrainian = obj.get("subTerrainian").getAsBoolean();
        boolean allowSurfaceBreak = obj.get("allowSurfaceBreak").getAsBoolean();

        float caveNoiseScaleX = obj.get("caveNoiseScaleX").getAsFloat();
        float caveNoiseScaleY = obj.get("caveNoiseScaleY").getAsFloat();
        float caveNoiseScaleZ = obj.get("caveNoiseScaleZ").getAsFloat();
        float caveThreshold = obj.get("caveThreshold").getAsFloat();

        Map<Integer, Vector2Int> composition = new HashMap<>();
        JsonObject compObj = obj.getAsJsonObject("BiomeComposition");
        if (compObj != null) {
            for (Map.Entry<String, JsonElement> entry : compObj.entrySet()) {
                int key = Integer.parseInt(entry.getKey());
                Vector2Int vec = context.deserialize(entry.getValue(), Vector2Int.class);
                composition.put(key, vec);
            }
        }

        return new Biome.Builder()

                .name(name)
                .ID(ID)

                .similarBiomes(similarBiomes)

                .elevation(elevation)
                .elevationBlending(elevationBlending)

                .aquatic(aquatic)
                .ocean(ocean)

                .allowCaves(allowCaves)
                .subTerrainian(subTerrainian)
                .allowSurfaceBreak(allowSurfaceBreak)

                .caveNoiseScaleX(caveNoiseScaleX)
                .caveNoiseScaleY(caveNoiseScaleY)
                .caveNoiseScaleZ(caveNoiseScaleZ)
                .caveThreshold(caveThreshold)

                .BiomeComposition(composition)

                .build();
    }
}
