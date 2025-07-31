package com.AdventureRPG.SettingsSystem;

import com.google.gson.*;
import java.lang.reflect.Type;

public class SettingsDeserializer implements JsonDeserializer<Settings> {

    @Override
    public Settings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        Settings.Builder builder = new Settings.Builder();

        if (obj.has("BASE_SPEED"))
            builder.setBaseSpeed(obj.get("BASE_SPEED").getAsFloat());

        if (obj.has("REGION_IMAGE_PATH"))
            builder.setRegionImagePath(obj.get("REGION_IMAGE_PATH").getAsString());

        if (obj.has("BLOCK_SIZE") && obj.has("CHUNK_SIZE") && obj.has("CHUNKS_PER_PIXEL")) {
            builder.setChunkSettings(
                    obj.get("BLOCK_SIZE").getAsFloat(),
                    obj.get("CHUNK_SIZE").getAsInt(),
                    obj.get("CHUNKS_PER_PIXEL").getAsInt());
        }

        if (obj.has("MAX_RENDER_DISTANCE") && obj.has("MAX_RENDER_HEIGHT")) {
            builder.setRenderSettings(
                    obj.get("MAX_RENDER_DISTANCE").getAsInt(),
                    obj.get("MAX_RENDER_HEIGHT").getAsInt());
        }

        if (obj.has("LOD_START_DISTANCE") && obj.has("MAX_LOD_DISTANCE")) {
            builder.setLOD(
                    obj.get("LOD_START_DISTANCE").getAsInt(),
                    obj.get("MAX_LOD_DISTANCE").getAsInt());
        }

        if (obj.has("WORLD_TICK"))
            builder.setWorldTick(obj.get("WORLD_TICK").getAsFloat());

        if (obj.has("MAX_CHUNK_LOADS_PER_FRAME") && obj.has("MAX_CHUNK_LOADS_PER_TICK")) {
            builder.setLoaderLimits(
                    obj.get("MAX_CHUNK_LOADS_PER_FRAME").getAsInt(),
                    obj.get("MAX_CHUNK_LOADS_PER_TICK").getAsInt());
        }

        if (obj.has("BIOME_PATH"))
            builder.setBiomePath(obj.get("BIOME_PATH").getAsString());

        if (obj.has("BASE_WORLD_ELEVATION") && obj.has("MIN_WORLD_ELEVATION") && obj.has("MAX_WORLD_ELEVATION")) {
            builder.setWorldGen(
                    obj.get("BASE_WORLD_ELEVATION").getAsInt(),
                    obj.get("MIN_WORLD_ELEVATION").getAsInt(),
                    obj.get("MAX_WORLD_ELEVATION").getAsInt());
        }

        if (obj.has("BASE_ELEVATION_BLENDING"))
            builder.setElevationBlending(obj.get("BASE_ELEVATION_BLENDING").getAsInt());

        if (obj.has("BASE_OCEAN_LEVEL") && obj.has("OCEAN_TIDE_OFFSET")) {
            builder.setOceanSettings(
                    obj.get("BASE_OCEAN_LEVEL").getAsInt(),
                    obj.get("OCEAN_TIDE_OFFSET").getAsInt());
        }

        if (obj.has("WATER_NOISE_OFFSET"))
            builder.setWaterOffset(obj.get("WATER_NOISE_OFFSET").getAsInt());

        if (obj.has("MIN_CAVE_ELEVATION"))
            builder.setCaveSettings(obj.get("MIN_CAVE_ELEVATION").getAsInt());

        return builder.build();
    }
}