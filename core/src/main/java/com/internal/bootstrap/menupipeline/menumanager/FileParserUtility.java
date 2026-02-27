package com.internal.bootstrap.menupipeline.menumanager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.menupipeline.element.DimensionVector2;
import com.internal.bootstrap.menupipeline.element.ElementOrigin;
import com.internal.bootstrap.menupipeline.element.ElementType;
import com.internal.bootstrap.menupipeline.element.LayoutStruct;
import com.internal.core.engine.UtilityPackage;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.mathematics.vectors.Vector2;

class FileParserUtility extends UtilityPackage {

    // On Click \\

    static String[] parseOnClick(JsonObject json) {
        if (!json.has("on_click"))
            return null;
        JsonObject clickJson = json.getAsJsonObject("on_click");
        return new String[] {
                JsonUtility.validateString(clickJson, "class"),
                JsonUtility.validateString(clickJson, "method"),
                JsonUtility.getString(clickJson, "arg", null)
        };
    }

    // Element Type \\

    static ElementType parseElementType(String type, String id) {
        return switch (type.toLowerCase()) {
            case "sprite" -> ElementType.SPRITE;
            case "texture" -> ElementType.TEXTURE;
            case "button" -> ElementType.BUTTON;
            case "label" -> ElementType.LABEL;
            case "container" -> ElementType.CONTAINER;
            default -> {
                throwException("Unknown element type '" + type + "' on element '" + id + "'");
                yield null;
            }
        };
    }

    // Layout — full parse, absent fields get defaults \\

    static LayoutStruct parseLayout(JsonObject json) {
        return new LayoutStruct(
                parseOriginField(json, "anchor"),
                parseOriginField(json, "pivot"),
                DimensionVector2.parse(json, "position", "0%", "0%"),
                DimensionVector2.parse(json, "size", "10%", "10%"),
                json.has("min_size") ? DimensionVector2.parse(json, "min_size", "0%", "0%") : null,
                json.has("max_size") ? DimensionVector2.parse(json, "max_size", "100%", "100%") : null);
    }

    // Layout Override — absent fields null, preserved from template \\

    static LayoutStruct parseLayoutOverride(JsonObject json) {

        boolean hasAny = json.has("anchor") || json.has("pivot") || json.has("position")
                || json.has("size") || json.has("min_size") || json.has("max_size");

        if (!hasAny)
            return null;

        return new LayoutStruct(
                json.has("anchor") ? parseOriginField(json, "anchor") : null,
                json.has("pivot") ? parseOriginField(json, "pivot") : null,
                json.has("position") ? DimensionVector2.parse(json, "position", "0%", "0%") : null,
                json.has("size") ? DimensionVector2.parse(json, "size", "10%", "10%") : null,
                json.has("min_size") ? DimensionVector2.parse(json, "min_size", "0%", "0%") : null,
                json.has("max_size") ? DimensionVector2.parse(json, "max_size", "100%", "100%") : null);
    }

    // Origin Field \\

    static Vector2 parseOriginField(JsonObject json, String key) {

        if (!json.has(key))
            return new Vector2(0f, 0f);

        JsonElement el = json.get(key);

        if (el.isJsonPrimitive()) {
            ElementOrigin o = ElementOrigin.fromString(el.getAsString());
            return new Vector2(o.x, o.y);
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            float x = JsonUtility.getFloat(obj, "x", 0f);
            float y = JsonUtility.getFloat(obj, "y", 0f);
            return new Vector2(x, y);
        }

        return new Vector2(0f, 0f);
    }
}