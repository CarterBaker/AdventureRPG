package com.AdventureRPG.WorldSystem.Blocks;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class BlockDeserializer implements JsonDeserializer<Builder> {

    @Override
    public Builder deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        Builder builder = new Builder();

        builder.name(obj.get("name").getAsString());

        if (obj.has("texture"))
            builder.texture(obj.get("texture").getAsString());

        else {
            if (obj.has("upTex"))
                builder.upTex(obj.get("upTex").getAsString());
            if (obj.has("northTex"))
                builder.northTex(obj.get("northTex").getAsString());
            if (obj.has("southTex"))
                builder.southTex(obj.get("southTex").getAsString());
            if (obj.has("eastTex"))
                builder.eastTex(obj.get("eastTex").getAsString());
            if (obj.has("westTex"))
                builder.westTex(obj.get("westTex").getAsString());
            if (obj.has("downTex"))
                builder.downTex(obj.get("downTex").getAsString());
        }

        if (obj.has("material"))
            builder.material(obj.get("material").getAsString());

        else {
            if (obj.has("upMat"))
                builder.upMat(obj.get("upMat").getAsString());
            if (obj.has("northMat"))
                builder.northMat(obj.get("northMat").getAsString());
            if (obj.has("southMat"))
                builder.southMat(obj.get("southMat").getAsString());
            if (obj.has("eastMat"))
                builder.eastMat(obj.get("eastMat").getAsString());
            if (obj.has("westMat"))
                builder.westMat(obj.get("westMat").getAsString());
            if (obj.has("downMat"))
                builder.downMat(obj.get("downMat").getAsString());
        }

        try {
            String stateStr = obj.get("type").getAsString().toUpperCase();
            builder.type(com.AdventureRPG.WorldSystem.Blocks.Type.valueOf(stateStr));
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Invalid state value: " + obj.get("state").getAsString(), e);
        }

        return builder;
    }
}
