package com.AdventureRPG.core.engine.settings;

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

        // Physics Settings
        if (obj.has("FIXED_TIME_STEP"))
            builder.FIXED_TIME_STEP(obj.get("FIXED_TIME_STEP").getAsFloat());

        // Thread Settings
        if (obj.has("AI_AVAILABLE_THREADS"))
            builder.AI_AVAILABLE_THREADS(obj.get("AI_AVAILABLE_THREADS").getAsInt());
        if (obj.has("GENERATION_AVAILABLE_THREADS"))
            builder.GENERATION_AVAILABLE_THREADS(obj.get("GENERATION_AVAILABLE_THREADS").getAsInt());
        if (obj.has("GENERAL_AVAILABLE_THREADS"))
            builder.GENERAL_AVAILABLE_THREADS(obj.get("GENERAL_AVAILABLE_THREADS").getAsInt());

        return builder.build();
    }
}
