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

        // Window Settings
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

        // Render Settings
        if (obj.has("maxRenderDistance"))
            builder.maxRenderDistance(obj.get("maxRenderDistance").getAsInt());

        // Constant Settings \\

        // Thread Settings
        if (obj.has("AVAILABLE_THREADS"))
            builder.AVAILABLE_THREADS(obj.get("AVAILABLE_THREADS").getAsInt());

        // Path Settings
        if (obj.has("CALENDAR_JSON_PATH"))
            builder.CALENDAR_JSON_PATH(obj.get("CALENDAR_JSON_PATH").getAsString());
        if (obj.has("BLOCK_JSON_PATH"))
            builder.BLOCK_JSON_PATH(obj.get("BLOCK_JSON_PATH").getAsString());
        if (obj.has("BLOCK_TEXTURE_PATH"))
            builder.BLOCK_TEXTURE_PATH(obj.get("BLOCK_TEXTURE_PATH").getAsString());
        if (obj.has("BIOME_JSON_PATH"))
            builder.BIOME_JSON_PATH(obj.get("BIOME_JSON_PATH").getAsString());
        if (obj.has("REGION_IMAGE_PATH"))
            builder.REGION_IMAGE_PATH(obj.get("REGION_IMAGE_PATH").getAsString());

        // PBR Settings
        if (obj.has("NORMAL_MAP_DEFAULT")) {
            JsonObject colorObj = obj.getAsJsonObject("NORMAL_MAP_DEFAULT");
            builder.NORMAL_MAP_DEFAULT(new com.badlogic.gdx.graphics.Color(
                    colorObj.get("r").getAsFloat(),
                    colorObj.get("g").getAsFloat(),
                    colorObj.get("b").getAsFloat(),
                    colorObj.has("a") ? colorObj.get("a").getAsFloat() : 1f));
        }
        if (obj.has("HEIGHT_MAP_DEFAULT")) {
            JsonObject colorObj = obj.getAsJsonObject("HEIGHT_MAP_DEFAULT");
            builder.HEIGHT_MAP_DEFAULT(new com.badlogic.gdx.graphics.Color(
                    colorObj.get("r").getAsFloat(),
                    colorObj.get("g").getAsFloat(),
                    colorObj.get("b").getAsFloat(),
                    colorObj.has("a") ? colorObj.get("a").getAsFloat() : 1f));
        }
        if (obj.has("METAL_MAP_DEFAULT")) {
            JsonObject colorObj = obj.getAsJsonObject("METAL_MAP_DEFAULT");
            builder.METAL_MAP_DEFAULT(new com.badlogic.gdx.graphics.Color(
                    colorObj.get("r").getAsFloat(),
                    colorObj.get("g").getAsFloat(),
                    colorObj.get("b").getAsFloat(),
                    colorObj.has("a") ? colorObj.get("a").getAsFloat() : 1f));
        }
        if (obj.has("CUSTOM_MAP_DEFAULT")) {
            JsonObject colorObj = obj.getAsJsonObject("CUSTOM_MAP_DEFAULT");
            builder.CUSTOM_MAP_DEFAULT(new com.badlogic.gdx.graphics.Color(
                    colorObj.get("r").getAsFloat(),
                    colorObj.get("g").getAsFloat(),
                    colorObj.get("b").getAsFloat(),
                    colorObj.has("a") ? colorObj.get("a").getAsFloat() : 1f));
        }

        // Time Settings
        if (obj.has("MINUTES_PER_HOUR"))
            builder.MINUTES_PER_HOUR(obj.get("MINUTES_PER_HOUR").getAsInt());
        if (obj.has("HOURS_PER_DAY"))
            builder.HOURS_PER_DAY(obj.get("HOURS_PER_DAY").getAsInt());
        if (obj.has("DAYS_PER_DAY"))
            builder.DAYS_PER_DAY(obj.get("DAYS_PER_DAY").getAsInt());
        if (obj.has("MIDDAY_OFFSET"))
            builder.MIDDAY_OFFSET(obj.get("MIDDAY_OFFSET").getAsFloat());

        if (obj.has("STARTING_DAY"))
            builder.STARTING_DAY(obj.get("STARTING_DAY").getAsInt());
        if (obj.has("STARTING_MONTH"))
            builder.STARTING_MONTH(obj.get("STARTING_MONTH").getAsInt());
        if (obj.has("STARTING_YEAR"))
            builder.STARTING_YEAR(obj.get("STARTING_YEAR").getAsInt());
        if (obj.has("STARTING_AGE"))
            builder.STARTING_AGE(obj.get("STARTING_AGE").getAsInt());
        if (obj.has("YEARS_PER_AGE"))
            builder.YEARS_PER_AGE(obj.get("YEARS_PER_AGE").getAsInt());

        // Movement
        if (obj.has("BASE_WALKING_SPEED"))
            builder.BASE_WALKING_SPEED(obj.get("BASE_WALKING_SPEED").getAsFloat());

        // Atlas Settings
        if (obj.has("BLOCK_TEXTURE_SIZE"))
            builder.BLOCK_TEXTURE_SIZE(obj.get("BLOCK_TEXTURE_SIZE").getAsInt());
        if (obj.has("BLOCK_ATLAS_PADDING"))
            builder.BLOCK_ATLAS_PADDING(obj.get("BLOCK_ATLAS_PADDING").getAsInt());
        if (obj.has("CHUNKS_PER_PIXEL"))
            builder.CHUNKS_PER_PIXEL(obj.get("CHUNKS_PER_PIXEL").getAsInt());

        // Scale Settings
        if (obj.has("BLOCK_SIZE"))
            builder.BLOCK_SIZE(obj.get("BLOCK_SIZE").getAsFloat());
        if (obj.has("CHUNK_SIZE"))
            builder.CHUNK_SIZE(obj.get("CHUNK_SIZE").getAsInt());
        if (obj.has("WORLD_HEIGHT"))
            builder.WORLD_HEIGHT(obj.get("WORLD_HEIGHT").getAsInt());

        // World Tick Settings
        if (obj.has("WORLD_TICK"))
            builder.WORLD_TICK(obj.get("WORLD_TICK").getAsFloat());
        if (obj.has("MAX_CHUNK_LOADS_PER_FRAME"))
            builder.MAX_CHUNK_LOADS_PER_FRAME(obj.get("MAX_CHUNK_LOADS_PER_FRAME").getAsInt());
        if (obj.has("MAX_CHUNK_LOADS_PER_TICK"))
            builder.MAX_CHUNK_LOADS_PER_TICK(obj.get("MAX_CHUNK_LOADS_PER_TICK").getAsInt());

        return builder.build();
    }
}
