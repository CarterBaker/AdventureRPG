package com.AdventureRPG.WorldSystem.Blocks;

import com.google.gson.*;

import java.lang.reflect.Type;

public class BlockDeserializer implements JsonDeserializer<Block> {

    @Override
    public Block deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        Block.Builder builder = new Block.Builder();

        builder.name(obj.get("name").getAsString());
        builder.ID(obj.get("ID").getAsInt());

        if (obj.has("top"))
            builder.top(obj.get("top").getAsInt());
        if (obj.has("side"))
            builder.side(obj.get("side").getAsInt());
        if (obj.has("bottom"))
            builder.bottom(obj.get("bottom").getAsInt());

        try {
            String stateStr = obj.get("state").getAsString().toUpperCase();
            builder.state(State.valueOf(stateStr));
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Invalid state value: " + obj.get("state").getAsString(), e);
        }

        return builder.build();
    }
}
