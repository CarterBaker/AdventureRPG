package com.AdventureRPG.SettingsSystem;

import com.google.gson.*;
import java.lang.reflect.Type;

public class SettingsDeserializer implements JsonDeserializer<Settings> {

    @Override
    public Settings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        Settings.Builder builder = new Settings.Builder();

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

        // Movement \\

        if (obj.has("BASE_SPEED"))
            builder.BASE_SPEED(obj.get("BASE_SPEED").getAsFloat());

        // Block Settings \\

        if (obj.has("BLOCK_TEXTURE_PATH"))
            builder.BLOCK_TEXTURE_PATH(obj.get("BLOCK_TEXTURE_PATH").getAsString());
        if (obj.has("BLOCK_TEXTURE_SIZE"))
            builder.BLOCK_TEXTURE_SIZE(obj.get("BLOCK_TEXTURE_SIZE").getAsInt());
        if (obj.has("BLOCK_ATLAS_PADDING"))
            builder.BLOCK_ATLAS_PADDING(obj.get("BLOCK_ATLAS_PADDING").getAsInt());

        // Region Map Settings \\

        if (obj.has("REGION_IMAGE_PATH"))
            builder.REGION_IMAGE_PATH(obj.get("REGION_IMAGE_PATH").getAsString());

        // Chunk Settings \\

        if (obj.has("BLOCK_SIZE"))
            builder.BLOCK_SIZE(obj.get("BLOCK_SIZE").getAsFloat());
        if (obj.has("CHUNK_SIZE"))
            builder.CHUNK_SIZE(obj.get("CHUNK_SIZE").getAsInt());
        if (obj.has("CHUNKS_PER_PIXEL"))
            builder.CHUNKS_PER_PIXEL(obj.get("CHUNKS_PER_PIXEL").getAsInt());

        // Render Settings \\

        if (obj.has("MAX_RENDER_DISTANCE"))
            builder.MAX_RENDER_DISTANCE(obj.get("MAX_RENDER_DISTANCE").getAsInt());
        if (obj.has("MAX_RENDER_HEIGHT"))
            builder.MAX_RENDER_HEIGHT(obj.get("MAX_RENDER_HEIGHT").getAsInt());

        // LOD Settings \\

        if (obj.has("LOD_START_DISTANCE"))
            builder.LOD_START_DISTANCE(obj.get("LOD_START_DISTANCE").getAsInt());
        if (obj.has("MAX_LOD_DISTANCE"))
            builder.MAX_LOD_DISTANCE(obj.get("MAX_LOD_DISTANCE").getAsInt());

        // Tick \\

        if (obj.has("WORLD_TICK"))
            builder.WORLD_TICK(obj.get("WORLD_TICK").getAsFloat());

        // Loader Settings \\

        if (obj.has("MAX_CHUNK_LOADS_PER_FRAME"))
            builder.MAX_CHUNK_LOADS_PER_FRAME(obj.get("MAX_CHUNK_LOADS_PER_FRAME").getAsInt());
        if (obj.has("MAX_CHUNK_LOADS_PER_TICK"))
            builder.MAX_CHUNK_LOADS_PER_TICK(obj.get("MAX_CHUNK_LOADS_PER_TICK").getAsInt());

        // Biome Settings \\

        if (obj.has("BIOME_PATH"))
            builder.BIOME_PATH(obj.get("BIOME_PATH").getAsString());

        // World Generation \\

        if (obj.has("BASE_WORLD_ELEVATION"))
            builder.BASE_WORLD_ELEVATION(obj.get("BASE_WORLD_ELEVATION").getAsInt());
        if (obj.has("MIN_WORLD_ELEVATION"))
            builder.MIN_WORLD_ELEVATION(obj.get("MIN_WORLD_ELEVATION").getAsInt());
        if (obj.has("MAX_WORLD_ELEVATION"))
            builder.MAX_WORLD_ELEVATION(obj.get("MAX_WORLD_ELEVATION").getAsInt());

        if (obj.has("BASE_ELEVATION_BLENDING"))
            builder.BASE_ELEVATION_BLENDING(obj.get("BASE_ELEVATION_BLENDING").getAsInt());

        if (obj.has("BASE_OCEAN_LEVEL"))
            builder.BASE_OCEAN_LEVEL(obj.get("BASE_OCEAN_LEVEL").getAsInt());
        if (obj.has("OCEAN_TIDE_OFFSET"))
            builder.OCEAN_TIDE_OFFSET(obj.get("OCEAN_TIDE_OFFSET").getAsInt());

        if (obj.has("WATER_NOISE_OFFSET"))
            builder.WATER_NOISE_OFFSET(obj.get("WATER_NOISE_OFFSET").getAsInt());
        if (obj.has("WATER_HEIGHT_OFFSET"))
            builder.WATER_HEIGHT_OFFSET(obj.get("WATER_HEIGHT_OFFSET").getAsInt());

        if (obj.has("MIN_CAVE_ELEVATION"))
            builder.MIN_CAVE_ELEVATION(obj.get("MIN_CAVE_ELEVATION").getAsInt());

        if (obj.has("CAVE_NOISE_OFFSET"))
            builder.CAVE_NOISE_OFFSET(obj.get("CAVE_NOISE_OFFSET").getAsInt());
        if (obj.has("SURFACE_BREAK__OFFSET"))
            builder.SURFACE_BREAK__OFFSET(obj.get("SURFACE_BREAK__OFFSET").getAsInt());

        if (obj.has("BIOME_BLEND_OFFSET"))
            builder.BIOME_BLEND_OFFSET(obj.get("BIOME_BLEND_OFFSET").getAsInt());

        return builder.build();
    }
}
