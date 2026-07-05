package application.bootstrap.menupipeline.menumanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.util.DimensionVector2;
import application.bootstrap.menupipeline.util.LayoutStruct;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector2;

class MenuFileParserUtility extends EngineUtility {

    /*
     * Stateless JSON parsing helpers shared by InternalBuilder.
     *
     * parseStateBlock handles on_hover_enter, on_hover, on_hover_exit, and
     * click_state — all four are identical in shape: optional use/inline element,
     * sprite, layout, color, text, children, and optional method callback.
     *
     * parseOnDrag parses on_drag — method callback only, no element swap.
     * parseOnClick parses on_click — method callback only, no element swap.
     */

    // Callbacks — method only \\

    static String[] parseOnClick(JsonObject json) {
        return parseCallback(json, "on_click");
    }

    static String[] parseOnDrag(JsonObject json) {
        return parseCallback(json, "on_drag");
    }

    private static String[] parseCallback(JsonObject json, String key) {

        if (!json.has(key))
            return null;

        JsonObject obj = json.getAsJsonObject(key);

        return new String[] {
                JsonUtility.validateString(obj, "class"),
                JsonUtility.validateString(obj, "method"),
                JsonUtility.getString(obj, "arg", null)
        };
    }

    // State Block Keys \\

    static boolean hasStateBlock(JsonObject json, String key) {
        return json.has(key);
    }

    static JsonObject getStateBlock(JsonObject json, String key) {
        return json.getAsJsonObject(key);
    }

    // Element Type \\

    static ElementType parseElementType(String type, String id) {
        return switch (type.toLowerCase()) {
            case "sprite" -> ElementType.SPRITE;
            case "texture" -> ElementType.TEXTURE;
            case "button" -> ElementType.BUTTON;
            case "label" -> ElementType.LABEL;
            case "container" -> ElementType.CONTAINER;
            case "toolbar" -> ElementType.TOOLBAR;
            case "canvas_area" -> ElementType.CANVAS_AREA;
            default -> {
                throwException("Unknown element type '" + type + "' on element '" + id + "'");
                yield null;
            }
        };
    }

    // Color \\

    static Color parseColor(JsonObject json) {

        if (!json.has("color"))
            return null;

        JsonArray arr = json.getAsJsonArray("color");

        if (arr.size() != 4)
            throwException("'color' must be exactly 4 floats [r, g, b, a]");

        return new Color(
                arr.get(0).getAsFloat(),
                arr.get(1).getAsFloat(),
                arr.get(2).getAsFloat(),
                arr.get(3).getAsFloat());
    }

    // Layout — full parse, absent fields get defaults \\

    static LayoutStruct parseLayout(JsonObject json) {
        return new LayoutStruct(
                parseOriginField(json, "anchor"),
                parseOriginField(json, "pivot"),
                DimensionVector2.parse(json, "position",
                        EngineSetting.ELEMENT_DEFAULT_POSITION,
                        EngineSetting.ELEMENT_DEFAULT_POSITION),
                DimensionVector2.parse(json, "size",
                        EngineSetting.ELEMENT_DEFAULT_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_SIZE),
                json.has("min_size") ? DimensionVector2.parse(json, "min_size",
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE) : null,
                json.has("max_size") ? DimensionVector2.parse(json, "max_size",
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE) : null);
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
                json.has("position") ? DimensionVector2.parse(json, "position",
                        EngineSetting.ELEMENT_DEFAULT_POSITION,
                        EngineSetting.ELEMENT_DEFAULT_POSITION) : null,
                json.has("size") ? DimensionVector2.parse(json, "size",
                        EngineSetting.ELEMENT_DEFAULT_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_SIZE) : null,
                json.has("min_size") ? DimensionVector2.parse(json, "min_size",
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE) : null,
                json.has("max_size") ? DimensionVector2.parse(json, "max_size",
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE) : null);
    }

    // Origin Field \\

    static Vector2 parseOriginName(String name) {

        String normalized = name.toLowerCase();
        float x = 0.5f;
        float y = 0.5f;

        if (normalized.contains("left"))
            x = 0f;
        else if (normalized.contains("right"))
            x = 1f;

        if (normalized.contains("top"))
            y = 1f;
        else if (normalized.contains("bottom"))
            y = 0f;

        return new Vector2(x, y);
    }

    static Vector2 parseOriginField(JsonObject json, String key) {

        if (!json.has(key))
            return new Vector2(0.5f, 0.5f);

        JsonElement el = json.get(key);

        if (el.isJsonPrimitive())
            return parseOriginName(el.getAsString());

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            return new Vector2(
                    JsonUtility.getFloat(obj, "x", 0.5f),
                    JsonUtility.getFloat(obj, "y", 0.5f));
        }

        return new Vector2(0.5f, 0.5f);
    }
}