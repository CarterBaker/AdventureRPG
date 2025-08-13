package com.AdventureRPG.WorldSystem.Biomes;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class BiomeDeserializer implements JsonDeserializer<Biome> {

    @Override
    public Biome deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        Biome.Builder builder = new Biome.Builder();

        // Base \\

        if (obj.has("name"))
            builder.name(obj.get("name").getAsString());
        if (obj.has("id"))
            builder.id(obj.get("id").getAsInt());

        // Composition \\

        if (obj.has("airBlock"))
            builder.airBlock(obj.get("airBlock").getAsInt());

        return builder.build();
    }
}
