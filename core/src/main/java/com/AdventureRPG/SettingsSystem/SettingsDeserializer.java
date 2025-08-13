package com.AdventureRPG.SettingsSystem;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class SettingsDeserializer implements JsonDeserializer<Settings> {

    @Override
    public Settings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        Settings.Builder builder = new Settings.Builder();

        // Runtime Settings \\

        // Window Settings \\

        if (obj.has("FOV"))
            builder.FOV(obj.get("FOV").getAsFloat());
        if (obj.has("windowWidth"))
            builder.windowWidth(obj.get("windowWidth").getAsInt());
        if (obj.has("windowHeight"))
            builder.windowHeight(obj.get("windowHeight").getAsInt());
        if (obj.has("windowX"))
            builder.windowX(obj.get("windowX").getAsInt());
        if (obj.has("windowY"))
            builder.windowY(obj.get("windowY").getAsInt());
        if (obj.has("fullscreen"))
            builder.fullscreen(obj.get("fullscreen").getAsBoolean());

        // Render Settings \\

        if (obj.has("maxRenderDistance"))
            builder.maxRenderDistance(obj.get("maxRenderDistance").getAsInt());

        // Constant Settings \\

        // Movement \\

        if (obj.has("BASE_WALKING_SPEED"))
            builder.BASE_WALKING_SPEED(obj.get("BASE_WALKING_SPEED").getAsFloat());

        // Path Settings \\

        if (obj.has("BLOCK_TEXTURE_PATH"))
            builder.BLOCK_TEXTURE_PATH(obj.get("BLOCK_TEXTURE_PATH").getAsString());
        if (obj.has("BIOME_JSON_PATH"))
            builder.BIOME_JSON_PATH(obj.get("BIOME_JSON_PATH").getAsString());
        if (obj.has("REGION_IMAGE_PATH"))
            builder.REGION_IMAGE_PATH(obj.get("REGION_IMAGE_PATH").getAsString());

        // Atlas Settings \\

        if (obj.has("BLOCK_TEXTURE_SIZE"))
            builder.BLOCK_TEXTURE_SIZE(obj.get("BLOCK_TEXTURE_SIZE").getAsInt());
        if (obj.has("BLOCK_ATLAS_PADDING"))
            builder.BLOCK_ATLAS_PADDING(obj.get("BLOCK_ATLAS_PADDING").getAsInt());
        if (obj.has("CHUNKS_PER_PIXEL"))
            builder.CHUNKS_PER_PIXEL(obj.get("CHUNKS_PER_PIXEL").getAsInt());

        // Scale Settings \\

        if (obj.has("BLOCK_SIZE"))
            builder.BLOCK_SIZE(obj.get("BLOCK_SIZE").getAsFloat());
        if (obj.has("CHUNK_SIZE"))
            builder.CHUNK_SIZE(obj.get("CHUNK_SIZE").getAsInt());
        if (obj.has("WORLD_HEIGHT"))
            builder.WORLD_HEIGHT(obj.get("WORLD_HEIGHT").getAsInt());

        // World Tick Settings \\

        if (obj.has("WORLD_TICK"))
            builder.WORLD_TICK(obj.get("WORLD_TICK").getAsFloat());
        if (obj.has("MAX_CHUNK_LOADS_PER_FRAME"))
            builder.MAX_CHUNK_LOADS_PER_FRAME(obj.get("MAX_CHUNK_LOADS_PER_FRAME").getAsInt());
        if (obj.has("MAX_CHUNK_LOADS_PER_TICK"))
            builder.MAX_CHUNK_LOADS_PER_TICK(obj.get("MAX_CHUNK_LOADS_PER_TICK").getAsInt());

        return builder.build();
    }
}
