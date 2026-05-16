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

class FileParserUtility extends EngineUtility {

    /*
     * Stateless JSON parsing helpers shared by InternalBuilder. Handles
     * on_click resolution, element type mapping, color parsing, layout parsing,
     * and origin field parsing. Package-private — only InternalBuilder may call
     * these.
     */

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

    static Vector2 parseOriginField(JsonObject json, String key) {

        if (!json.has(key))
            return new Vector2(0f, 0f);

        JsonElement el = json.get(key);

        if (el.isJsonPrimitive())
            return parseOriginName(el.getAsString());

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            return new Vector2(
                    JsonUtility.getFloat(obj, "x", 0f),
                    JsonUtility.getFloat(obj, "y", 0f));
        }

        return new Vector2(0f, 0f);
    }

    private static Vector2 parseOriginName(String name) {
        return switch (name.trim().toLowerCase()) {
            case "top_left" -> new Vector2(0f, 1f);
            case "top_center" -> new Vector2(0.5f, 1f);
            case "top_right" -> new Vector2(1f, 1f);
            case "bottom_left" -> new Vector2(0f, 0f);
            case "bottom_center" -> new Vector2(0.5f, 0f);
            case "bottom_right" -> new Vector2(1f, 0f);
            case "center" -> new Vector2(0.5f, 0.5f);
            case "left" -> new Vector2(0f, 0.5f);
            case "right" -> new Vector2(1f, 0.5f);
            default -> {
                throwException("Unknown origin: '" + name + "'");
                yield null;
            }
        };
    }
}