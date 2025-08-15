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

        if (obj.has("up"))
            builder.up(obj.get("up").getAsString());
        if (obj.has("north"))
            builder.north(obj.get("north").getAsString());
        if (obj.has("south"))
            builder.south(obj.get("south").getAsString());
        if (obj.has("east"))
            builder.east(obj.get("east").getAsString());
        if (obj.has("west"))
            builder.west(obj.get("west").getAsString());
        if (obj.has("down"))
            builder.down(obj.get("down").getAsString());

        try {
            String stateStr = obj.get("state").getAsString().toUpperCase();
            builder.state(State.valueOf(stateStr));
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Invalid state value: " + obj.get("state").getAsString(), e);
        }

        return builder;
    }
}
