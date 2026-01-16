package com.internal.core.engine.settings;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class SettingsDeserializer implements JsonDeserializer<Settings> {

    @Override
    public Settings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext internalContext)
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

        return builder.build();
    }
}
