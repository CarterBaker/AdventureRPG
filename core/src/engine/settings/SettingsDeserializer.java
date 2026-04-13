package engine.settings;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
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

        // Window
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

        // Render
        if (obj.has("maxRenderDistance"))
            builder.maxRenderDistance(obj.get("maxRenderDistance").getAsInt());

        // Input
        if (obj.has("mouseSensitivity"))
            builder.mouseSensitivity(obj.get("mouseSensitivity").getAsFloat());

        // Bindings — Game Movement
        if (obj.has("bindMoveForward"))
            builder.bindMoveForward(readKeys(obj, "bindMoveForward"));
        if (obj.has("bindMoveBack"))
            builder.bindMoveBack(readKeys(obj, "bindMoveBack"));
        if (obj.has("bindMoveLeft"))
            builder.bindMoveLeft(readKeys(obj, "bindMoveLeft"));
        if (obj.has("bindMoveRight"))
            builder.bindMoveRight(readKeys(obj, "bindMoveRight"));
        if (obj.has("bindJump"))
            builder.bindJump(readKeys(obj, "bindJump"));
        if (obj.has("bindWalk"))
            builder.bindWalk(readKeys(obj, "bindWalk"));
        if (obj.has("bindSprint"))
            builder.bindSprint(readKeys(obj, "bindSprint"));

        // Bindings — Game Actions
        if (obj.has("bindInventory"))
            builder.bindInventory(readKeys(obj, "bindInventory"));

        // Bindings — Editor Single
        if (obj.has("bindToggleInspector"))
            builder.bindToggleInspector(readKeys(obj, "bindToggleInspector"));
        if (obj.has("bindFocusSelected"))
            builder.bindFocusSelected(readKeys(obj, "bindFocusSelected"));
        if (obj.has("bindDeleteSelected"))
            builder.bindDeleteSelected(readKeys(obj, "bindDeleteSelected"));

        // Bindings — Editor Combos
        if (obj.has("bindSave"))
            builder.bindSave(readKeys(obj, "bindSave"));
        if (obj.has("bindUndo"))
            builder.bindUndo(readKeys(obj, "bindUndo"));
        if (obj.has("bindRedo"))
            builder.bindRedo(readKeys(obj, "bindRedo"));
        if (obj.has("bindDuplicate"))
            builder.bindDuplicate(readKeys(obj, "bindDuplicate"));
        if (obj.has("bindOpenConsole"))
            builder.bindOpenConsole(readKeys(obj, "bindOpenConsole"));

        return builder.build();
    }

    private static int[] readKeys(JsonObject obj, String field) {
        JsonArray arr = obj.getAsJsonArray(field);
        int[] keys = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++)
            keys[i] = arr.get(i).getAsInt();
        return keys;
    }
}