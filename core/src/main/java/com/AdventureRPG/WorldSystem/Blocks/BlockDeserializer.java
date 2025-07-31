package com.AdventureRPG.WorldSystem.Blocks;

import com.google.gson.*;

import java.lang.reflect.Type;

public class BlockDeserializer implements JsonDeserializer<Block> {

    @Override
    public Block deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();

        String name = obj.get("name").getAsString();
        int ID = obj.get("ID").getAsInt();
        int top = obj.get("top").getAsInt();
        int side = obj.get("side").getAsInt();
        int bottom = obj.get("bottom").getAsInt();

        String stateStr = obj.get("state").getAsString().toUpperCase();
        State state = State.valueOf(stateStr);

        return new Block.Builder()
                .name(name)
                .ID(ID)
                .top(top)
                .side(side)
                .bottom(bottom)
                .state(state)
                .build();
    }
}
