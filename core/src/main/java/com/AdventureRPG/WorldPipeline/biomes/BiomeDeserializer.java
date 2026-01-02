package com.AdventureRPG.WorldPipeline.biomes;

import java.lang.reflect.Type;

import com.badlogic.gdx.graphics.Color;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class BiomeDeserializer implements JsonDeserializer<Biome> {

    @Override
    public Biome deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext systemContext)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        Biome.Builder builder = new Biome.Builder();

        // Base \\

        if (obj.has("name"))
            builder.name(obj.get("name").getAsString());
        if (obj.has("id"))
            builder.id(obj.get("id").getAsInt());
        if (obj.has("biomeColor")) {
            String colorStr = obj.get("biomeColor").getAsString().trim();

            // Accept #RRGGBB or R,G,B or even named colors if you want
            if (colorStr.startsWith("#")) {
                // Parse hex
                builder.biomeColor(Color.valueOf(colorStr));
            } else if (colorStr.contains(",")) {
                // Parse "r,g,b" or "r,g,b,a"
                String[] parts = colorStr.split(",");
                float r = Float.parseFloat(parts[0]) / 255f;
                float g = Float.parseFloat(parts[1]) / 255f;
                float b = Float.parseFloat(parts[2]) / 255f;
                float a = parts.length > 3 ? Float.parseFloat(parts[3]) / 255f : 1f;
                builder.biomeColor(new Color(r, g, b, a));
            } else {
                throw new JsonParseException("Unsupported color format: " + colorStr);
            }
        }

        // Composition \\

        if (obj.has("airBlock"))
            builder.airBlock(obj.get("airBlock").getAsInt());

        return builder.build();
    }
}
